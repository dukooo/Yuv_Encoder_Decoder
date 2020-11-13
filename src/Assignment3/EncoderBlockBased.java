package Assignment3;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class EncoderBlockBased {

	boolean FMEenabled, fastME, useGreyRef;
	byte[] yOnly;// the original file after padding
	int width, height; // after padding
	int frameLength, blockSize, multipleN, rangeR, qp, numRef;
	int frameIndex;
	ArrayList<byte[][]> buffers;
	byte[] vbsBytes;
	byte[] currFrame;
	byte[][] reference; // reference[frame index][pixel index]
	String mvFileName;
	String mvEntropyEncodedFileName;
	// for geenrating the graph
	String residualAfterMVFileName;
	// for the decoder side
	String residualFileName;
	String qtcFileName, qtcDiagOrderedFileName, qtcRleEncodedFileName, qtcEntropyEncodedFileName;
	String reconstructedFileName;
	String vbsFileName;
	byte[] motionvector;
	int I_Period;
    int[] modesInFrame;
	// byte[] intraPredictor;
    ResidualApproximation resApp;
	ResidualBlockGenerator resGen;
	MotionVectorGenerator mvGen;
	ReconstructedBlockGenerator recGen;
	ModeGenerator modGen;

	public EncoderBlockBased() {
		PropertyHelper ph = new PropertyHelper();
		frameLength = Integer.valueOf(ph.getProperty("frameCount"));
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		yOnly = addPaddingToYOnly(Integer.valueOf(ph.getProperty("vidWidth")),
								  Integer.valueOf(ph.getProperty("vidHeight")),
								  FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName")));
		numRef = Integer.valueOf(ph.getProperty("nRefFrames"));
		I_Period = Integer.valueOf(ph.getProperty("I_Period"));
		reconstructedFileName = ph.getProperty("encReconstructedFileName");
        mvFileName = ph.getProperty("mvFileName");
        mvEntropyEncodedFileName = ph.getProperty("mvEntropyEncodedFileName");
        residualAfterMVFileName = ph.getProperty("residualAfterMVFileName");
        residualFileName = ph.getProperty("residualFileName");
        qtcFileName = ph.getProperty("qtcFileName");
        qtcEntropyEncodedFileName = ph.getProperty("qtcEntropyEncodedFileName");
        qtcDiagOrderedFileName = ph.getProperty("qtcDiagOrderedFileName");
        qtcRleEncodedFileName = ph.getProperty("qtcRleEncodedFileName");
		vbsFileName = ph.getProperty("vbsFileName");
		FMEenabled = Boolean.parseBoolean(ph.getProperty("FMEEnable"));
        fastME = Boolean.parseBoolean(ph.getProperty("fastME"));
        useGreyRef = false;
		setPredictor();
		currFrame = new byte[width * height];
		vbsBytes = new byte[width * height / blockSize / blockSize]; // store 1 frame of VBS (length = number of blocks)
		resApp = new ResidualApproximation();
		resGen = new ResidualBlockGenerator();
		mvGen = new MotionVectorGenerator();
		recGen = new ReconstructedBlockGenerator();
		modGen = new ModeGenerator();
	}

	public byte[] addPaddingToYOnly(int origWidth, int origHeight, byte[] yOnly) {
		byte[] newYOnly = new byte[width * height * frameLength];
		for (int i = 0; i < frameLength; i++) {
			for (int y = 0; y < height; y++) {
				if (y < origHeight) {
					for ( int x = 0; x < width; x++) {
						if (x < origWidth)
							newYOnly[i * width * height + y * width + x] = yOnly[i * origWidth * origHeight + y * origWidth + x];
						else
							newYOnly[i * width * height + y * width + x] = (byte)128;
					}
				}
				else {
					for ( int x = 0; x < width; x++)
						newYOnly[i * width * height + y * width + x] = (byte)128;
				}
			}
		}
		return newYOnly;
	}

	public void setPredictor() {
		reference = new byte[numRef][width * height];
		for (int i = 0; i < numRef; i++) {
			for (int j = 0; j < width * height; j++) reference[i][j] = (byte) 128;
		}
	}

	/**
	 * Private helper function to help with Inter Frame Encoding. Will do following steps:
	 * 1. Generate MV
	 * 2. Generate P Frame Residual
	 * 3. Transform, Quantize, Rescale, invTransform.
	 * 4. Reconstruct.
	 * All operations/modifications on arrays are done in place.
	 * Only returning the MV array as that is not passed in.
	 * @param currFrame
	 * @param reference
	 * @param y
	 * @param x
	 * @param blockSize
	 * @param prevMV
	 * @param residualFrame
	 * @param transResidual
	 * @param qtcResidual
	 * @param rescaledResidual
	 * @param invTransResidual
	 * @param reconstructedFrame
	 * @return int array of motion vectors
	 */
	private int[] encoderInterTQRIR(byte [] currFrame, byte[][] reference, int y, int x, int blockSize, int[] prevMV, short[] residualFrame,
			int[] transResidual, int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, byte[] reconstructedFrame, ArrayList<byte[][]> buffers) {

		int[] mvBlock;
		if (fastME && FMEenabled)
			mvBlock = mvGen.generateMVbyBlockFast(currFrame, buffers, y, x, blockSize, prevMV, -1);
		else if (fastME)
			mvBlock = mvGen.generateMVbyBlockFast(currFrame, reference, y, x, blockSize, prevMV, -1);
		else if (FMEenabled)
			mvBlock = mvGen.generateMVbyBlockFraction(currFrame, reference, y, x, blockSize, prevMV, buffers);
		else
			mvBlock = mvGen.generateMVbyBlock(currFrame, reference, y, x, blockSize, prevMV);
		this.resGen.generatePframeResidualBlock(currFrame, reference, blockSize, residualFrame, mvBlock, y, x, buffers);

		resApp.transfOnBlock(residualFrame, transResidual, x, y, blockSize);
		resApp.quantOnBlock(transResidual, qtcResidual, x, y, blockSize);
		resApp.rescaleOnBlock(qtcResidual, rescaledResidual, x, y, blockSize);
		resApp.invTransfOnBlock(rescaledResidual, invTransResidual, x, y, blockSize);
		recGen.reconstructedPframeBlock(reconstructedFrame, invTransResidual, reference, x, y, blockSize, mvBlock, buffers);

		return mvBlock;
	}


	public int [] encodeInter(int [] finalQTC, ArrayList<Integer> finalMV, byte[] finalReconstructed, ArrayList<byte[][]> buffers, int[] prevMV, int y, int x ) {
		int  blockIndex = y / blockSize * width / blockSize + (x/blockSize);
		int[] currMV = new int[3]; // return this at the end, so the next call can use it as prevMV

		// Using 1 for Whole Block, 2 for Sub Blocks
		short[] residualFrame1 = new short[width * height];
		int[] transResidual1 = new int[width * height];
		int[] qtcResidual1 = new int[width * height];
		int[] rescaledResidual1 = new int[width * height];
		short[] invTransResidual1 = new short[width * height];
		short[] residualFrame2 = new short[width * height];
		int[] transResidual2 = new int[width * height];
		int[] qtcResidual2 = new int[width * height];
		int[] rescaledResidual2 = new int[width * height];
		short[] invTransResidual2 = new short[width * height];
		byte[] reconstructedFrame1 = finalReconstructed.clone();
		byte[] reconstructedFrame2 = finalReconstructed.clone();
//		byte[] finalReconstructed = new byte[width * height];


		if(x == 0) {
			prevMV[0] = 0;
			prevMV[1] = 0;
			prevMV[2] = 0;
		}
		// generate reconstructed block for the whole block and store mv/qtc into mvBlock.mv/qtcBlock.qtc

		// Step 1. Do everything for this block using whole block (aka blocksize = blocksize)
		int[] mvWholeBlock = this.encoderInterTQRIR(currFrame, reference, y, x, blockSize, prevMV, residualFrame1, transResidual1,
				qtcResidual1, rescaledResidual1, invTransResidual1, reconstructedFrame1, buffers);
		int[] mvWholeBlockDiffEncoded = Helper.getDiffEncoding(mvWholeBlock, prevMV);
		double rdoBlock1 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame1, y, x, resApp.getQP(), blockSize,
				width, mvWholeBlockDiffEncoded, qtcResidual1);

		double rdoBlock2 = Double.MAX_VALUE;
		int[] mvSubBlock4 = null;
		int[] mvMergedBlocksDiffEncoded =null;
		int[] mvMergedBlocks = null;
		if (VariableBlockSize.isVBSEnabled()) {
			// Step 2. Do everything again for this block using sub blocks (aka blocksize = blocksize/2)
			resApp.setQP(resApp.getQP()-1); // Set new QP because we are now doing blocksize/2
			int[] mvSubBlock1 = this.encoderInterTQRIR(currFrame, reference, y, x, blockSize/2, prevMV, residualFrame2, transResidual2,
					qtcResidual2, rescaledResidual2, invTransResidual2, reconstructedFrame2, buffers);
			int[] mvSubBlock2 = this.encoderInterTQRIR(currFrame, reference,  y, x+(blockSize/2), blockSize/2, mvSubBlock1, residualFrame2, transResidual2,
					qtcResidual2, rescaledResidual2, invTransResidual2, reconstructedFrame2, buffers);
			int[] mvSubBlock3 = this.encoderInterTQRIR(currFrame, reference, y+(blockSize/2), x, blockSize/2, mvSubBlock2, residualFrame2, transResidual2,
					qtcResidual2, rescaledResidual2, invTransResidual2, reconstructedFrame2, buffers);
			mvSubBlock4 = this.encoderInterTQRIR(currFrame, reference, y+(blockSize/2), x+(blockSize/2), blockSize/2, mvSubBlock3, residualFrame2, transResidual2,
					qtcResidual2, rescaledResidual2, invTransResidual2, reconstructedFrame2, buffers);


			mvMergedBlocks = Helper.addAll(mvSubBlock1, mvSubBlock2);
			mvMergedBlocks = Helper.addAll(mvMergedBlocks, mvSubBlock3);
			mvMergedBlocks = Helper.addAll(mvMergedBlocks, mvSubBlock4);
			mvMergedBlocksDiffEncoded = Helper.getDiffEncoding(mvMergedBlocks, prevMV);


			rdoBlock2 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame2, y, x, resApp.getQP(), blockSize,
					width, mvMergedBlocksDiffEncoded, qtcResidual2);
			resApp.setQP(resApp.getQP()+1); // Restore QP to original value (same as config)
		}

		for (int yb = y; yb < y + blockSize; yb++) {
			for (int xb = x; xb < x + blockSize; xb++) {
				// Take the block with better RDO
				if (rdoBlock1 < rdoBlock2) {
					finalReconstructed[yb * width + xb] = reconstructedFrame1[yb * width + xb];
					finalQTC[yb * width + xb] = qtcResidual1[yb * width + xb];
					//System.out.println("Taking reconstructed block with whole block operation.");
				}
				else {
					finalReconstructed[yb * width + xb] = reconstructedFrame2[yb * width + xb];
					finalQTC[yb * width + xb] = qtcResidual2[yb * width + xb];
					//System.out.println("Taking reconstructed block with sub block operation.");
				}

			}
		}

		// Make some decisions on what to write/save
		if (rdoBlock1 < rdoBlock2) {
			vbsBytes[blockIndex] = 0; // no split
			currMV = mvWholeBlock.clone();
			for (int mv : mvWholeBlockDiffEncoded)
				finalMV.add(mv);
//					writeToMVFile(mvWholeBlockDiffEncoded, blockIndex);
		}
		else {
			vbsBytes[blockIndex] = 1; // split
			currMV = mvSubBlock4.clone();
			for (int mv : mvMergedBlocksDiffEncoded)
				finalMV.add(mv);
//					writeToMVFile(mvMergedBlocksDiffEncoded, blockIndex);
		}
		prevMV[0] = currMV[0];
		prevMV[1] = currMV[1];
		prevMV[2] = currMV[2];

		return prevMV;
	}


	private int encoderIntraTQRIR(byte [] currFrame, int y, int x, int blockSize, int prevMode,
			short[] residualFrame, int[] transResidual, int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, byte[] reconstructedFrame) {
		int mode = 0;
		if (useGreyRef) {
			resGen.generateIframeResidualBlockUsingGreyRef(currFrame, reconstructedFrame, residualFrame, mode, y, x, blockSize);
		}
		else {
			mode = modGen.generateModeByBlock(currFrame, reconstructedFrame, y, x, blockSize, prevMode);
			
			resGen.generateIframeResidualBlock(currFrame, reconstructedFrame, residualFrame, mode, y, x, blockSize);
		}
		
		

		resApp.transfOnBlock(residualFrame, transResidual, x, y, blockSize);
		resApp.quantOnBlock(transResidual, qtcResidual, x, y, blockSize);
		resApp.rescaleOnBlock(qtcResidual, rescaledResidual, x, y, blockSize);
		resApp.invTransfOnBlock(rescaledResidual, invTransResidual, x, y, blockSize);
		recGen.reconstructedIframeBlock(reconstructedFrame, invTransResidual, reconstructedFrame, x, y, blockSize, mode);

		return mode;
	}

//	private void encodeIntraBlock(byte [] currFrame, int y, int x, int blockSize, int prevMode, short[] residualFrame,
//			int[] transResidual, int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, byte[] reconstructedFrame, int blockIndex) {
//
//	}

	public int encodeIntra(int [] finalQTC, ArrayList<Integer> finalMV, byte[] finalReconstructed, int [] prevMode, int y, int x) {

		int currMode =0 , blockIndex = y / blockSize * width / blockSize + (x/blockSize);
		byte[] reconstructedFrame1 = finalReconstructed.clone();
		byte[] reconstructedFrame2 = finalReconstructed.clone();
//		byte[] finalReconstructed = new byte[width * height];
		short[] residualFrame1 = new short[width * height];
		int[] transResidual1 = new int[width * height];
		int[] qtcResidual1 = new int[width * height];
		int[] rescaledResidual1 = new int[width * height];
		short[] invTransResidual1 = new short[width * height];
		short[] residualFrame2 = new short[width * height];
		int[] transResidual2 = new int[width * height];
		int[] qtcResidual2 = new int[width * height];
		int[] rescaledResidual2 = new int[width * height];
		short[] invTransResidual2 = new short[width * height];
		if (x == 0)
			prevMode[0] = 0;

		// Step 1. Do whole block, AKA blocksize = blocksize from config
		int mode1 = encoderIntraTQRIR(currFrame, y, x, blockSize, prevMode[0],
		residualFrame1, transResidual1, qtcResidual1, rescaledResidual1, invTransResidual1,reconstructedFrame1);

		// Calculate rdo
		double rdoBlock1 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame1, y, x, resApp.getQP(), blockSize,
				width, new int[] {mode1-prevMode[0]}, qtcResidual1);

		double rdoBlock2 = Double.MAX_VALUE;
		int [] diffEncodedMode = null;
		int [] mode_vbs = null;
		int modeSub4 = 0;
		if (VariableBlockSize.isVBSEnabled()) {
			// Step 2. Do 4 sub blocks, AKA blocksize = blocksize/2
			resApp.setQP(resApp.getQP()-1);
			int modeSub1 = encoderIntraTQRIR(currFrame, y, x, blockSize/2, prevMode[0],
					residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);

			int modeSub2 = encoderIntraTQRIR(currFrame, y, x+(blockSize/2), blockSize/2, modeSub1,
					residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);

			int modeSub3 = encoderIntraTQRIR(currFrame, y+(blockSize/2), x, blockSize/2, modeSub2,
					residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);

			modeSub4 = encoderIntraTQRIR(currFrame, y+(blockSize/2), x+(blockSize/2), blockSize/2, modeSub3,
					residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);
			mode_vbs = new int[] {modeSub1, modeSub2, modeSub3, modeSub4};

			// Calculate rdo
			diffEncodedMode = new int[] {modeSub1 - prevMode[0], modeSub2 - modeSub1, modeSub3-modeSub2, modeSub4-modeSub3};
			rdoBlock2 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame2, y, x, resApp.getQP(), blockSize,
					width, diffEncodedMode, qtcResidual2);
			resApp.setQP(resApp.getQP()+1);
		}

		for (int yb = y; yb < y + blockSize; yb++) {
			for (int xb = x; xb < x + blockSize; xb++) {

				// note: rdoBlock2 defaults to Double.Max_VALUE unless VBS is enabled.
				if (rdoBlock1 < rdoBlock2) {
					finalReconstructed[yb * width + xb] = reconstructedFrame1[yb * width + xb];
					reconstructedFrame2[yb * width + xb] = reconstructedFrame1[yb * width + xb];
					finalQTC[yb * width + xb] = qtcResidual1[yb * width + xb];
					//System.out.println("Taking reconstructed block with whole block operation.");
				}
				else {
					finalReconstructed[yb * width + xb] = reconstructedFrame2[yb * width + xb];
					reconstructedFrame1[yb * width + xb] = reconstructedFrame2[yb * width + xb];
					finalQTC[yb * width + xb] = qtcResidual2[yb * width + xb];

					//System.out.println("Taking reconstructed block with sub block operation.");
				}

			}
		}

		if (rdoBlock1 < rdoBlock2) {
			finalMV.add(mode1-prevMode[0]);
			//writeToMVFile(new int[] {mode1-prevMode}, blockIndex);
			vbsBytes[blockIndex] = 0; // no split
			currMode = mode1;
		}
		else {
			for (int mode : diffEncodedMode) {
				finalMV.add(mode);
			}
			//writeToMVFile(diffEncodedMode, blockIndex);
			vbsBytes[blockIndex] = 1; // split
			currMode = modeSub4;
		}

		blockIndex++;
		prevMode[0] = currMode;
		return currMode;
	}
	
	public void updateLastReferenceFrame(byte[] reconstructed) {
		reference[0] = reconstructed.clone();
	}
	public void updateReference(byte[] reconstructed) {
		for (int j = numRef - 1; j > 0; j--)
      		reference[j] = reference[j - 1];
    	reference[0] = reconstructed;
	}

	public void buildBuffers() {
		if (FMEenabled) {
			InterpolationBuffer interpolationBuffer = new InterpolationBuffer();
			buffers = interpolationBuffer.buildBuffers(reference);
		}
		else buffers = new ArrayList<>();
	}

	public void startEncoding() {
		for (int i = 0; i < frameLength; i++) {
			frameIndex = i;
			System.out.println("Working on Frame # " + frameIndex);
	        setCurr(i);
	        buildBuffers();
	        // Storage for entire frame
	        byte[] reconstructed = new byte[width * height]; // per pixel
	        int[] qtc = new int[width*height]; // per pixel
	        vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
	        ArrayList<Integer> finalMV = new ArrayList<Integer>(); // 1 frame of MVs
	        int [] prevMV = new int[3]; // always 3 elements in mv
	        int [] prevMode = new int[1]; // 1 element in mode
	        
	        if (frameIndex % I_Period != 0) {
	        	for (int y = 0; y < height; y+= blockSize) {
	        		for (int x = 0; x < width; x += blockSize ) {
	        			encodeInter(qtc, finalMV, reconstructed, buffers, prevMV, y, x);
	        		}
	        	}
	        	updateReference(reconstructed);

	        }
	        else {
	        	for (int y = 0; y < height; y+= blockSize) {
	        		for (int x = 0; x < width; x += blockSize ) {
	        			encodeIntra(qtc, finalMV, reconstructed, prevMode, y, x);
	        		}
	        	}
	        	setPredictor();
	        }
	        // Write data we care about to file
	        writeToReconstructedFile(reconstructed);
//	        System.out.println("Frame index " + frameIndex + "MV size = " + finalMV.size());
	        this.writeToMVFile(finalMV);
	        int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
			writeToQtcFile(Helper.intArrToShortArr(qtc));
			writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded));
			writeToVBSFile(vbsBytes);
		}

		// Only call this when everything is done.
		EntropyEncDec.generateAllEntropyEncodedFiles();
	}

	public void writeToReconstructedFile(byte[] reconstructedFrame) {
		if (frameIndex == 0)
			FileHelper.writeToFile(reconstructedFrame, reconstructedFileName);
		else
			FileHelper.writeToFileAppend(reconstructedFrame, reconstructedFileName);
	}

	/**
	 * Helper function to write to the diagonal re-ordered QTC
	 * @param qtcEnc
	 */
	public void writeToQtcDiagOrderedFile(short[] qtc) {
		if (frameIndex == 0)
			FileHelper.writeToFile(qtc, qtcDiagOrderedFileName);
		else
			FileHelper.writeToFileAppend(qtc, qtcDiagOrderedFileName);
	}

	/**
	 * Helper func to write raw Quantized TC values
	 * @param qtcEnc
	 */
	public void writeToQtcFile(short[] qtc) {
		if (frameIndex == 0)
			FileHelper.writeToFile(qtc, this.qtcFileName);
		else
			FileHelper.writeToFileAppend(qtc, this.qtcFileName);
	}

	/**
	 * Write ints for mode and MV. Need to know block because data will come in block by block
	 * @param mv
	 * @param blockIndex
	 */
	public void writeToMVFile(int[] mv, int blockIndex) {

		if (frameIndex == 0 && blockIndex == 0) {
			FileHelper.writeToFile(Helper.intArrToShortArr(mv), mvFileName);
		}
		else {
			FileHelper.writeToFileAppend(Helper.intArrToShortArr(mv), mvFileName);
		}
	}

	public void writeToMVFile(ArrayList<Integer> mv) {
		if (frameIndex == 0) {
			FileHelper.writeToFile(Helper.intArrListToShortArr(mv), mvFileName);
		}
		else {
			FileHelper.writeToFileAppend(Helper.intArrListToShortArr(mv), mvFileName);
		}
	}

	/**
	 * Write byte for VBS.
	 * @param vbsSplit
	 */
	public void writeToVBSFile(byte[] vbsSplit) {
		if (frameIndex == 0) {
			FileHelper.writeToFile(vbsSplit, vbsFileName);
		}
		else {
			FileHelper.writeToFileAppend(vbsSplit, vbsFileName);
		}
	}

	// put the current frame y only value into currFrame
	public void setCurr(int frameIndex) {
		for (int i = 0; i < width * height; i++) {
			currFrame[i] = yOnly[frameIndex * width * height + i];
		}
	}

	public static void main(String[] args) {
		Encoder en = new Encoder();
//		 en.test4a();
		en.startEncoding();
	}

}

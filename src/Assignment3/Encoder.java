package Assignment3;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;



public class Encoder {

	boolean FMEenabled, fastME;
	byte[] yOnly;// the original file after padding
	int width, height; // after padding
	int frameLength, blockSize, multipleN, rangeR, qp, numRef;
	int frameIndex;
	byte[] vbsBytes;
	byte[] currFrame;
	byte[][] reference; // reference[frame index][pixel index]
	String mvFileName;
	String mvEntropyEncodedFileName;
	// for generating the graph
	String residualAfterMVFileName;
	// for the decoder side
	String residualFileName;
	String qtcFileName, qtcDiagOrderedFileName, qtcRleEncodedFileName, qtcEntropyEncodedFileName;
	String qpFileName;
	String reconstructedFileName;
	String vbsFileName;
	String scFileName, scEntropyEncodedFileName;
	byte[] motionvector;
	int I_Period;
    int[] modesInFrame;
	// byte[] intraPredictor;
    ResidualApproximation resApp;
	ResidualBlockGenerator resGen;
	MotionVectorGenerator mvGen;
	ReconstructedBlockGenerator recGen;
	ModeGenerator modGen;
	MultiplePass mp;
	int bitBudget_perFrame, bitBudget_perRow;
	int RCflag, pass;
	int[] sc, rowQP; 
	int[][] rowBitcount1;
	double[][] rowBitcount2;
	int threshold, prevQP;
	ArrayList<byte[][]> buffers;
	ArrayList<ArrayList<byte[][]>> allFrameBuffer;
	int[] prevFrameMVsplit; 
	int[] prevFrameMVnotSplit; 
	
	public Encoder() {
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
		setPredictor();
		currFrame = new byte[width * height];
		vbsBytes = new byte[width * height / blockSize / blockSize]; // store 1 frame of VBS (length = number of blocks)
		resApp = new ResidualApproximation();
		resGen = new ResidualBlockGenerator();
		mvGen = new MotionVectorGenerator();
		recGen = new ReconstructedBlockGenerator();
		modGen = new ModeGenerator();
		mp = new MultiplePass();
		bitBudget_perFrame = RateControl.getBitBudget_perFrame(); 
        bitBudget_perRow = RateControl.getBitBudget_perRow();
		RCflag = Integer.valueOf(ph.getProperty("RCflag"));
		qpFileName = ph.getProperty("qpFileName");
		sc = new int[frameLength];
		rowBitcount1 = new int[frameLength][height / blockSize];
		rowBitcount2 = new double[frameLength][height / blockSize];
		rowQP = new int[height / blockSize];
		scFileName = ph.getProperty("scFileName");
        scEntropyEncodedFileName = ph.getProperty("scEntropyEncodedFileName");
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
	
	public void setPredictor(byte[] previous) {
		reference = new byte[numRef][width * height];
		reference[0] = previous;
		for (int i = 1; i < numRef; i++) {
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
		int p = -1;
		if (fastME && FMEenabled){
			if (RCflag == 3) {
				p = 2;
				if (blockSize == this.blockSize) { // not split
					int index = ((y / blockSize) * (width / blockSize) + x / blockSize) * 3;
					prevMV[0] = prevFrameMVnotSplit[index];
					prevMV[1] = prevFrameMVnotSplit[index + 1];
					prevMV[2] = prevFrameMVnotSplit[index + 2];
				}
				else { // split
					int index = ((y / this.blockSize) * (width / this.blockSize) + x / this.blockSize) * 12;
					if (y / this.blockSize == 0)
						index += 3;
					else if (x / this.blockSize == 0)
						index += 6;
					else 
						index += 9;
					prevMV[0] = prevFrameMVsplit[index];
					prevMV[1] = prevFrameMVsplit[index + 1];
					prevMV[2] = prevFrameMVsplit[index + 2];
				}
			}
			mvBlock = mvGen.generateMVbyBlockFast(currFrame, buffers, y, x, blockSize, prevMV, p);
		}
		else if (fastME) {
			if (RCflag == 3) {
				p = 2;
				if (blockSize == this.blockSize) { // not split
					int index = ((y / blockSize) * (width / blockSize) + x / blockSize) * 3;
					prevMV[0] = prevFrameMVnotSplit[index];
					prevMV[1] = prevFrameMVnotSplit[index + 1];
					prevMV[2] = prevFrameMVnotSplit[index + 2];
				}
				else { // split
					int index = ((y / this.blockSize) * (width / this.blockSize) + x / this.blockSize) * 12;
					if (y / this.blockSize == 0)
						index += 3;
					else if (x / this.blockSize == 0)
						index += 6;
					else 
						index += 9;
					prevMV[0] = prevFrameMVsplit[index];
					prevMV[1] = prevFrameMVsplit[index + 1];
					prevMV[2] = prevFrameMVsplit[index + 2];
				}
			}
			mvBlock = mvGen.generateMVbyBlockFast(currFrame, reference, y, x, blockSize, prevMV, p);
		}
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
	
	
	public byte[] encodeInter(int [] finalQTC) {
		int blockIndex = 0;
		prevFrameMVnotSplit = new int[width * height / blockSize / blockSize * 3];
		prevFrameMVsplit = new int[width * height / blockSize / blockSize * 3 * 4];
		int[] prevMV = new int[3];
		prevMV[0] = 0;
		prevMV[1] = 0;
		prevMV[2] = 0;
		
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
		byte[] reconstructedFrame1 = new byte[width * height];
		byte[] reconstructedFrame2 = new byte[width * height];
		byte[] finalReconstructed = new byte[width * height];
		int remainingBudget_perFrame = bitBudget_perFrame; // starting bit budget of a frame (based on fps and bitrate)
		int newBitBudget_perRow = bitBudget_perRow; // bit budget of first row (based on fps and bitrate)
		ArrayList<byte[][]> buffers;
		if (FMEenabled && !(RCflag == 3 && pass == 2)) {
			InterpolationBuffer interpolationBuffer = new InterpolationBuffer();
			buffers = interpolationBuffer.buildBuffers(reference);
			if (RCflag == 3 && pass == 1)
				allFrameBuffer.add(buffers);
		}
		else if (FMEenabled && RCflag == 3 && pass == 2) {
			buffers = allFrameBuffer.get(frameIndex - 1);
		}
		else buffers = new ArrayList<>();
		for (int y = 0; y < height; y+=blockSize) {
			int[] qtc_perRow = new int[width*blockSize]; // qtc bits per row
			byte[] vbs_perRow = new byte[width/blockSize];  // vbs bits per row
			int vbs_perRowIndex = 0;
			if (RCflag == 1) {
				resApp.qp = RateControl.findQP(newBitBudget_perRow, height, false); // finds appropriate QP value from QP table to meet the target per row bit budget
				writeToQPFile(resApp.qp, y); // writes to qp.txt (required for entropy encode)
                // System.out.println("---> At P Frame #" + frameIndex + " Row #" + y/blockSize + ": Budget/Row = " + newBitBudget_perRow + ", QP = " +resApp.qp);
            }
			else if ((RCflag == 2 || RCflag == 3) && pass == 2) {
				//set newBitBudget_perRow
				newBitBudget_perRow = MultiplePass.getRowBitBudget(remainingBudget_perFrame, frameIndex, rowBitcount2, (int)y / blockSize);
				resApp.qp = mp.findQP(newBitBudget_perRow, height, false);
				writeToQPFile(resApp.qp, y);
                // System.out.println("---> At P Frame #" + frameIndex + " Row #" + y/blockSize + ": Budget/Row = " + newBitBudget_perRow + ", QP = " +resApp.qp);
			}
			for (int x = 0; x < width; x+=blockSize) {				
				if(x == 0) {
					prevMV[0] = 0;
					prevMV[1] = 0;
					prevMV[2] = 0;
				}
				// generate reconstructed block for the whole block and store mv/qtc into mvBlock.mv/qtcBlock.qtc
				
				// Step 1. Do everything for this block using whole block (aka blocksize = blocksize)
				int[] mvWholeBlock = this.encoderInterTQRIR(currFrame, reference, y, x, blockSize, prevMV, residualFrame1, transResidual1, 
						qtcResidual1, rescaledResidual1, invTransResidual1, reconstructedFrame1, buffers);
				if (RCflag == 3 && pass == 1)
					prevFrameMVnotSplit = MultiplePass.setPrevFrameMVnotSplit(mvWholeBlock, prevFrameMVnotSplit, y, x, blockSize, width);
				int[] mvWholeBlockDiffEncoded = Helper.getDiffEncoding(mvWholeBlock, prevMV);
				double rdoBlock1 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame1, y, x, resApp.getQP(), blockSize, 
						width, mvWholeBlockDiffEncoded, qtcResidual1);
				
				double rdoBlock2 = Double.MAX_VALUE;
				int[] mvSubBlock4 = null;
				int[] mvMergedBlocksDiffEncoded =null;
				int[] mvMergedBlocks = null;
				if (VariableBlockSize.isVBSEnabled()) {
					// Step 2. Do everything again for this block using sub blocks (aka blocksize = blocksize/2)
					boolean qpChanged = false;
					if (resApp.getQP() != 0) {
						resApp.setQP(resApp.getQP()-1); // Set new QP because we are now doing blocksize/2
						qpChanged = true;
					}
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
					if (RCflag == 3 && pass == 1)
						prevFrameMVsplit = MultiplePass.setPrevFrameMVsplit(mvMergedBlocks, prevFrameMVsplit, y, x, blockSize, width);
					mvMergedBlocksDiffEncoded = Helper.getDiffEncoding(mvMergedBlocks, prevMV);
					
					
					rdoBlock2 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame2, y, x, resApp.getQP(), blockSize, 
							width, mvMergedBlocksDiffEncoded, qtcResidual2);	
					if (qpChanged) resApp.setQP(resApp.getQP()+1); // Restore QP to original value (same as config)
				}
								
				for (int yb = y; yb < y + blockSize; yb++) {
					for (int xb = x; xb < x + blockSize; xb++) {
						// Take the block with better RDO
						if (rdoBlock1 < rdoBlock2) {
							finalReconstructed[yb * width + xb] = reconstructedFrame1[yb * width + xb];
							finalQTC[yb * width + xb] = qtcResidual1[yb * width + xb];
							if (RCflag != 0) qtc_perRow[(yb-y)*width + xb] = qtcResidual1[yb * width + xb]; // for finding qtc bits per row
							//System.out.println("Taking reconstructed block with whole block operation.");
						}
						else {
							finalReconstructed[yb * width + xb] = reconstructedFrame2[yb * width + xb];
							finalQTC[yb * width + xb] = qtcResidual2[yb * width + xb];
							if (RCflag != 0) qtc_perRow[(yb-y)*width + xb] = qtcResidual2[yb * width + xb]; // for finding qtc bits per row
							//System.out.println("Taking reconstructed block with sub block operation.");
						}
							
					}
				}
				
				// Make some decisions on what to write/save
				if (rdoBlock1 < rdoBlock2) {
					prevMV = mvWholeBlock;
					vbsBytes[blockIndex] = 0; // no split
					writeToMVFile(mvWholeBlockDiffEncoded, blockIndex);
					if (RCflag != 0) {
						vbs_perRow[vbs_perRowIndex++] = 0; // for finding vbs bit per row
						if (x == 0) FileHelper.writeToFile(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perRow_mv.mv"); // for finding mv bits per row
						else FileHelper.writeToFileAppend(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perRow_mv.mv");
						if ((RCflag == 2 || RCflag == 3) && pass == 1) {
							if (x == 0 && y == 0) FileHelper.writeToFile(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perFrame_mv.mv"); // for finding mv bits per row
							else FileHelper.writeToFileAppend(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perFrame_mv.mv");
						}
					}
				}
				else {
					prevMV = mvSubBlock4;
					vbsBytes[blockIndex] = 1; // split
					writeToMVFile(mvMergedBlocksDiffEncoded, blockIndex);
					if (RCflag != 0) {
						vbs_perRow[vbs_perRowIndex++] = 1; // for finding vbs bit per row
						if (x == 0) FileHelper.writeToFile(Helper.intArrToShortArr(mvMergedBlocksDiffEncoded), "perRow_mv.mv"); // for finding mv bits per row
						else FileHelper.writeToFileAppend(Helper.intArrToShortArr(mvMergedBlocksDiffEncoded), "perRow_mv.mv");
						if ((RCflag == 2 || RCflag == 3) && pass == 1) {
							if (x == 0 && y == 0) {
								File file2 = new File(System.getProperty("user.dir") + "/output/perFrame_mv.mv"); file2.delete();
								FileHelper.writeToFile(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perFrame_mv.mv");
								} // for finding mv bits per row
							else FileHelper.writeToFileAppend(Helper.intArrToShortArr(mvWholeBlockDiffEncoded), "perFrame_mv.mv");
						}
					}
				}
					
				blockIndex++;
			}
			if (RCflag != 0) {
                int actualBitSpent = RateControl.getActualBitSpent("perRow_mv.mv", qtc_perRow, y, width, blockSize, vbs_perRow); // finds actual bits spent (qtc + mv + vbs bitcounts)
				File file = new File(System.getProperty("user.dir") + "/output/perRow_mv.mv"); file.delete(); // for cleaner console output
				if (RCflag == 1) {
					remainingBudget_perFrame -= actualBitSpent; // finds remaining budget of the frame
	                int remainingRows = (height/blockSize) - (y/blockSize + 1); // finds remaining rows that need to be encoded
					if (remainingRows != 0) newBitBudget_perRow = remainingBudget_perFrame/remainingRows; // if not last line, finds new budget per row
					// System.out.println("     Acutal Bit: " + actualBitSpent + " Remaining Budget/Frame: " + remainingBudget_perFrame);
				}
				else if ((RCflag == 2 || RCflag == 3) && pass == 1) {
					rowBitcount1[frameIndex][(int)(y / blockSize)] = actualBitSpent;
				}
				else if ((RCflag == 2 || RCflag == 3) && pass == 2) {
					remainingBudget_perFrame -= actualBitSpent; // finds remaining budget of the frame
	                int remainingRows = (height/blockSize) - (y/blockSize + 1); // finds remaining rows that need to be encoded
					if (remainingRows != 0) MultiplePass.updateProportion(frameIndex, rowBitcount2, (int)(y / blockSize));
				}
			}
		}
		if ((RCflag == 2 || RCflag == 3) && pass == 1) {
			// calculate proportion, set rowBitcount2
			MultiplePass.setrowBitCount2(frameIndex, rowBitcount1, rowBitcount2);
			// decide if this frame is a scene changing
			int frameBitCount = RateControl.getActualBitSpent("perFrame_mv.mv", finalQTC, 0, width, height, vbsBytes);
			File file = new File(System.getProperty("user.dir") + "/output/perFrame_mv.mv"); file.delete(); // for cleaner console output
			if (frameBitCount > threshold)
				sc[frameIndex] = 1;
		}
		return finalReconstructed;
	}
	
	
	private int encoderIntraTQRIR(byte [] currFrame, int y, int x, int blockSize, int prevMode, 
			short[] residualFrame, int[] transResidual, int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, byte[] reconstructedFrame) {

		
		int mode = modGen.generateModeByBlock(currFrame, reconstructedFrame, y, x, blockSize, prevMode);
		resGen.generateIframeResidualBlock(currFrame, reconstructedFrame, residualFrame, mode, y, x, blockSize);
		
		resApp.transfOnBlock(residualFrame, transResidual, x, y, blockSize);
		resApp.quantOnBlock(transResidual, qtcResidual, x, y, blockSize);
		resApp.rescaleOnBlock(qtcResidual, rescaledResidual, x, y, blockSize);
		resApp.invTransfOnBlock(rescaledResidual, invTransResidual, x, y, blockSize);
		recGen.reconstructedIframeBlock(reconstructedFrame, invTransResidual, reconstructedFrame, x, y, blockSize, mode);
	
		return mode;
	}
	
	public byte[] encodeIntra(int [] finalQTC) {
		int prevMode = 0, blockIndex = 0;
		byte[] reconstructedFrame1 = new byte[width * height];
		byte[] reconstructedFrame2 = new byte[width * height];
		byte[] finalReconstructed = new byte[width * height];
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
		int remainingBudget_perFrame = bitBudget_perFrame;// starting bit budget of a frame (based on fps and bitrate)
		int newBitBudget_perRow = bitBudget_perRow; // bit budget of first row (based on fps and bitrate)
		for (int y = 0; y < height; y += blockSize) {
			int[] qtc_perRow = new int[width*blockSize]; // qtc bits per row
			byte[] vbs_perRow = new byte[width/blockSize]; // vbs bits per row
			int vbs_perRowIndex = 0;
			if (RCflag == 1) {
				resApp.qp = RateControl.findQP(newBitBudget_perRow, height, true); // finds appropriate QP value from QP table to meet the target per row bit budget
				writeToQPFile(resApp.qp, y); // writes to qp.txt (required for entropy encode)
                // System.out.println("---> At I Frame #" + frameIndex + " Row #" + y/blockSize + ": Budget/Row = " + newBitBudget_perRow + ", QP = " +resApp.qp);
            }
			else if ((RCflag == 2 || RCflag == 3) && pass == 2) {
				//set newBitBudget_perRow
				newBitBudget_perRow = MultiplePass.getRowBitBudget(remainingBudget_perFrame, frameIndex, rowBitcount2, (int)y / blockSize);
				resApp.qp = mp.findQP(newBitBudget_perRow, height, true);
				writeToQPFile(resApp.qp, y);
                // System.out.println("---> At I Frame #" + frameIndex + " Row #" + y/blockSize + ": Budget/Row = " + newBitBudget_perRow + ", QP = " +resApp.qp);
			}
			for (int x = 0; x < width; x+= blockSize) {				
				if (x == 0) 
					prevMode = 0;
				// Step 1. Do whole block, AKA blocksize = blocksize from config
				int mode1 = encoderIntraTQRIR(currFrame, y, x, blockSize, prevMode, 
				residualFrame1, transResidual1, qtcResidual1, rescaledResidual1, invTransResidual1,reconstructedFrame1);
				
				// Calculate rdo
				double rdoBlock1 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame1, y, x, resApp.getQP(), blockSize, 
						width, new int[] {mode1-prevMode}, qtcResidual1);
				
				double rdoBlock2 = Double.MAX_VALUE;
				int [] diffEncodedMode = null;
				int [] mode_vbs = null;
				int modeSub4 = 0;
				if (VariableBlockSize.isVBSEnabled()) {
					// Step 2. Do 4 sub blocks, AKA blocksize = blocksize/2
					boolean qpChanged = false;
					if (resApp.getQP() != 0) {
						resApp.setQP(resApp.getQP()-1); 
						qpChanged = true;
					}
					int modeSub1 = encoderIntraTQRIR(currFrame, y, x, blockSize/2, prevMode, 
							residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);
					
					int modeSub2 = encoderIntraTQRIR(currFrame, y, x+(blockSize/2), blockSize/2, modeSub1, 
							residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);
					
					int modeSub3 = encoderIntraTQRIR(currFrame, y+(blockSize/2), x, blockSize/2, modeSub2, 
							residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);
					
					modeSub4 = encoderIntraTQRIR(currFrame, y+(blockSize/2), x+(blockSize/2), blockSize/2, modeSub3, 
							residualFrame2, transResidual2, qtcResidual2, rescaledResidual2, invTransResidual2,reconstructedFrame2);
					mode_vbs = new int[] {modeSub1, modeSub2, modeSub3, modeSub4};
					
					// Calculate rdo
					diffEncodedMode = new int[] {modeSub1 - prevMode, modeSub2 - modeSub1, modeSub3-modeSub2, modeSub4-modeSub3};
					rdoBlock2 = VariableBlockSize.calculateRdoForBlock(currFrame, reconstructedFrame2, y, x, resApp.getQP(), blockSize, 
							width, diffEncodedMode, qtcResidual2);
					if (qpChanged) resApp.setQP(resApp.getQP()+1);
				}
				
				for (int yb = y; yb < y + blockSize; yb++) {
					for (int xb = x; xb < x + blockSize; xb++) {
						
						// note: rdoBlock2 defaults to Double.Max_VALUE unless VBS is enabled.
						if (rdoBlock1 < rdoBlock2) {
							finalReconstructed[yb * width + xb] = reconstructedFrame1[yb * width + xb];
							reconstructedFrame2[yb * width + xb] = reconstructedFrame1[yb * width + xb];
							finalQTC[yb * width + xb] = qtcResidual1[yb * width + xb];
							if (RCflag != 0) qtc_perRow[(yb-y)*width + xb] = qtcResidual1[yb * width + xb]; // for finding qtc bits per row
							//System.out.println("Taking reconstructed block with whole block operation.");
						}
						else {
							finalReconstructed[yb * width + xb] = reconstructedFrame2[yb * width + xb];
							reconstructedFrame1[yb * width + xb] = reconstructedFrame2[yb * width + xb];
							finalQTC[yb * width + xb] = qtcResidual2[yb * width + xb];
							if (RCflag != 0) qtc_perRow[(yb-y)*width + xb] = qtcResidual2[yb * width + xb]; // for finding qtc bits per row
							//System.out.println("Taking reconstructed block with sub block operation.");
						}
							
					}
				}
				
				if (rdoBlock1 < rdoBlock2) {
					writeToMVFile(new int[] {mode1-prevMode}, blockIndex);
					vbsBytes[blockIndex] = 0; // no split
					if (RCflag != 0) {
						vbs_perRow[vbs_perRowIndex++] = 0; // for finding vbs bit per row
						if (x == 0) FileHelper.writeToFile(Helper.intArrToShortArr(new int[] {mode1-prevMode}), "perRow_mv.mv"); // for finding mv bits per row
						else FileHelper.writeToFileAppend(Helper.intArrToShortArr(new int[] {mode1-prevMode}), "perRow_mv.mv");
						if ((RCflag == 2 || RCflag == 3) && pass == 1) {
							if (x == 0 && y == 0) FileHelper.writeToFile(Helper.intArrToShortArr(new int[] {mode1-prevMode}), "perFrame_mv.mv"); // for finding mv bits per row
							else FileHelper.writeToFileAppend(Helper.intArrToShortArr(new int[] {mode1-prevMode}), "perFrame_mv.mv");
						}
					}
					prevMode = mode1;
				}
				else {
					writeToMVFile(diffEncodedMode, blockIndex);
					vbsBytes[blockIndex] = 1; // split
					prevMode = modeSub4;
					if (RCflag != 0) {
						vbs_perRow[vbs_perRowIndex++] = 1; // for finding vbs bit per row
						if (x == 0) FileHelper.writeToFile(Helper.intArrToShortArr(diffEncodedMode), "perRow_mv.mv"); // for finding mv bits per row
						else FileHelper.writeToFileAppend(Helper.intArrToShortArr(diffEncodedMode), "perRow_mv.mv");
						if ((RCflag == 2 || RCflag == 3) && pass == 1) {
							if (x == 0 && y == 0) {
								File file2 = new File(System.getProperty("user.dir") + "/output/perFrame_mv.mv"); file2.delete();
								FileHelper.writeToFile(Helper.intArrToShortArr(diffEncodedMode), "perFrame_mv.mv"); 
							}// for finding mv bits per row
							else FileHelper.writeToFileAppend(Helper.intArrToShortArr(diffEncodedMode), "perFrame_mv.mv");
						}
					}
				}

				blockIndex++;
			}
			if (RCflag != 0) {
                int actualBitSpent = RateControl.getActualBitSpent("perRow_mv.mv", qtc_perRow, y, width, blockSize, vbs_perRow); // finds actual bits spent (qtc + mv + vbs bitcounts)
				File file = new File(System.getProperty("user.dir") + "/output/perRow_mv.mv"); file.delete(); // for cleaner console output
				if (RCflag == 1) {
					remainingBudget_perFrame -= actualBitSpent; // finds remaining budget of the frame
	                int remainingRows = (height/blockSize) - (y/blockSize + 1); // finds remaining rows that need to be encoded
					if (remainingRows != 0) newBitBudget_perRow = remainingBudget_perFrame/remainingRows; // if not last line, finds new budget per row
					// System.out.println("     Acutal Bit: " + actualBitSpent + " Remaining Budget/Frame: " + remainingBudget_perFrame);
				}
				else if ((RCflag == 2 || RCflag == 3) && pass == 1) {
					rowBitcount1[frameIndex][(int)(y / blockSize)] = actualBitSpent;
				}
				else if ((RCflag == 2 || RCflag == 3) && pass == 2) {
					remainingBudget_perFrame -= actualBitSpent; // finds remaining budget of the frame
	                int remainingRows = (height/blockSize) - (y/blockSize + 1); // finds remaining rows that need to be encoded
					if (remainingRows != 0) MultiplePass.updateProportion(frameIndex, rowBitcount2, (int)(y / blockSize));
				}
			}
		}
		if ((RCflag == 2 || RCflag == 3) && pass == 1) {
			// calculate proportion, set rowBitcount2
			MultiplePass.setrowBitCount2(frameIndex, rowBitcount1, rowBitcount2);
		}
		return finalReconstructed;
	}
	
	public void startEncoding() {
		if (RCflag == 2 || RCflag == 3) {
			allFrameBuffer = new ArrayList<ArrayList<byte[][]>>();
			prevQP = 4;
			threshold = MultiplePass.findThreshold(height, 4);
			PropertyHelper ph = new PropertyHelper();
			ph.setProperty("quantizationPar", "" + prevQP);
			// encode for pass 1
			pass = 1;
			for (int i = 0; i < frameLength; i++) {
				frameIndex = i;
				System.out.println("Working on Pass 1, Frame # " + frameIndex);
		        setCurr();
		        // Storage for entire frame
		        byte[] reconstructed = new byte[width * height];
		        int[] qtc = new int[width*height];
		        vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
		        if (frameIndex % I_Period != 0) {
		        	reconstructed = encodeInter(qtc);
		        	for (int j = numRef - 1; j > 0; j--)
		          		reference[j] = reference[j - 1];
		        	reference[0] = reconstructed;
		        }
		        else {
		        	reconstructed = encodeIntra(qtc);
		        	setPredictor(reconstructed);
		        }
		        // Write data we care about to file
		        writeToReconstructedFile(reconstructed);
		        int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
				writeToQtcFile(Helper.intArrToShortArr(qtc));
				writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded));
				writeToVBSFile(vbsBytes);
			}
			EntropyEncDec.generateAllEntropyEncodedFiles();
			//update table
			int avgRowBitcount = MultiplePass.findAvgRowBitcount(rowBitcount1);
			//System.out.println(avgRowBitcount);
			mp.updataTable(height, avgRowBitcount, prevQP);
			// encode pass 2
			pass = 2;
			for (int i = 0; i < frameLength; i++) {
				frameIndex = i;
				System.out.println("Working on Pass 2, Frame # " + frameIndex);
		        setCurr();
		        // Storage for entire frame
		        byte[] reconstructed = new byte[width * height];
		        int[] qtc = new int[width*height];
		        vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
		        if (frameIndex % I_Period != 0 && sc[frameIndex] != 1) {
		        	reconstructed = encodeInter(qtc);
		        	for (int j = numRef - 1; j > 0; j--)
		          		reference[j] = reference[j - 1];
		        	reference[0] = reconstructed;
		        }
		        else {
		        	reconstructed = encodeIntra(qtc);
		        	setPredictor(reconstructed);
		        }
		        // Write data we care about to file
		        writeToReconstructedFile(reconstructed);
		        int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
				writeToQtcFile(Helper.intArrToShortArr(qtc));
				writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded));
				writeToVBSFile(vbsBytes);
			}
			writeToSCFile(Helper.intArrToByteArr(sc));
			EntropyEncDec.generateAllEntropyEncodedFiles();
		}
		else {
			pass = -1;
			for (int i = 0; i < frameLength; i++) {
				frameIndex = i;
				System.out.println("Working on Frame # " + frameIndex);
		        setCurr();
		        // Storage for entire frame
		        byte[] reconstructed = new byte[width * height];
		        int[] qtc = new int[width*height];
		        vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
		        if (frameIndex % I_Period != 0) {
		        	reconstructed = encodeInter(qtc);
		        	for (int j = numRef - 1; j > 0; j--)
		          		reference[j] = reference[j - 1];
		        	reference[0] = reconstructed;
		        }
		        else {
		        	reconstructed = encodeIntra(qtc);
		        	setPredictor(reconstructed);
		        }
		        // Write data we care about to file
		        writeToReconstructedFile(reconstructed);
		        int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
				writeToQtcFile(Helper.intArrToShortArr(qtc));
				writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded));
				writeToVBSFile(vbsBytes);
			}
			EntropyEncDec.generateAllEntropyEncodedFiles();
		}
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
	
	public void writeMVbeforeDiffEnc(int[] mv, int blockIndex) {
		if (frameIndex == 0 && blockIndex == 0) FileHelper.writeToFile(Helper.intArrToShortArr(mv), "visualPartC.mv");
		else FileHelper.writeToFileAppend(Helper.intArrToShortArr(mv), "visualPartC.mv");
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

	public void writeToSCFile(byte[] sc) {
		FileHelper.writeToFile(sc, scFileName);
	}
	
	public void writeToQPFile(int qp, int rowIndex) {
		byte[] qp_byte = {(byte) qp};
		if (frameIndex == 0 && rowIndex == 0) FileHelper.writeToFile(qp_byte, qpFileName);
		else FileHelper.writeToFileAppend(qp_byte, qpFileName);
	}

	// put the current frame y only value into currFrame
	public void setCurr() {
		for (int i = 0; i < width * height; i++) {
			currFrame[i] = yOnly[frameIndex * width * height + i];
		}
	}

	public static void main(String[] args) {
		Encoder en = new Encoder();
//		 en.test4a();
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("encoding time: " + (endTimeMs - startTimeMs));
	}


}

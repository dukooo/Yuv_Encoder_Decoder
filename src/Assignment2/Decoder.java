package Assignment2;

import java.util.ArrayList;

public class Decoder {

	int frameLength, blockSize, width, height, numRef, I_Period;
	int mvIndex, frameIndex;
	String mvFileName, qtcFileName, vbsFileName, decodedFileName;
	byte[][] reference; // reference[frame index][pixel index]
	short[] mvDiff;
	short[] qtc;
	byte[] vbsBytes;
    ResidualApproximation resApp;
	ResidualBlockGenerator resGen;
	MotionVectorGenerator mvGen;
	ReconstructedBlockGenerator recGen;
	ModeGenerator modGen;
	boolean FMEenabled;

	public Decoder() {
		PropertyHelper ph = new PropertyHelper();
		frameLength = Integer.valueOf(ph.getProperty("frameCount"));
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		numRef = Integer.valueOf(ph.getProperty("nRefFrames"));
		I_Period = Integer.valueOf(ph.getProperty("I_Period"));
        mvFileName = ph.getProperty("mvEntropyDecodedFileName");
        qtcFileName = ph.getProperty("qtcEntropyDecodedFileName");
        vbsFileName = ph.getProperty("vbsEntropyDecodedFileName");
		decodedFileName = ph.getProperty("decReconstructedFileName");
		FMEenabled = Boolean.parseBoolean(ph.getProperty("FMEEnable"));
		resApp = new ResidualApproximation();
		resGen = new ResidualBlockGenerator();
		mvGen = new MotionVectorGenerator();
		recGen = new ReconstructedBlockGenerator();
		modGen = new ModeGenerator();
		EntropyEncDec.generateAllEntropyDecodedFiles();
	}
	
	public void setPredictor() {
		reference = new byte[numRef][width * height];
		for (int i = 0; i < numRef; i++) {
			for (int j = 0; j < width * height; j++) reference[i][j] = (byte) 128;
		}
	}
	
	//TODO: add entropy decoding for mv, qtc.
	public void decodeMV_QTC_VBS() {
        String outputDir = System.getProperty("user.dir") + "/output/";
		mvDiff = FileHelper.readShortFile(outputDir + mvFileName);
		qtc = FileHelper.readShortFile(outputDir + qtcFileName);
        vbsBytes = FileHelper.readByteFile(outputDir + vbsFileName);
	}
	
	public int[] setQTCResidual() {
		int[] qtcResidual = new int[width * height];
		for (int i = 0; i < width * height; i++) {
			qtcResidual[i] = (int)qtc[frameIndex * width * height + i];
		}
		return qtcResidual;
	}
	
	public void decoderInterTQRIR(int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, 
			byte[] decodedFrame, int blocksize, int[] mv, int x, int y, ArrayList<byte[][]> buffers) {
		resApp.rescaleOnBlock(qtcResidual, rescaledResidual, x, y, blocksize);
		resApp.invTransfOnBlock(rescaledResidual, invTransResidual, x, y, blocksize);
		recGen.reconstructedPframeBlock(decodedFrame, invTransResidual, reference, x, y, blocksize, mv, buffers);
	}
	
	public byte[] decodeInter() {
		int blockIndex = 0;
		int[] prevMV = new int[3];
		prevMV[0] = 0;
		prevMV[1] = 0;
		prevMV[2] = 0;
		int[] qtcResidual = setQTCResidual();
		int[] rescaledResidual = new int[width * height];
		short[] invTransResidual = new short[width * height];
		byte[] decodedFrame = new byte[width * height];
		ArrayList<byte[][]> buffers;
		if (FMEenabled){
			InterpolationBuffer interpolationBuffer = new InterpolationBuffer();
			buffers = interpolationBuffer.buildBuffers(reference);
		}
		else buffers = new ArrayList<>();
		for (int y = 0; y < height; y+=blockSize) {
			for (int x = 0; x < width; x+=blockSize) {
				int[] currMV;
				if (VariableBlockSize.isVBSEnabled()) {
					if (vbsBytes[frameIndex * width * height / blockSize / blockSize + blockIndex] == 0) { // no split
						int[] mv = {mvDiff[mvIndex] + prevMV[0], mvDiff[mvIndex + 1] + prevMV[1], mvDiff[mvIndex + 2] + prevMV[2]};
						currMV = mv;
						decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize, mv, x, y, buffers);
					}
					else { // split
						resApp.setQP(resApp.getQP()-1);
						int[] mvSub1 = {mvDiff[mvIndex] + prevMV[0], mvDiff[mvIndex + 1] + prevMV[1], mvDiff[mvIndex + 2] + prevMV[2]};
						mvIndex+=3;
						int[] mvSub2 = {mvDiff[mvIndex] + mvSub1[0], mvDiff[mvIndex + 1] + mvSub1[1], mvDiff[mvIndex + 2] + mvSub1[2]};
						mvIndex+=3;
						int[] mvSub3 = {mvDiff[mvIndex] + mvSub2[0], mvDiff[mvIndex + 1] + mvSub2[1], mvDiff[mvIndex + 2] + mvSub2[2]};
						mvIndex+=3;
						int[] mvSub4 = {mvDiff[mvIndex] + mvSub3[0], mvDiff[mvIndex + 1] + mvSub3[1], mvDiff[mvIndex + 2] + mvSub3[2]};
						currMV = mvSub4;
						decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, mvSub1, x, y, buffers);
						decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, mvSub2, x+(blockSize/2), y, buffers);
						decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, mvSub3, x, y+(blockSize/2), buffers);
						decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, mvSub4, x+(blockSize/2), y+(blockSize/2), buffers);
						resApp.setQP(resApp.getQP()+1);
					}
				}
				else {
					int[] mv = {mvDiff[mvIndex] + prevMV[0], mvDiff[mvIndex + 1] + prevMV[1], mvDiff[mvIndex + 2] + prevMV[2]};
					currMV = mv;
					decoderInterTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize, mv, x, y, buffers);
				}
				blockIndex++;
				mvIndex+=3;
				if (x == width - blockSize) {
					prevMV[0] = 0;
					prevMV[1] = 0;
					prevMV[2] = 0;
				}
				else
					prevMV = currMV;
			}
		}
		return decodedFrame;
	}
	
	public void decoderIntraTQRIR(int[] qtcResidual, int[] rescaledResidual, short[] invTransResidual, 
			byte[] decodedFrame, int blocksize, int mode, int x, int y) {
		resApp.rescaleOnBlock(qtcResidual, rescaledResidual, x, y, blocksize);
		resApp.invTransfOnBlock(rescaledResidual, invTransResidual, x, y, blocksize);
		recGen.reconstructedIframeBlock(decodedFrame, invTransResidual, decodedFrame, x, y, blocksize, mode);
	}
	
	public byte[] decodeIntra() {
		int prevMode = 0, blockIndex = 0;
		byte[] decodedFrame = new byte[width * height];
		int[] qtcResidual = setQTCResidual();
		int[] rescaledResidual = new int[width * height];
		short[] invTransResidual = new short[width * height];
		for (int y = 0; y < height; y += blockSize) {
			for (int x = 0; x < width; x+= blockSize) {
				int mode;
				if (VariableBlockSize.isVBSEnabled()) {
					if (vbsBytes[frameIndex * width * height / blockSize / blockSize + blockIndex] == 0) { // no split
						mode = mvDiff[mvIndex] + prevMode;
						decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize, mode, x, y);
					}
					else { 
						resApp.setQP(resApp.getQP()-1);
						int modeSub1 = mvDiff[mvIndex] + prevMode;
						mvIndex++;
						int modeSub2 = modeSub1 + mvDiff[mvIndex];
						mvIndex++;
						int modeSub3 = modeSub2 + mvDiff[mvIndex];
						mvIndex++;
						mode = modeSub3 + mvDiff[mvIndex];
						decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, modeSub1, x, y);
						decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, modeSub2, x+(blockSize/2), y);
						decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, modeSub3, x, y+(blockSize/2));
						decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize/2, mode, x+(blockSize/2), y+(blockSize/2));
						resApp.setQP(resApp.getQP()+1);
					}
				}
				else {
					mode = mvDiff[mvIndex] + prevMode;
					decoderIntraTQRIR(qtcResidual, rescaledResidual, invTransResidual, decodedFrame, blockSize, mode, x, y);
				}
				if (x == width - blockSize) {
					prevMode = 0;
				}
				else
					prevMode = mode;
				blockIndex++;
				mvIndex++;
			}
		}
		return decodedFrame;
	}
	
    public void startDecoding() {
        reference = new byte[numRef][width * height];
        byte[] decoded = new byte[width * height];
        setPredictor();
        decodeMV_QTC_VBS();
        for (int i = 0; i < frameLength; i++) {
            frameIndex = i;
			System.out.println("Working on Frame # " + frameIndex);
	        if (frameIndex % I_Period != 0) {
	        	decoded = decodeInter();
	        	for (int j = numRef - 1; j > 0; j--)
	          		reference[j] = reference[j - 1];
	        	reference[0] = decoded;
	        }
	        else {
	        	decoded = decodeIntra();
	        	setPredictor();
	        }
	        writeToDecodedFile(decoded);
        }
    }
    
	public void writeToDecodedFile(byte[] decodedFrame) {
		if (frameIndex == 0)
			FileHelper.writeToFile(decodedFrame, decodedFileName);
		else
			FileHelper.writeToFileAppend(decodedFrame, decodedFileName);
	}
	
	public static void main(String[] args) {
        Decoder de = new Decoder();
        de.startDecoding();
        //test
        String outputDir = System.getProperty("user.dir") + "/output/";
        byte[] decoded = FileHelper.readByteFile(outputDir + de.decodedFileName);
        byte[] reconstructed = FileHelper.readByteFile(outputDir + "reconstructedYOnly.yuv");
        for (int i = 0; i < decoded.length; i++) {
        	if (decoded[i] != reconstructed[i])
        		System.out.println(i / (de.width * de.height));
        }
	}
}

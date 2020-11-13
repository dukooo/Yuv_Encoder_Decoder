package Assignment3;



public class VariableBlockSize {
	
	public VariableBlockSize() {
		return;
	}
	
	public static boolean isVBSEnabled() {
		// first always check config, if feature flag turned off, return false
		PropertyHelper ph = new PropertyHelper();
		if (ph.getProperty("vbsEnable").equals("0")) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param originalFrame
	 * @param reconstructedFrame
	 * @param rowCoordinate
	 * @param colCoordinate
	 * @param qp
	 * @param blocksize
	 * @param vidWidth
	 * @param mvDiffEncoded
	 * @param qtcResidual, qtc for the whole frame
	 * @return
	 */
	public static double calculateRdoForBlock(byte[] originalFrame, byte[] reconstructedFrame, int rowCoordinate, int colCoordinate, 
			int qp, int blocksize, int vidWidth, int[] mvDiffEncoded, int[] qtcResidual) {
		
		// 1. Calculate SAD for 2 blocks
		int sad = Helper.calculateSAD(originalFrame, colCoordinate, rowCoordinate, reconstructedFrame, colCoordinate, rowCoordinate, blocksize, vidWidth);
		
		// 2. Get bit count of entropy encoded mv+quantized residuals
		int bitCnt = EntropyEncDec.expGolombEncBitCount(mvDiffEncoded);
		int [] qtcBlk = Helper.extractBlockFromFrame(qtcResidual, colCoordinate, rowCoordinate, blocksize, vidWidth); 
		int[] qtcRle = EntropyEncDec.runLevelEnc(qtcBlk);
		bitCnt += EntropyEncDec.expGolombEncBitCount(qtcRle);
		
		// 3. Use equation from Lecture 5
		PropertyHelper ph = new PropertyHelper();
		double rdoConstant = Double.parseDouble(ph.getProperty("rdoConstant"));
		double rdo = (rdoConstant * Math.pow(2, ((qp-12)/3))) * (bitCnt);
		rdo += sad;
		
		return rdo;
	}
	
	public static void main(String[] args) {
		//TODO: Add code to generate image.
	}
	

}

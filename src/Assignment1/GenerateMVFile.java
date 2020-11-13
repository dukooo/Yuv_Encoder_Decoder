/**
 * exercise 3 of assignment 1, ECE 1783
 */
package Assignment1;

import java.util.ArrayList;


public class GenerateMVFile extends BasicMotionEstimation{

	int blockSize;
	int range;
	String filename;

	public GenerateMVFile(String filename, int width, int height, int frameLength, int blockSize, int range) {
		super(filename, width, height, frameLength);
		this.blockSize = blockSize;
		this.range = range;
		this.filename=filename;
	}

	/**
	 * Generate a File with all Motion Vectors in byte format. The file will have bytes where
	 * Byte 1 = Motion Vector X (horizontal motion) of 1st (ixi) block of the input file
	 * Byte 2 = Motion Vector Y (vertical motion) of 1st (ixi) block of the input file
	 * Byte 3 = Motion Vector X (horizontal motion) of 2nd (ixi) block of the input file
	 * Byte 4 = Motion Vector Y (vertical motion) of 2nd (ixi) block of the input file
	 * @param outputFileName, name of output file, output will be written to "output" directory
	 */
	public void generateMVFile(String outputFileName) {

		// Print the motion vector for the first 10 frames taking i = 64, r = 1;
		BasicMotionEstimation yuv = new BasicMotionEstimation(this.filename, width, height, frameLength);
		int i = 0;
		int writeCount = 0;
		// Need 2 (X, Y) bytes for each block.
		byte[] mvBytes = new byte[2 * frameLength* (int)(super.getHeightAfterPadding(blockSize)/blockSize*super.getWidthAfterPadding(blockSize)/blockSize)];

		while (i < frameLength) {
			yuv.readOneFrame();
			ArrayList<int[]> mv = yuv.findBestPrediction(64, 1);
			for (int[] item : mv) {

				mvBytes[writeCount] = (byte) item[0];
				writeCount++;
				mvBytes[writeCount] = (byte) item[1];
				writeCount++;
			}
			i++;
		}

		FileHelper.writeToFile(mvBytes, outputFileName);
	}
}

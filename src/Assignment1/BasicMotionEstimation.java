/**
 * exercise 3 of assignment 1, ECE 1783
 */
package Assignment1;

import java.util.ArrayList;

public class BasicMotionEstimation extends ReadYOnly{
	
	public BasicMotionEstimation(String filename, int width, int height, int frameLength) {
		super(filename, width, height, frameLength);
	}
	
	/**
	 * Find the best prediction for the current frame (currFrame) from the previous frame (prevFrame)
	 * @param i, each frame is divided into (ixi) blocks
	 * @param r, 
	 * @return an ArrayList of integer Pairs. Key of the pair is x and value is y
	 */
	public ArrayList<int[]> findBestPrediction(int i, int r) {
		int heightAfterPadding = getHeightAfterPadding(i);
		int widthAfterPadding = getWidthAfterPadding(i);
		int[] ref = getReferenceFrame(i);
		int[] cur = paddingYOnly(currFrame, i);
		ArrayList<int[]> mv = new ArrayList<int[]>();
		int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
		// for each block
		for (int h1 = 0; h1 < heightAfterPadding; h1 += i) {
			for (int w1 = 0; w1 < widthAfterPadding; w1 += i) {
				// check SAD value with each reference block
				for (int h2 = h1 - r; h2 <= h1 + r; h2++) {
					for (int w2 = w1 - r; w2 <= w1 + r; w2++) {
						// ignore blocks falling partially or totally outside the previous frame
						if (w2 >= 0 && w2 + i <= widthAfterPadding && h2 >= 0 && h2 + i <= heightAfterPadding) {
							int sad = calculateSAD(cur, w1, h1, ref, w2, h2, i);
							if (sad < minSAD) {
								minSAD = sad;
								minx = w2 - w1;
								miny = h2 - h1;
							}
							else if (sad == minSAD) {
								if ((Math.abs(w2 - w1) + Math.abs(h2 - h1) < Math.abs(minx) + Math.abs(miny))
								|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
								 	&& Math.abs(h2 - h1) < Math.abs(miny))
								|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
								 	&& Math.abs(h2 - h1) == Math.abs(miny) && Math.abs(w2 - w1) < Math.abs(minx))) {
									minx = w2 - w1;
									miny = h2 - h1;
								}
							}
						}
					}
				}
				int[] xy = new int[2];
				xy[0] = minx;
				xy[1] = miny;
				mv.add(xy);
			}
		}
		return mv;
	}
	
	/**
	 * Get the Y-Only-Block-Average file of the previous frame
	 * @param i, each frame is divided into (ixi) blocks
	 * @return the Y-Only-Block-Average file of the previous frame
	 */
	public int[] getReferenceFrame(int i) {
		int heightAfterPadding = getHeightAfterPadding(i);
		int widthAfterPadding = getWidthAfterPadding(i);
		int[] ref;
		if (prevFrame != null) 
			ref = getYOnlyBlockAverage(prevFrame, i);
		else { // Assume the reference contains all samples having a value of 128
			ref = new int[widthAfterPadding * heightAfterPadding];
			for (int n = 0; n < widthAfterPadding * heightAfterPadding; n++) 
				ref[n] = 128;
		}
		return ref;
	}
	
	/**
	 * calculate Sum of Absolute Difference (SAD) between 
	 * @param cur, Y-only file of the current frame
	 * @param w1, width of the top-left pixel in the block of current frame
	 * @param h1, height of the top-left pixel in the block of current frame
	 * @param ref, Y-Only-Block-Average file of the previous frame
	 * @param w2, width of the top-left pixel in the block of previous frame
	 * @param h2, height of the top-left pixel in the block of previous frame
	 * @param i, each frame is divided into (ixi) blocks
	 * @return sad value between these two blocks
	 */
	public int calculateSAD(int[] cur, int w1, int h1, int[] ref, int w2, int h2, int i) {
		int widthAfterPadding = getWidthAfterPadding(i);
		int sad = 0;
		for (int k = 0; k < i; k++) {
			int wOfCur = w1 + k;
			int wOfRef = w2 + k;
			for (int j = 0; j < i; j++) {
				int hOfCur = h1 + j;
				int hOfRef = h2 + k;
				sad += Math.abs(cur[hOfCur * widthAfterPadding + wOfCur] - ref[hOfRef * widthAfterPadding + wOfRef]);
			}
		}
		return sad;
	}
}

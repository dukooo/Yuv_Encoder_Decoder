package Assignment2;

import java.util.ArrayList;

public class MotionVectorGenerator {

	int width, height, numRef, rangeR, blockSize;
	boolean FMEenabled;
	public MotionVectorGenerator() {
		PropertyHelper ph = new PropertyHelper();
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		numRef = Integer.valueOf(ph.getProperty("nRefFrames"));
		rangeR = Integer.valueOf(ph.getProperty("rangeR"));
		FMEenabled = Boolean.parseBoolean(ph.getProperty("FMEEnable"));
	}
	
	
	/**
	 * Can be called same as generateMVbyBlock. The difference is that this function uses fast motion estimation
	 * and nearest neighbour search. This is a replica of generateMVbyBlockFast that takes ArrayList and can support 
	 * Fractional ME at the same time as Fast
	 * @param currFrame
	 * @param buffers
	 * @param h1
	 * @param w1
	 * @param bs
	 * @param prevMV
	 * @return
	 */
	public int[] generateMVbyBlockFast(byte[] currFrame, ArrayList<byte[][]> buffers, int h1, int w1, int bs, int[] prevMV) {
		int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, minf = Integer.MAX_VALUE;
		boolean done = false;
		byte[][] interpolatedBuffer = new byte[0][0];
		// Search origin variables
		// int soX = w1 + prevMV[1], soY = h1 + prevMV[2], soF = prevMV[0];
		int soX = 2*bs*w1 + bs*prevMV[1], soY = 2*bs*h1 + bs*prevMV[2], soF = prevMV[0];
		int numbBlcok_width_expanded = 0; 
        int numbBlcok_height_expanded = 0;
		for (int h = 0; h < height; h++) if (h + bs <= height) numbBlcok_height_expanded++;
        for (int w = 0; w < width; w++) if (w + bs <= width) numbBlcok_width_expanded++;
		int numbBlcok_width_buffer = numbBlcok_width_expanded * 2 - 1; 
        int numbBlcok_height_buffer = numbBlcok_height_expanded * 2 - 1;
		int width_buffer = bs * numbBlcok_width_buffer; 
        int height_buffer = bs * numbBlcok_height_buffer;
		
		// Step 1. Search origin (aka block at w1 and h1) on all reference frames
		for (int f = 0; f < numRef; f++) {
			interpolatedBuffer = buffers.get(f);
			// int sad = calculateSAD(currFrame, interpolatedBuffer, w1, h1, f, w1, h1, bs);
			int sad = calculateSAD(currFrame, interpolatedBuffer, w1, h1, f, 2*bs*w1, 2*bs*h1, bs);
			if (sad < minSAD) {
				minSAD = sad;
				minf = f;
				minx = 2*bs*w1;
				miny = 2*bs*h1;
			}
			else if (sad == minSAD) {
				minf = Math.min(f, minf); // Pick the smaller f number if we have a tie
				minx = 2*bs*w1;
				miny = 2*bs*h1;
				minSAD = sad;
			}
		}
		
		// Step 2. Search by setting origin to location pointed to by prevMV
		interpolatedBuffer = buffers.get(soF);
		while (!done) {
			
			/*
			 * 0 -> self
			 * 1 -> vertical up neighbour
			 * 2 -> vertical down neighbour
			 * 3 -> left neighbour
			 * 4 -> right neighbour
			*/
			int sad = Integer.MAX_VALUE;
			int w2 = 0;
			int h2 = 0;
			for (int i =0; i < 5; i++) {
				switch(i) {
					case 0: {
						w2 = soX;
						h2 = soY;
						break;
					}
					case 1: {
						w2 = soX;
						h2 = soY-bs;
						break; 
					}
					case 2: {
						w2 = soX;
						h2 = soY+bs;
						break; 
					}
					case 3: {
						w2 = soX-bs;
						h2 = soY;
						break; 
					}
					case 4: {
						w2 = soX+bs;
						h2 = soY;
						break; 
					}
				}
				
				// out of bound, ignore
				if (w2 < 0 || w2+bs >= width_buffer || h2 < 0 || h2+bs >= height_buffer)
					continue;
				
				sad = calculateSAD(currFrame, interpolatedBuffer, w1, h1, soF, w2, h2, bs);
				
				if (sad < minSAD) {
					minSAD = sad;
					minf = soF;
					minx = w2;
					miny = h2;
				}
			}
			
			// Finished search the cross, now make decision if we need to continue
			if ((minx == soX && miny == soY) || (minx == 2*bs*w1 && miny == 2*bs*h1)) {
				done = true;
			}
			
			soX = minx;
			soY = miny;
		}
		
		// Store mv and return
		int[] fxy = new int[3];
		fxy[0] = minf;
		fxy[1] = (minx - 2*bs*w1)/bs;
		fxy[2] = (miny - 2*bs*h1)/bs; // fxy contains mv of current block no matter what the block size is
		return fxy;
	}
	
	/**
	 * Can be called same as generateMVbyBlock. The difference is that this function uses fast motion estimation
	 * and nearest neighbour search
	 * @param currFrame
	 * @param reference
	 * @param h1
	 * @param w1
	 * @param bs
	 * @param prevMV
	 * @return
	 */
	public int[] generateMVbyBlockFast(byte[] currFrame, byte[][] reference, int h1, int w1, int bs, int[] prevMV) {
		int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, minf = Integer.MAX_VALUE;
		boolean done = false;
		
		// Search origin variables
		int soX = w1 + prevMV[1], soY = h1 + prevMV[2], soF = prevMV[0];
		
		// Step 1. Search origin (aka block at w1 and h1) on all reference frames
		for (int f = 0; f < numRef; f++) {
			int sad = calculateSAD(currFrame, reference, w1, h1, f, w1, h1, bs);
			if (sad < minSAD) {
				minSAD = sad;
				minf = f;
				minx = w1;
				miny = h1;
			}
			else if (sad == minSAD) {
				minf = Math.min(f, minf); // Pick the smaller f number if we have a tie
				minx = w1;
				miny = h1;
				minSAD = sad;
			}
		}
		//System.out.println("Min SAD of all origin blocks : " + minSAD);
		
		// Step 2. Search by setting origin to location pointed to by prevMV
		while (!done) {
			
			/*
			 * 0 -> self
			 * 1 -> vertical up neighbour
			 * 2 -> vertical down neighbour
			 * 3 -> left neighbour
			 * 4 -> right neighbour
			*/
			int sad = Integer.MAX_VALUE;
			int w2 = 0;
			int h2 = 0;
			for (int i =0; i < 5; i++) {
				switch(i) {
					case 0: {
						w2 = soX;
						h2 = soY;
						break;
					}
					case 1: {
						w2 = soX;
						h2 = soY-1;
						break; 
					}
					case 2: {
						w2 = soX;
						h2 = soY+1;
						break; 
					}
					case 3: {
						w2 = soX-1;
						h2 = soY;
						break; 
					}
					case 4: {
						w2 = soX+1;
						h2 = soY;
						break; 
					}
				}
				
				// out of bound, ignore
				if (w2 < 0 || w2+bs >= width || h2 < 0 || h2+bs >= height)
					continue;
				
				sad = calculateSAD(currFrame, reference, w1, h1, soF, w2, h2, bs);
				
				if (sad < minSAD) {
					minSAD = sad;
					minf = soF;
					minx = w2;
					miny = h2;
				}
			}
			
			//System.out.println("minx , miny " + minx + "," + miny);
			// Finished search the cross, now make decision if we need to continue
			if ((minx == soX && miny == soY) || (minx == w1 && miny == h1)) {
				done = true;
			}
			
			soX = minx;
			soY = miny;
		}
		
		// Store mv and return
		int[] fxy = new int[3];
		fxy[0] = minf;
		fxy[1] = minx - w1;
		fxy[2] = miny - h1; // fxy contains mv of current block no matter what the block size is
		return fxy;
	}
	
	public int[] generateMVbyBlockFraction(byte[] currFrame, byte[][] reference, int h1, int w1, int bs, int[] prevMV, ArrayList<byte[][]> buffers) {
		byte[][] interpolatedBuffer = new byte[0][0];
		int numbBlcok_width_expanded = 0; 
        int numbBlcok_height_expanded = 0;
		for (int h = 0; h < height; h++) if (h + bs <= height) numbBlcok_height_expanded++;
        for (int w = 0; w < width; w++) if (w + bs <= width) numbBlcok_width_expanded++;
		int numbBlcok_width_buffer = numbBlcok_width_expanded * 2 - 1; 
        int numbBlcok_height_buffer = numbBlcok_height_expanded * 2 - 1;
		int width_buffer = bs * numbBlcok_width_buffer; 
        int height_buffer = bs * numbBlcok_height_buffer;
		int scale = 2*bs;
		
		// finds best matching block from interpolated buffer where interpolated blocks and reference blocks exist (See A2 Page 2 Part C Example)
		int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, minf = Integer.MAX_VALUE;
		for (int f = 0; f < numRef; f++) { // check SAD value with each reference frame
			interpolatedBuffer = buffers.get(f); // gets interpolated buffer corresponding to frame f
			// block operation occurs on the buffer to find best matching block
			for (int h2 = scale*(h1 - rangeR); h2 <= scale*(h1 + rangeR); h2+=bs) { // check SAD value with each block in the buffer
				for (int w2 = scale*(w1 - rangeR); w2 <= scale*(w1 + rangeR); w2+=bs) { // h2 and w2 are scaled to be on the buffer
					// ignore blocks falling partially or totally outside the buffer
					if (w2 >= 0 && w2 + bs <= width_buffer && h2 >= 0 && h2 + bs <= height_buffer) {
						int  sad = calculateSAD(currFrame, interpolatedBuffer, w1, h1, f, w2, h2, bs); // passes interpolated buffer as "reference"
						if (sad < minSAD) {
							minSAD = sad;
							minf = f;
							minx = (w2 - scale*w1)/bs; // scales w1 (to be on the buffer) -> finds mv -> scales mv back: the result mv is eventually scaled by 2 in both directions
							miny = (h2 - scale*h1)/bs;																								// so all mvs can be in integers
						}
						else if (sad == minSAD) {
							int tempx = (w2 - scale*w1)/bs;
							int tempy = (h2 - scale*h1)/bs;
							if ((Math.abs(tempx) + Math.abs(tempy) < Math.abs(minx) + Math.abs(miny))
							|| (Math.abs(tempx) + Math.abs(tempy) == Math.abs(minx) + Math.abs(miny)
							 	&& Math.abs(tempy) < Math.abs(miny))
							|| (Math.abs(tempx) + Math.abs(tempy) == Math.abs(minx) + Math.abs(miny)
							 	&& Math.abs(tempy) == Math.abs(miny) && Math.abs(tempx) < Math.abs(minx))) {
								minf = f;
								minx = tempx;
								miny = tempy;
							}
						}
					}
				}
			}
		}
		int[] fxy = new int[3];
		fxy[0] = minf;
		fxy[1] = minx;
		fxy[2] = miny; 
		return fxy;
	}
	
	public int[] generateMVbyBlock(byte[] currFrame, byte[][] reference, int h1, int w1, int bs, int[] prevMV) {
		int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, minf = Integer.MAX_VALUE;
		for (int f = 0; f < numRef; f++) { // check SAD value with each reference frame
			for (int h2 = h1 - rangeR; h2 <= h1 + rangeR; h2++) { // check SAD value with each reference block
				for (int w2 = w1 - rangeR; w2 <= w1 + rangeR; w2++) {
					// ignore blocks falling partially or totally outside the previous frame
					if (w2 >= 0 && w2 + bs <= width && h2 >= 0 && h2 + bs <= height) {
						int sad = calculateSAD(currFrame, reference, w1, h1, f, w2, h2, bs);
						if (sad < minSAD) {
							minSAD = sad;
							minf = f;
							minx = w2 - w1;
							miny = h2 - h1;
						}
						else if (sad == minSAD) {
							if ((Math.abs(w2 - w1) + Math.abs(h2 - h1) < Math.abs(minx) + Math.abs(miny))
							|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
							 	&& Math.abs(h2 - h1) < Math.abs(miny))
							|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
							 	&& Math.abs(h2 - h1) == Math.abs(miny) && Math.abs(w2 - w1) < Math.abs(minx))) {
								minf = f;
								minx = w2 - w1;
								miny = h2 - h1;
							}
						}
					}
				}
			}
		}
		int[] fxy = new int[3];
		fxy[0] = minf;
		fxy[1] = minx;
		fxy[2] = miny; 
		return fxy;
	}

	/**
	 * calculate SAD between current block and 
	 * @param w1
	 * @param h1
	 * @param f
	 * @param w2
	 * @param h2
	 * @param bs
	 * @return
	 */
	public int calculateSAD(byte[] currFrame, byte[][] reference, int w1, int h1, int f, int w2, int h2, int bs) {
		int sad = 0;
		for (int k = 0; k < bs; k++) {
			int wOfCur = w1 + k;
			int wOfRef = w2 + k;
			for (int j = 0; j < bs; j++) {
				int hOfCur = h1 + j;
				int hOfRef = h2 + j;
				if (FMEenabled) sad += Math.abs(Helper.unsignedByteToInt(currFrame[hOfCur * width + wOfCur])
											  - Helper.unsignedByteToInt(reference[hOfRef][wOfRef])); //when FME is enabled, 2d interpolation buffer is passed as "reference"
				else sad += Math.abs(Helper.unsignedByteToInt(currFrame[hOfCur * width + wOfCur])
							       - Helper.unsignedByteToInt(reference[f][hOfRef * width + wOfRef]));
			}
		}
		return sad;
	}
}

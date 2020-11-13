package Assignment2;

public class VisualizeChangePartB {
	
	/**
	 * Modify frame data in place.
	 * @param frame
	 * @param row
	 * @param col
	 * @param vbsFlag
	 */
	public static void darkenBorder(byte[] frame, int row, int col, int bs, int vidWidth, int dataOffset, boolean vbsFlag) {
		System.out.println(Integer.toString(row) + " : "+ Integer.toString(col));
		for (int y = row; y < row+bs; y++) {
			for (int x = col; x < col+bs; x++) { 
				
				if (x ==col || x==col+bs-1 || y==row || y==row+bs-1) {
					frame[dataOffset + y*vidWidth+x] = 0;
				}
				
				if (vbsFlag) {
					// also darken middle lines if vbs flag
					if (y==row+(bs/2) || x ==col+(bs/2)) {
						frame[dataOffset + y*vidWidth+x] = 0;
					}
					
				}
			}
		}
		
	}
	
	
	public static void main(String[] args) {
		//TODO: Add code to generate image.
		
		// 2 files I care about:
		PropertyHelper ph = new PropertyHelper();
		String inputYUV = ph.getProperty("encReconstructedFileName");
		String inputVBS = ph.getProperty("vbsFileName");
		
		// Other Params I need
		int frameCnt = Integer.parseInt(ph.getProperty("frameCount"));
		int vidWidth = Integer.parseInt(ph.getProperty("vidWidth"));
		int vidHeight = Integer.parseInt(ph.getProperty("vidHeight"));
		int blkSize = Integer.parseInt(ph.getProperty("blkSizeI"));
		
		byte[] yuvBytes = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + inputYUV);
		byte[] vbsBytes = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + inputVBS);
		
		if (vbsBytes.length != (frameCnt * vidWidth * vidHeight / blkSize / blkSize)) {
			System.out.println("Incorrect number of vbs bytes");
			System.exit(1);
		}
		
		
		int blkInd = 0;
		
		// per frame
		for (int frame = 0; frame < frameCnt; frame++) {
			// per block, add lines around the border
			int frameDataOffset = frame*vidHeight*vidWidth;
			
					
			for (int row = 0; row < vidHeight ; row+=blkSize) {
				for (int col = 0; col < vidWidth; col+= blkSize) { 
//					System.out.println(col);
					if (vbsBytes[blkInd] == 1) {
						VisualizeChangePartB.darkenBorder(yuvBytes, row, col, blkSize, vidWidth, frameDataOffset, true);
					}
					else {
						VisualizeChangePartB.darkenBorder(yuvBytes, row, col, blkSize, vidWidth, frameDataOffset , false);
					}
					blkInd ++;	
				}
				
			}
			
		}
		
		
		FileHelper.writeToFile(yuvBytes, "visualizepartb.yuv");
	} 

}

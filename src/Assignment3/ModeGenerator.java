package Assignment3;

public class ModeGenerator {

	int width, height, blockSize;
	
	public ModeGenerator() {
		PropertyHelper ph = new PropertyHelper();
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
	}
	
	//in reference, all previous blocks are reconstructed while all blocks after contain value 0;
	public int generateModeByBlock(byte[] currFrame, byte[] reference, int h1, int w1, int bs, int prevMode) {
        int horizontalSAD = 0, verticalSAD = 0, index = 0, mode;
        byte[] horizontalPredictor = new byte[bs * bs];
        byte[] verticalPredictor = new byte[bs * bs];
        for (int h = h1; h < h1 + bs; h++) {
            for (int w = w1; w < w1 + bs; w++) {
                // Horizontal Intra
                if (w1 == 0) // uses 128 for absent samples at left frame border
                    horizontalPredictor[index] = (byte) 128;
                else // uses left border of the block
                    horizontalPredictor[index] = reference[h * width + w1 - 1];
                
                horizontalSAD += Math.abs(Helper.unsignedByteToInt(horizontalPredictor[index])
                                        - Helper.unsignedByteToInt(currFrame[h * width + w]));
                
                // Vertical Intra
                if (h1 == 0) // uses 128 for absent samples at top frame border
                    verticalPredictor[index] = (byte) 128;
                else    // uses top border of the block
                    verticalPredictor[index] = reference[(h1 - 1) * width + w];

                verticalSAD += Math.abs(Helper.unsignedByteToInt(verticalPredictor[index])
                                      - Helper.unsignedByteToInt(currFrame[h * width + w]));
                index++;
            }
        }
        // determines the mode by comparing SAD values found above
        if (verticalSAD < horizontalSAD) mode = 1;  // mode that gives lowest SAD cost is selected (1 -> vertical)
        else mode = 0;  // (0 -> horizontal)
        return mode;
	}
}

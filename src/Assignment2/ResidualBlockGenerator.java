package Assignment2;

import java.util.ArrayList;

public class ResidualBlockGenerator {
	
	int blockSize, width, height;
	boolean FMEenabled;
	public ResidualBlockGenerator() {
		PropertyHelper ph = new PropertyHelper();
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		FMEenabled = Boolean.parseBoolean(ph.getProperty("FMEEnable"));
	}

	
	public short[] generatePframeResidualBlock(byte[] currFrame, byte[][] reference, int bs, 
			short[] residualFrame, int[] motionVector, int rowCoordinate, int colCoordinate, ArrayList<byte[][]> buffers) {
		
		for (int i = rowCoordinate; i < rowCoordinate + bs; i++){
            for (int j = colCoordinate; j < colCoordinate + bs; j++) {
                int curFrameBlkR = i; // Row coordinate of current frame block's top left pixel
                int curFrameBlkC = j; // Col coordinate of current frame block's top left pixel
                int predictedFrameIndex = motionVector[0];

				if (!FMEenabled) { // FME is not enabled
					int predictedFrameBlkR = curFrameBlkR + (int)motionVector[2]; // Assuming Y motion is movement in vertical row direction
					int predictedFrameBlkC = curFrameBlkC + (int)motionVector[1]; // Assuming X motion is movement in horizontal direction

					// Need to check if I'm going outside of the frame
					if (curFrameBlkR >= height || curFrameBlkR < 0 ||
						curFrameBlkC >= width || curFrameBlkC < 0 ||
						predictedFrameBlkR >= height || predictedFrameBlkR < 0 ||
						predictedFrameBlkC >= width || predictedFrameBlkC < 0) {
						System.out.println(curFrameBlkR);
						System.out.println(curFrameBlkC);
						System.out.println(predictedFrameBlkR);
						System.out.println(predictedFrameBlkC);
						if (i == rowCoordinate & j == colCoordinate) {
							// For the first loop, predictedFrame is starting point of the ixi block,
							// this should never be out of bounds. if it is, then our motion vector is wrong
							// Special check on motion vector, if motion vector is taking me out of bounds then this should fail
							System.out.println("Motion Vector points to out of bounds frame. Failing program.");
							System.exit(1);
						}
						// Outside of frame.
						continue;
					}
					int tempRes = (Helper.unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - Helper.unsignedByteToInt(reference[predictedFrameIndex][predictedFrameBlkR * width + predictedFrameBlkC]));
					residualFrame[i * width + j] = (short)tempRes;
				}
				else { // FME is  enabled
					byte[][] interpolatedBuffer = buffers.get(predictedFrameIndex);
					int mv_x = motionVector[1];
                    int mv_y = motionVector[2];
                    int tempRes;
                    if (mv_x % 2 == 0 && mv_y % 2 == 0) { // best mathicng block is reference blcok so MC works as before
                        int predictedFrameBlkR = curFrameBlkR + mv_y/2; 
                        int predictedFrameBlkC = curFrameBlkC + mv_x/2;
                        tempRes = (Helper.unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - Helper.unsignedByteToInt(reference[predictedFrameIndex][predictedFrameBlkR * width + predictedFrameBlkC]));
                    }
                    else { // best matching block is interpolated block
                        int predictedFrameBlkR = (bs*2*rowCoordinate + bs* mv_y) + (i-rowCoordinate); // scales current coordinates and mvs to be on the interpolated buffer to find corresponding interpolated block
                        int predictedFrameBlkC = (bs*2*colCoordinate + bs* mv_x) + (j-colCoordinate);
                        tempRes = (Helper.unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - Helper.unsignedByteToInt(interpolatedBuffer[predictedFrameBlkR][predictedFrameBlkC]));
                    }
					residualFrame[i * width + j] = (short)tempRes;
				}
            }
        }
		return residualFrame;
	}
	
	public short[] generateIframeResidualBlock(byte[] currFrame, byte[] reference, short[] residualFrame, 
			int mode, int rowCoordinate, int colCoordinate, int bs) { 
		
        int index = 0;
        byte[] horizontalPredictor = new byte[bs * bs];
        byte[] verticalPredictor = new byte[bs * bs];
        for (int h = rowCoordinate; h < rowCoordinate + bs; h++) {
            for (int w = colCoordinate; w < colCoordinate + bs; w++) {
                // Horizontal Intra
                if (colCoordinate == 0) // uses 128 for absent samples at left frame border
                    horizontalPredictor[index] = (byte) 128;
                else // uses left border of the block
                    horizontalPredictor[index] = reference[h * width + colCoordinate - 1];
                // Vertical Intra
                if (rowCoordinate == 0) // uses 128 for absent samples at top frame border
                    verticalPredictor[index] = (byte) 128;
                else    // uses top border of the block
                    verticalPredictor[index] = reference[(rowCoordinate - 1) * width + w];
                index++;
            }
        }
        index = 0;
        for (int h = rowCoordinate; h < rowCoordinate + bs; h++) {
            for (int w = colCoordinate; w < colCoordinate + bs; w++) {
                if (mode == 0)
    				residualFrame[h * width + w] = (short) (Helper.unsignedByteToInt(currFrame[h * width + w])
    						- Helper.unsignedByteToInt(horizontalPredictor[index]));
				else
					residualFrame[h * width + w] = (short) (Helper.unsignedByteToInt(currFrame[h * width + w])
							- Helper.unsignedByteToInt(verticalPredictor[index]));
                index++;
            }
        }
        return residualFrame;
	}
}

package Assignment2;

import java.util.ArrayList;

public class ReconstructedBlockGenerator {

	int width, height;
	boolean FMEenabled;
	public ReconstructedBlockGenerator() {
		PropertyHelper ph = new PropertyHelper();
		int blocksize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blocksize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blocksize);
		FMEenabled = Boolean.parseBoolean(ph.getProperty("FMEEnable"));
	}
	
	public byte[] reconstructedPframeBlock(byte[] reconstructedFrame, short[] invTransResidual, byte[][] reference,
			int col, int row, int bs, int[] mv, ArrayList<byte[][]> buffers) {
		int frameIndex = mv[0];
		int motion_x = mv[1];
		int motion_y = mv[2];
		if (!FMEenabled) { //FME is NOT enabled
			for (int y = row; y < row + bs; y++) {
				for (int x = col; x < col + bs; x++) {
					int predictor_ind = x + motion_x + ((motion_y + y) * this.width);
					reconstructedFrame[y * width + x] = (byte)(Helper.unsignedByteToInt(reference[frameIndex][predictor_ind]) + (int)(invTransResidual[y * width + x]));
				}
			}
		}
		else { //FME is ENABLED
			byte[][] interpolatedBuffer = buffers.get(frameIndex);
			for (int y = row; y < row + bs; y++) {
				for (int x = col; x < col + bs; x++) {
					if (motion_x % 2 == 0 && motion_y % 2 == 0) { // best mathicng block is reference block so MC works as before
						int predictor_ind = x + motion_x/2 + ((motion_y/2 + y) * this.width);
						reconstructedFrame[y * width + x] = (byte)(Helper.unsignedByteToInt(reference[frameIndex][predictor_ind]) + (int)(invTransResidual[y * width + x]));			
					}
					else { // best matching block is interpolated block
						int predictor_ind_x = (bs*2*col + bs*motion_x) + (x - col); // scales current coordinates and mvs to be on the interpolated buffer to find corresponding interpolated block
						int predictor_ind_y = (bs*2*row + bs*motion_y) + (y - row);
						reconstructedFrame[y * width + x] = (byte)(Helper.unsignedByteToInt(interpolatedBuffer[predictor_ind_y][predictor_ind_x]) + (int)(invTransResidual[y * width + x]));			
					}
				}
			}
		}
		return reconstructedFrame;
	}
	
	public byte[] reconstructedIframeBlock(byte[] reconstructedFrame, short[] invTransResidual, byte[] reference,
			int col, int row, int bs, int mode) {
		int index = 0;
		byte[] intraPredictor = new byte[bs * bs];
        for (int h = row; h < row + bs; h++) {
            for (int w = col; w < col + bs; w++) {
            	if (mode == 0) {
	                // Horizontal Intra
	                if (col == 0) // uses 128 for absent samples at left frame border
	                    intraPredictor[index] = (byte) 128;
	                else
	                    intraPredictor[index] = reference[h * width + col - 1];
            	}
            	else {
	                // Horizontal Intra
	                if (row == 0) // uses 128 for absent samples at left frame border
	                    intraPredictor[index] = (byte) 128;
	                else
	                    intraPredictor[index] = reference[(row - 1) * width + w];
            	}
                reconstructedFrame[h * width + w] = (byte) ((int)(invTransResidual[h * width + w])
                		+ Helper.unsignedByteToInt(intraPredictor[index]));
                index++;
            }
        }
        return reconstructedFrame;  
	}
}

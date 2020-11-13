package Assignment1;

public class ResidualBlockGenerator {
	
	int blockSize;
	int width;
	int height;
	int n;
	
	public ResidualBlockGenerator(String yuvFileName, String mvFileName, int width, int height, int blockSize, int n) {
		
		this.blockSize = blockSize;
		this.width = width;
		this.height = height;
		this.n = n;
		String outFileName = "ResidualBlock.arb";
		
		// TODO: 300 should not be hardcoded. We should change ReadYOnly class to calculate frameLength internally
		// Instead of passing it in.
		ReadYOnly yOnlyReader = new ReadYOnly(yuvFileName, width, height, 300);
		
		// Get my motion vector file
		int [] motionVector = FileHelper.readMVFile(System.getProperty("user.dir") + "/output/" + mvFileName);
		
		int frameCnt = 0;
		while (frameCnt < yOnlyReader.frameLength) {
			// Work on 1 frame at a time. yOnlyReader is reading original YOnly File. Not the MV.
			yOnlyReader.readOneFrame();
			byte [] currFrame = yOnlyReader.currFrame;
			byte [] predictedFrame = yOnlyReader.prevFrame;
			byte [] residualFrame = new byte[currFrame.length];
			
			int blockNum = 0;
			// For each block, calculate the residual block, moving in raster order (line by line)
			for (int row=0; row < height; row+=this.blockSize) {
				for (int col=0; col < width; col+=this.blockSize) {
					// for part 3
					calcResidualBlock(currFrame, predictedFrame, residualFrame, motionVector, row, col, blockNum, this.blockSize, 128);
					// for part 4
					// calcResidualBlockBeforeApprox(currFrame, predictedFrame, residualFrame, motionVector, row, col, blockNum, this.blockSize, 128);
					blockNum+=1;
				}
			}
			
			// Write entire frame to file. This is essential part 3.f
			if (frameCnt == 0) {
				FileHelper.writeToFile(residualFrame, outFileName);
			}
			else {
				FileHelper.writeToFileAppend(residualFrame, outFileName);
			}
			frameCnt += 1;
		}
		return;
	}
	
	/**
	 * Calculate the residual block. Basically, I take the predicted frame and add the motion vector value to get the 
	 * predicted block. And then use the predicted block, current block, to calculate the residual block.
	 * And then finally write the value to residualFrame.
	 * @param currFrame, the frame we are trying to look at and compute now
	 * @param predictedFrame, the previous frame where our predicted blocks will be
	 * @param residualFrame, residual block's new values will be written to residualFrame
	 * @param motionVector, motion vectors
	 * @param rowCoordinate, Y component of top left pixel of block
	 * @param colCoordinate, X component of top left pixel of block 
	 * @param blockNum, block number so we know which motion vector value to use
	 * @param blockSize, (ixi) block, blockSize=i
	 * @param padding, default padding if indices fall out of bound
	 */
	public void calcResidualBlockBeforeApprox(byte[] currFrame, byte[] predictedFrame, byte[] residualFrame, 
			int[] motionVector, int rowCoordinate, int colCoordinate, int blockNum, int blockSize, int padding) {
		
		for (int i=rowCoordinate; i< rowCoordinate+blockSize; i++){
            for (int j=colCoordinate; j < colCoordinate+blockSize; j++) {
                int curFrameBlkR = i; // Row coordinate of current frame block's top left pixel
                int curFrameBlkC = j; // Col coordinate of current frame block's top left pixel
                int predictedFrameBlkR = curFrameBlkR + motionVector[blockNum*2 + 1]; // Assuming Y motion is movement in vertical row direction
                int predictedFrameBlkC = curFrameBlkC + motionVector[blockNum*2]; // Assuming X motion is movement in horizontal direction
                  
            	// Need to check if I'm going outside of the frame
            	if (curFrameBlkR >= this.height || curFrameBlkR < 0 || 
            		curFrameBlkC >= this.width || curFrameBlkC < 0 || 
            		predictedFrameBlkR >= this.height || predictedFrameBlkR < 0 ||
            		predictedFrameBlkC >= this.width || predictedFrameBlkC < 0) {
            		
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
            	if (predictedFrame != null) {
            		int tempRes = (ReadYOnly.unsignedByteToInt(currFrame[curFrameBlkR*this.width + curFrameBlkC]) - ReadYOnly.unsignedByteToInt(predictedFrame[predictedFrameBlkR*this.width + predictedFrameBlkC]));
            		residualFrame[i*this.width +j] = (byte) tempRes;
            	}
            	else {
            		int tempRes = (ReadYOnly.unsignedByteToInt(currFrame[curFrameBlkR*this.width + curFrameBlkC]) - padding);
            		residualFrame[i*this.width +j] = (byte) tempRes;
            	}
            	
            }
        }
		return;
	}
	
	public void calcResidualBlock(byte[] currFrame, byte[] predictedFrame, byte[] residualFrame, 
			int[] motionVector, int rowCoordinate, int colCoordinate, int blockNum, int blockSize, int padding) {
		calcResidualBlockBeforeApprox(currFrame, predictedFrame, residualFrame, 
				motionVector, rowCoordinate, colCoordinate, blockNum, blockSize, padding);
		for (int k = 0; k < residualFrame.length; k++) {
			residualFrame[k] = (byte) roundToNearestMultiple(residualFrame[k], this.n);
		}
	}
	
	/**
	 * Helper function to round to the nearest multiple of 2^n
	 * @param input, the number to be rounded
	 * @param n, 2^n
	 * @return
	 */
	public int roundToNearestMultiple(int input, int n) {
		
		double num = Math.pow(2, n);
		
		return  (int) (Math.round(input/num) * (int) num);
	}
	

}

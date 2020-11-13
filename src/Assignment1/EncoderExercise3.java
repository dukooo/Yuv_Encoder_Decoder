package Assignment1;

import java.util.ArrayList;

public class EncoderExercise3 {

    byte[] yOnly;// the original file after padding
    int width, height; // after padding
    int frameLength, blockSize, multipleN, rangeR;
    int frameIndex;
    byte[] currFrame;
    byte[] reference;
    String mvFileName;
    // for geenrating the graph
    String residualAfterMVFileName;
    String residualBeforeMVFileName;
    // for the decoder side
    String residualFileName;
    String reconstructedFileName;
    byte[] motionvector;


    public EncoderExercise3() {
        PropertyHelper ph = new PropertyHelper();
        frameLength = Integer.valueOf(ph.getProperty("frameCount"));
        blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
        width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
        height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
        yOnly = addPaddingToYOnly(Integer.valueOf(ph.getProperty("vidWidth")),
                                  Integer.valueOf(ph.getProperty("vidHeight")),
                                  FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName")));
        multipleN = Integer.valueOf(ph.getProperty("multipleN"));
        rangeR = Integer.valueOf(ph.getProperty("rangeR"));
        mvFileName = ph.getProperty("mvFileName");
        residualAfterMVFileName = ph.getProperty("residualAfterMVFileName");
        residualBeforeMVFileName = ph.getProperty("residualBeforeMVFileName");
        residualFileName = ph.getProperty("residualFileName");
        reconstructedFileName = ph.getProperty("encReconstructedFileName");

        setReference();
        currFrame = new byte[width * height];
    }



    public byte[] addPaddingToYOnly(int origWidth, int origHeight, byte[] yOnly) {
        byte[] newYOnly = new byte[width * height * frameLength];
        for (int i = 0; i < frameLength; i++) {
            for (int y = 0; y < height; y++) {
                if (y < origHeight) {
                    for ( int x = 0; x < width; x++) {
                        if (x < origWidth)
                            newYOnly[i * width * height + y * width + x] = yOnly[i * origWidth * origHeight + y * origWidth + x];
                        else
                            newYOnly[i * width * height + y * width + x] = (byte)128;
                    }
                }
                else {
                    for ( int x = 0; x < width; x++)
                        newYOnly[i * width * height + y * width + x] = (byte)128;
                }
            }
        }
        return newYOnly;
    }

    public void setReference() {
        reference = new byte[width * height];
        for (int i = 0; i < width * height; i++) reference[i] = (byte) 128;
    }

    public static int unsignedByteToInt(byte b) { return (int) b & 0xFF; }

    //-------------------------------------------------------------------------
    //----------------------------MOTION ESTIMATION----------------------------
    //-------------------------------------------------------------------------

    // return an arraylist containing the motion vector of the currFrame
    public byte[] generateMV() {
        ArrayList<int[]> mv = new ArrayList<int[]>();
        for (int h1 = 0; h1 < height; h1 += blockSize) { // for each block
            for (int w1 = 0; w1 < width; w1 += blockSize) {
                int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
                for (int h2 = h1 - rangeR; h2 <= h1 + rangeR; h2++) { // check SAD value with each reference block
                    for (int w2 = w1 - rangeR; w2 <= w1 + rangeR; w2++) {
                        // ignore blocks falling partially or totally outside the previous frame
                        if (w2 >= 0 && w2 + blockSize <= width && h2 >= 0 && h2 + blockSize <= height) {
                            int sad = calculateSAD(w1, h1, w2, h2);
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
                //for test
                /*
                if (minx != 0 || miny != 0)
                    System.out.println("frame: " + frameIndex + ", block(" + w1 + ", " + h1 + "): minx = " + minx + ", miny = " + miny);
                */
                mv.add(xy);
            }
        }
        byte[] mvBytes = writeToMVFile(mv);
        return mvBytes;
    }

    // calculate Sum of Absolute Difference (SAD) between two blocks
    public int calculateSAD(int w1, int h1, int w2, int h2) {
        int sad = 0;
        for (int k = 0; k < blockSize; k++) {
            int wOfCur = w1 + k;
            int wOfRef = w2 + k;
            for (int j = 0; j < blockSize; j++) {
                int hOfCur = h1 + j;
                int hOfRef = h2 + j;
                sad += Math.abs(unsignedByteToInt(currFrame[hOfCur * width + wOfCur])
                        - unsignedByteToInt(reference[hOfRef * width + wOfRef]));
            }
        }
        return sad;
    }

    // generate mv file, return byte[] for further use
    public byte[] writeToMVFile(ArrayList<int[]> mv) {
        byte[] mvBytes = new byte[width * height / blockSize / blockSize * 2];
        int writeCount = 0;
        for (int[] item : mv) {
            mvBytes[writeCount] = (byte) item[0];
            writeCount++;
            mvBytes[writeCount] = (byte) item[1];
            writeCount++;
        }
        if (frameIndex == 0) FileHelper.writeToFile(mvBytes, mvFileName);
        else FileHelper.writeToFileAppend(mvBytes, mvFileName);
        return mvBytes;
    }


    //--------------------------------------------------------------------------------
    //----------------------------RESIDUAL BLOCK GENERATER----------------------------
    //--------------------------------------------------------------------------------

    // return the one after motion compensation for further use
    public byte[] generateResidualBlock() {
        byte[] residualFrameAfterMV = new byte[currFrame.length];
        byte[] residualFrameBeforeMV = new byte[currFrame.length];
        this.motionvector = generateMV();
        int blockNum = 0;
        for (int row = 0; row < height; row+=blockSize) {
            for (int col = 0; col < width; col+=blockSize) {
                residualFrameAfterMV = calcResidualBlockAfterMV(residualFrameAfterMV, this.motionvector, row, col, blockNum);
                residualFrameBeforeMV = calcResidualBlockBeforeMV(residualFrameBeforeMV, row, col, blockNum);
                blockNum++;
            }
        }
        writeToResidualFile(residualFrameAfterMV, residualFrameBeforeMV);
        return residualFrameAfterMV;
    }

    /**
     * Calculate the residual block after motion compensation
     * @param rowCoordinate, Y component of top left pixel of block
     * @param colCoordinate, X component of top left pixel of block
     * @param blockNum, block index so we know which motion vector value to use
     */
    public byte[] calcResidualBlockAfterMV(byte[] residualFrame, byte[] motionVector, int rowCoordinate, int colCoordinate, int blockNum) {

        for (int i = rowCoordinate; i < rowCoordinate+blockSize; i++){
            for (int j = colCoordinate; j < colCoordinate+blockSize; j++) {
                int curFrameBlkR = i; // Row coordinate of current frame block's top left pixel
                int curFrameBlkC = j; // Col coordinate of current frame block's top left pixel
                int predictedFrameBlkR = curFrameBlkR + (int)motionVector[blockNum * 2 + 1]; // Assuming Y motion is movement in vertical row direction
                int predictedFrameBlkC = curFrameBlkC + (int)motionVector[blockNum * 2]; // Assuming X motion is movement in horizontal direction

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
                int tempRes = (unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - unsignedByteToInt(reference[predictedFrameBlkR * width + predictedFrameBlkC]));
                residualFrame[i * width + j] = (byte) roundToNearestMultiple(tempRes, multipleN);
            }
        }
        return residualFrame;
    }

    // Calculate the residual block before motion compensation
    public byte[] calcResidualBlockBeforeMV(byte[] residualFrame, int rowCoordinate, int colCoordinate, int blockNum) {

        for (int i = rowCoordinate; i < rowCoordinate+blockSize; i++){
            for (int j = colCoordinate; j < colCoordinate+blockSize; j++) {
                int curFrameBlkR = i; // Row coordinate of current frame block's top left pixel
                int curFrameBlkC = j; // Col coordinate of current frame block's top left pixel
                int predictedFrameBlkR = curFrameBlkR;
                int predictedFrameBlkC = curFrameBlkC;
                int tempRes = (unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - unsignedByteToInt(reference[predictedFrameBlkR * width + predictedFrameBlkC]));
                residualFrame[i * width + j] = (byte) roundToNearestMultiple(tempRes, multipleN);
            }
        }
        return residualFrame;
    }

    // Helper function to round to the nearest multiple of 2^n
    public int roundToNearestMultiple(int input, int n) {
        double num = Math.pow(2, n);
        return  (int) (Math.round(input/num) * (int) num);
    }

    // write to both residualBlockAfterMV3.yuv and residualBlockBeforeMV3.yuv
    public void writeToResidualFile(byte[] residualFrameAfterMV, byte[] residualFrameBeforeMV) {
        byte[] absResidualFrameAfterMV = new byte[residualFrameAfterMV.length];
        byte[] absResidualFrameBeforeMV = new byte[residualFrameBeforeMV.length];
        for (int i = 0; i < residualFrameAfterMV.length; i++) {
            absResidualFrameAfterMV[i] = (byte)Math.abs((int)residualFrameAfterMV[i]);
            absResidualFrameBeforeMV[i] = (byte)Math.abs((int)residualFrameBeforeMV[i]);
        }
        if (frameIndex == 0) {
            FileHelper.writeToFile(residualFrameAfterMV, residualFileName);
            FileHelper.writeToFile(absResidualFrameAfterMV, residualAfterMVFileName);
            FileHelper.writeToFile(absResidualFrameBeforeMV, residualBeforeMVFileName);
        }
        else {
            FileHelper.writeToFileAppend(residualFrameAfterMV, residualFileName);
            FileHelper.writeToFileAppend(absResidualFrameAfterMV, residualAfterMVFileName);
            FileHelper.writeToFileAppend(absResidualFrameBeforeMV, residualBeforeMVFileName);
        }
    }

    //-------------------------------------------------------------------------------------
    //----------------------------RECONSTRUCTED FRAME GENERATOR----------------------------
    //-------------------------------------------------------------------------------------

    // Helper function to turn pixel index i to blocknum
    public int getBlockNumForI(int pixel_i) {

        // first get row and col of pixel
        int row = pixel_i/this.width;
        int col = pixel_i % this.width;

        int block_row = row/this.blockSize;
        int block_col = col/this.blockSize;

        int block_num = block_row*(this.width/this.blockSize) + block_col;
        return block_num;

    }

    // generate the reconstructed frame
    public byte[] generateReconstructedFrame() {
        int oneFrameSize = width * height;
        byte[] residual = generateResidualBlock();
        byte[] reconstructedFrame = new byte[currFrame.length];
        byte[] predictorFrame = new byte[currFrame.length];
        for (int i = 0; i < oneFrameSize; i++) {
            int blockNum = this.getBlockNumForI(i);
            int motion_x = (int)(this.motionvector[2*blockNum]);
            int motion_y = (int)(this.motionvector[2*blockNum +1]);
            int predictor_ind = i + motion_x + (motion_y * this.width);
            reconstructedFrame[i] = (byte)(unsignedByteToInt(reference[predictor_ind]) + (int)(residual[i]));
            predictorFrame[i] = reference[predictor_ind];
        }
        writeToReconstructedFile(reconstructedFrame);
        writeToPredictorFile(predictorFrame);

        return reconstructedFrame;
    }

    // write to reconstructedFile.yuv
    public void writeToReconstructedFile(byte[] reconstructedFrame) {
        if (frameIndex == 0)
            FileHelper.writeToFile(reconstructedFrame, reconstructedFileName);
        else
            FileHelper.writeToFileAppend(reconstructedFrame, reconstructedFileName);
    }

    public void writeToPredictorFile(byte[] predictorFrame) {
    	if (frameIndex == 0)
            FileHelper.writeToFile(predictorFrame, "predictorFrames.yuv");
        else
            FileHelper.writeToFileAppend(predictorFrame, "predictorFrames.yuv");

    }

    //-------------------------------------------------------------------------
    //----------------------------ENCODER GENERATOR----------------------------
    //-------------------------------------------------------------------------

    // looping through all frames and do encoding
    public void startEncoding() {
        for (int i = 0; i < frameLength; i++) {
            frameIndex = i;
            setCurr();
            byte[] reconstructed = generateReconstructedFrame();
            reference = reconstructed;
        }
    }

    // put the current frame y only value into currFrame
    public void setCurr() {
        for (int i = 0; i < width * height; i++) {
            currFrame[i] = yOnly[frameIndex * width * height + i];
        }
    }

    public static void main(String[] args) {
        EncoderExercise3 en = new EncoderExercise3();
        en.startEncoding();
    }


}

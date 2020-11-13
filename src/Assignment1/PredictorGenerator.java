package Assignment1;

public class PredictorGenerator {

    int refFrameWidth, refFrameHeight, frameIndex, frameLength, iVal;
    byte[] motionVector;

    /**
     * apply motion vector at given reference frame to generate motion compensated predictor frame
     * @param mvFileName file name of the motion vector
     * @param width width of the Y only 4:2:0 yuv file
     * @param height height of the Y only 4:2:0 yuv file
     * @param frameLength number of frames
     * @param i size of the i x i block
     */
    public PredictorGenerator(String mvFileName, int width, int height, int frameLength, int i) {
        this.frameLength = frameLength;
        this.iVal = i;
        if (height % i != 0) this.refFrameWidth = (int)(width / i + 1) * i; // padding is added to the width
        else this.refFrameWidth = width;
        if (height % i != 0) this.refFrameHeight = (int)(height / i + 1) * i;// padding is added to the height
        else this.refFrameHeight = height;

        this.motionVector = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + mvFileName);
    }

    /**
     * 
     * @param refFrame reference frame where motion vectors will be applied to generate predictor frame
     * @param frameIndex used to retrieve motion vector at this given frame 
     * @return motion compensated predictor at given frame
     */
    public byte[] generatePredictor(byte[] refFrame, int frameIndex) {
        int refFrameSize = refFrameHeight*refFrameWidth;
        int numbBlocks_perRow = refFrameWidth/iVal;
        int numbBlocks_perFrame = numbBlocks_perRow * refFrameHeight/iVal;

        byte[] motionCompensatedPredictor = new byte[refFrameSize]; //motion compensated reference frame

        // For each reference frame, apply the motion vector and create motion compensated frame
        for (int h = 0; h < refFrameHeight; h++) {
            for (int w = 0; w < refFrameWidth; w++) {
                int currentElementIndex = h*refFrameWidth + w;

                int vectorIndex = 2 * (frameIndex*numbBlocks_perFrame + (h/iVal*numbBlocks_perRow + w/iVal)); //motion vector format: [x1,y1,x2,y2,x3,y3...] for raster ordered blocks
                int xVector = motionVector[vectorIndex];
                int yVector = motionVector[vectorIndex + 1];

                int foundElementIndex = (h+yVector)*refFrameWidth + (w+xVector);
                if (h+yVector < 0 || h+yVector >= refFrameHeight|| w+xVector < 0 || w+xVector >= refFrameWidth) { //mv goes out of the scope
                    // continue; //or fill in
                    motionCompensatedPredictor[frameIndex*refFrameSize + currentElementIndex] = refFrame[currentElementIndex];
                }
                else motionCompensatedPredictor[currentElementIndex] = refFrame[foundElementIndex];
                // else motionCompensatedPredictor[frameIndex*refFrameSize + currentElementIndex] = (byte) refFrame[foundElementIndex];
            }
        }
        // example:
        // 1  2  3  4                                                               1  2  3  4
        // 5  6  7  8   ==> iVal = 2, movement vector [0,0, 0,0, 0,0, -1, -1] ==>   5  6  7  8
        // 9  10 11 12                                                              9  10 6  7
        // 13 14 15 16                                                              13 14 10 11

        return motionCompensatedPredictor;
    }

    public static void main(String[] args) {
        
    }
}
package Assignment1;

import java.util.ArrayList;

public class DecoderExercise3 {

    int width, height; // after padding
    int frameLength, blockSize, multipleN, rangeR;
    int frameIndex;
    byte[] currFrame;
    byte[] predictor;
    String mvFileName;
    // for geenrating the graph
    String residualAfterMVFileName;
    String residualBeforeMVFileName;
    // for the decoder side
    String residualFileName;
    String reconstructedFileName;
    byte[] motionvector;


    public DecoderExercise3() {
        PropertyHelper ph = new PropertyHelper();
        frameLength = Integer.valueOf(ph.getProperty("frameCount"));
        blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
        width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
        height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
        mvFileName = ph.getProperty("mvFileName");
        residualFileName = ph.getProperty("residualFileName");
        reconstructedFileName = ph.getProperty("decReconstructedFileName");

    }
    public void startDecoding() {

        ReconstructedFile decoder = new ReconstructedFile(mvFileName, residualFileName, width, height, frameLength, blockSize);
        decoder.buildReconstructedFile(reconstructedFileName);
    }

    public static void main(String[] args) {
        DecoderExercise3 dec = new DecoderExercise3();
        dec.startDecoding();
    }
}

package Assignment1;

public class ReconstructedFile {

    int refFrameWidth, refFrameHeight, frameLength, iVal;
    byte[] residualFile;
    PredictorGenerator predictorBlockGenerator;

    public ReconstructedFile(String mvFileName, String residualFileName, int width, int height, int frameLength, int i) {
        this.frameLength = frameLength;
        this.iVal = i;
        if (height % i != 0) this.refFrameWidth = (int)(width / i + 1) * i; // padding is added to the width
        else this.refFrameWidth = width;
        if (height % i != 0) this.refFrameHeight = (int)(height / i + 1) * i;// padding is added to the height
        else this.refFrameHeight = height;

        this.residualFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + residualFileName);
        this.predictorBlockGenerator = new PredictorGenerator(mvFileName, width, height, frameLength, i);
    }

    public byte[] getReconstructedFile() {
        int oneFrameSize = refFrameHeight * refFrameWidth;
        byte[] refFrame = new byte[oneFrameSize];
        byte[] residual = new byte[oneFrameSize];
        byte[] reconstructed = new byte[oneFrameSize];
        byte[] reconstructedFile = new byte[oneFrameSize*frameLength];
        int index = 0;

        for (int frameIndex = 0; frameIndex < frameLength; frameIndex++) {
            if (frameIndex == 0) 
                for (int i = 0; i < oneFrameSize; i ++) refFrame[i] = (byte) 128; //first reference frame is filled with 128
            else 
                for (int i = 0; i < oneFrameSize; i ++) refFrame[i] = reconstructed[i]; //use reconstructed frame as ref frame

            byte[] predictor = predictorBlockGenerator.generatePredictor(refFrame, frameIndex); //motion compensated refFrame (predictor)
            for (int j = 0; j < oneFrameSize; j++) {
                residual[j] = residualFile[frameIndex*oneFrameSize + j];
                reconstructed[j] = (byte)(((int)predictor[j]&0xff) + (int)residual[j]); //extract residual values for current frame
                reconstructedFile[index++] = reconstructed[j];
            }
        }

        return reconstructedFile;
    }

    public void buildReconstructedFile(String fileName) {
        byte[] reconstructedFile = getReconstructedFile();
        FileHelper.writeToFile(reconstructedFile, fileName);
    }


    public static void main(String[] args) {
        
        //Testing
        int width = 352, height = 288, frameLength = 300, blockSize = 8, n = 1, r = 1;
        String fileName = "akiyo_cif_y_only.yuv";
        String mvFileName = "motionVector3.mv";
        String residualFileName = "residualBlock.arb";

        EncoderExercise3 en = new EncoderExercise3();
        en.startEncoding();
        
        ReconstructedFile test = new ReconstructedFile(mvFileName, residualFileName, width, height, frameLength, blockSize);
        test.buildReconstructedFile("decoded_"+fileName);
    }
}
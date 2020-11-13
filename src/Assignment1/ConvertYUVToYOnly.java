package Assignment1;

public class ConvertYUVToYOnly {

	public ConvertYUVToYOnly() {

        return;
	}


	public static byte[] convert420ToYOnly(String inFileName, String outFileName, int vidWidth, int vidHeight) {

		byte[] input = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + inFileName);
		byte[] output = new byte[input.length * 2 / 3];

		// Collect all yBytes and write to new file
        int processCount = 0;
        int writeCount = 0;
        while (processCount < input.length) {

            // Copy Y bytes
            for(int i=0; i < vidWidth*vidHeight; i++) {
            	output[writeCount] = input[processCount+i];
            	writeCount += 1;
            }
            processCount += vidWidth*vidHeight;
            processCount += vidWidth*vidHeight / 4; // Skip all U
            processCount += vidWidth*vidHeight / 4; // Skip all V

        }
        FileHelper.writeToFile(output, outFileName);
        return output;
	}

	public static void main(String[] args) {

		ConvertYUVToYOnly.convert420ToYOnly("foreman_cif.yuv", "foreman_cif_y_only.yuv", 352, 288);
    }

}

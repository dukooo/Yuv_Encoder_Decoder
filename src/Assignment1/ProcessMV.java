package Assignment1;

public class ProcessMV {

	static byte[] mv;

	public static void processmv() {
		PropertyHelper ph = new PropertyHelper();
		mv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvFileName"));
		int frameCount = Integer.valueOf(ph.getProperty("frameCount"));
		int iPeriod = Integer.valueOf(ph.getProperty("I_Period"));
		int blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		int width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		int height = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		int blockNum = width * height / blockSize / blockSize;
		byte[] mvProcessed = new byte[mv.length - ((frameCount - 1) / iPeriod + 1) * blockNum];
		int numIframe = 0;
		int numPframe = 0;
		for (int i = 0; i < frameCount; i++) {
			if (i % iPeriod == 0) {
				for (int j = 0; j < blockNum; j++) {
					mvProcessed[numIframe * blockNum + numPframe * blockNum * 2 + j] = mv[i * blockNum * 2 + j * 2 + 1];
				}
				numIframe++;
			}
			else {
				for (int j = 0; j < blockNum * 2; j++)
					mvProcessed[numIframe * blockNum + numPframe * blockNum * 2 + j] = mv[i * blockNum * 2 + j];
				numPframe++;
			}
		}
		FileHelper.writeToFile(mvProcessed, ph.getProperty("mvFileName"));
		//FileHelper.writeToFile(mvProcessed, "mvProcess.mv");
	}

	public static byte[] reverseProcessmv(byte[] mvProcessed) {
		PropertyHelper ph = new PropertyHelper();
		//byte[] mvProcessed = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvFileName"));
		//byte[] mvProcessed = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + "mvProcess.mv");
		int frameCount = Integer.valueOf(ph.getProperty("frameCount"));
		int iPeriod = Integer.valueOf(ph.getProperty("I_Period"));
		int blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		int width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		int height = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		int blockNum = width * height / blockSize / blockSize;
		if (mvProcessed.length + ((frameCount - 1) / iPeriod + 1) * blockNum != blockNum * frameCount * 2) {
			System.out.println(mvProcessed.length);
			System.out.println(((frameCount - 1) / iPeriod + 1) * blockNum);
			System.out.println(blockNum * frameCount * 2);
		}
		byte[] mvOriginal = new byte[blockNum * frameCount * 2];
		int numIframe = 0;
		int numPframe = 0;
		for (int i = 0; i < frameCount; i++) {
			if (i % iPeriod == 0) {
				for (int j = 0; j < blockNum; j++) {
					mvOriginal[i * blockNum * 2 + j * 2] = 0;
					mvOriginal[i * blockNum * 2 + j * 2 + 1] = mvProcessed[numIframe * blockNum + numPframe * blockNum * 2 + j];
				}
				numIframe++;
			}
			else {
				for (int j = 0; j < blockNum * 2; j++)
					mvOriginal[i * blockNum * 2 + j] = mvProcessed[numIframe * blockNum + numPframe * blockNum * 2 + j];
				numPframe++;
			}
		}
		return mvOriginal;
	}

	/*
	public static void main(String[] args) {
		processmv();
		byte[] mvOriginal = reverseProcessmv();
		if (mvOriginal.length != mv.length)
			System.out.println(mvOriginal.length + " " + mv.length);
		for (int i = 0; i < mvOriginal.length; i++) {
			if (mv[i] != mvOriginal[i])
				System.out.println("error");
		}
	}
	*/
}

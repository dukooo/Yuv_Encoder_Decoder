package Assignment3;

import java.time.ZonedDateTime;

public class A3Part2Report {

	int width, height, frameLength;
    
	public A3Part2Report() {
		PropertyHelper ph = new PropertyHelper();
		width = Integer.valueOf(ph.getProperty("vidWidth"));
        height = Integer.valueOf(ph.getProperty("vidHeight"));
        frameLength = Integer.valueOf(ph.getProperty("frameCount"));
	}
	
	public int findBitCount(String filename) {
		PropertyHelper ph = new PropertyHelper();
		byte[] ary = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty(filename));
		return ary.length * 8;
	}
	
	public int getAllBitcount(int RCflag) {
		int bitcount = findBitCount("mvEntropyEncodedFileName")
				 + findBitCount("qtcEntropyEncodedFileName")
				 + findBitCount("vbsEntropyEncodedFileName");
		if (RCflag != 0) bitcount += findBitCount("qpEntropyEncodedFileName");
		if (RCflag == 2 || RCflag == 3) bitcount += findBitCount("scEntropyEncodedFileName");
		return bitcount;
	}
	
	public double findPSNR(String originalFile, String decodedFile) {
		byte[] originalY = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + originalFile);
		byte[] decodedY = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + decodedFile);
		PropertyHelper ph = new PropertyHelper();
		double psnrSum = 0;
		int frameCount = Integer.valueOf(ph.getProperty("frameCount"));
		for (int i = 0; i < frameCount; i++) {
			psnrSum += findPSNRofFrame(i, originalY, decodedY);
		}
		return psnrSum / Integer.valueOf(ph.getProperty("frameCount"));
	}
	
	public double findPSNRofFrame(int frameIndex, byte[] originalY, byte[] decodedY) {
		double mseSum = 0;
		for (int i = 0; i < width * height; i++) {
			double sub = (int)(originalY[frameIndex * width * height + i] & 0xFF)
					   - (int)(decodedY[frameIndex * width * height + i] & 0xFF);
			mseSum += Math.pow(sub, 2);
		}
		double mse = mseSum / width / height;
		return 10 * Math.log10(Math.pow(Math.pow(2, 8) - 1, 2) / mse);
	}
	
	public void generateGraph1(boolean append, int RCflag, int qpORbitrate, int bitcount, double psnr, long encodingTime, String outputFile) {
		if (append == false) {
			//Write first line
			String tmp = "RCflag,qp/bitrate,bitcount,psnr,time\n";
			FileHelper.writeToFile(tmp.getBytes(), outputFile);
		}
		String tmp = RCflag + "," + qpORbitrate + "," + bitcount + "," + psnr + "," + encodingTime + "\n";
		FileHelper.writeToFileAppend(tmp.getBytes(), outputFile);
	}
	
	public void bitCountVSquality(boolean append, String outputFile) {
		PropertyHelper ph = new PropertyHelper();
		int qp = Integer.valueOf(ph.getProperty("quantizationPar"));
		int bitrate = Integer.valueOf(ph.getProperty("targetBR"));
		int RCflag = Integer.valueOf(ph.getProperty("RCflag"));
		Encoder en = new Encoder();
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
        long encodingTime = endTimeMs - startTimeMs;
		int bitcount = getAllBitcount(RCflag);
		Decoder de = new Decoder();
		de.startDecoding();
		double psnr = findPSNR(ph.getProperty("vidName"), ph.getProperty("decReconstructedFileName"));
		if (RCflag == 0)
			generateGraph1(append, RCflag, qp, bitcount, psnr, encodingTime, outputFile);
		else
			generateGraph1(append, RCflag, bitrate, bitcount, psnr, encodingTime, outputFile);
	}
	
    public void psnrVSFrameIndex(String outFileName) {
        PropertyHelper ph = new PropertyHelper();
        int frameCount = Integer.valueOf(ph.getProperty("frameCount"));
		double[] psnr1 = new double[frameCount];
		double[] psnr2 = new double[frameCount];
		double[] psnr3 = new double[frameCount];
		String originalFile = ph.getProperty("vidName");
		String decodedFile = ph.getProperty("decReconstructedFileName");
		// RCflag == 1
		System.out.println("----------> Working on graph 2 with RCflag = " + 1);
		ph.setProperty("RCflag", "1");
		Encoder en = new Encoder();
		en.startEncoding();
		Decoder de = new Decoder();
		de.startDecoding();
		byte[] originalY = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + originalFile);
		byte[] decodedY = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + decodedFile);
		for (int i = 0; i < frameCount; i++) {
			psnr1[i] = findPSNRofFrame(i, originalY, decodedY);
		}
		// RCflag == 2
		System.out.println("----------> Working on graph 2 with RCflag = " + 2);
		ph.setProperty("RCflag", "2");
		en = new Encoder();
		en.startEncoding();
		de = new Decoder();
		de.startDecoding();
		originalY = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + originalFile);
		decodedY = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + decodedFile);
		for (int i = 0; i < frameCount; i++) {
			psnr2[i] = findPSNRofFrame(i, originalY, decodedY);
		}
		// RCflag == 3
		System.out.println("----------> Working on graph 2 with RCflag = " + 3);
		ph.setProperty("RCflag", "3");
		en = new Encoder();
		en.startEncoding();
		de = new Decoder();
		de.startDecoding();
		originalY = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + originalFile);
		decodedY = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + decodedFile);
		for (int i = 0; i < frameCount; i++) {
			psnr3[i] = findPSNRofFrame(i, originalY, decodedY);
		}
		generateGraph2(psnr1, psnr2, psnr3, frameCount, outFileName);
	}
    
	public void generateGraph2(double[] psnr1, double[] psnr2, double[] psnr3, int frameLength, String outputFile) {
		String tmp = "frame index,RCflag = 1,RCflag = 2,RCflag = 3\n";
		FileHelper.writeToFile(tmp.getBytes(), outputFile);
		for (int i = 0; i < frameLength; i++) {
			tmp = (i + 1) + "," + psnr1[i] + "," + psnr2[i] + "," + psnr3[i] + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outputFile);
		}
	}
	
	public static void main(String[] args) {

		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		PropertyHelper ph = new PropertyHelper();
		A3Part2Report re = new A3Part2Report();
		ph.setProperty("vidName", "CIF_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
        ph.setProperty("frameCount", "21");
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "true");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("I_Period", "21");
        ////////////////////////////////////////////////////////
        // Part 1
        ////////////////////////////////////////////////////////
        String outputFile = "part2graph1.csv";
        /*
        // RCflag == 0, qp = 3, 6, 9
        ph.setProperty("RCflag", "0");
        ph.setProperty("quantizationPar", "3");
        System.out.println("----------> Working on graph 1 with RCflag = 0 and qp = 3");
        re.bitCountVSquality(false, outputFile);
        ph.setProperty("quantizationPar", "6");
        System.out.println("----------> Working on graph 1 with RCflag = 0 and qp = 6");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("quantizationPar", "9");
        System.out.println("----------> Working on graph 1 with RCflag = 0 and qp = 9");
        re.bitCountVSquality(true, outputFile);
        // RCflag == 1, bitrate = 7000000, 2400000, 360000
        ph.setProperty("RCflag", "1");
        ph.setProperty("targetBR", "7000000");
        System.out.println("----------> Working on graph 1 with RCflag = 1 and targetBR = 7000000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "2400000");
        System.out.println("----------> Working on graph 1 with RCflag = 1 and targetBR = 2400000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "360000");
        System.out.println("----------> Working on graph 1 with RCflag = 1 and targetBR = 360000");
        re.bitCountVSquality(true, outputFile);
        // RCflag == 2, bitrate = 7000000, 2400000, 360000
        ph.setProperty("RCflag", "2");
        ph.setProperty("targetBR", "7000000");
        System.out.println("----------> Working on graph 1 with RCflag = 2 and targetBR = 7000000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "2400000");
        System.out.println("----------> Working on graph 1 with RCflag = 2 and targetBR = 2400000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "360000");
        System.out.println("----------> Working on graph 1 with RCflag = 2 and targetBR = 360000");
        re.bitCountVSquality(true, outputFile);
        // RCflag == 3, bitrate = 7000000, 2400000, 360000
        ph.setProperty("RCflag", "3");
        ph.setProperty("targetBR", "7000000");
        System.out.println("----------> Working on graph 1 with RCflag = 3 and targetBR = 7000000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "2400000");
        System.out.println("----------> Working on graph 1 with RCflag = 3 and targetBR = 2400000");
        re.bitCountVSquality(true, outputFile);
        ph.setProperty("targetBR", "360000");
        System.out.println("----------> Working on graph 1 with RCflag = 3 and targetBR = 360000");
        re.bitCountVSquality(true, outputFile);
		*/
        ////////////////////////////////////////////////////////
        // Part 2
        ////////////////////////////////////////////////////////
        ph.setProperty("targetBR", "2000000");
        re.psnrVSFrameIndex("part2graph2.csv");
		long endTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println(endTimeMs - startTimeMs);
	}
	
}

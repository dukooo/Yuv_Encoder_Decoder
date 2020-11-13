package Assignment1;

import java.time.ZonedDateTime;

public class Exercise4Report {

	int width;
	int height;

	public Exercise4Report() {
		PropertyHelper ph = new PropertyHelper();
		this.width = Integer.valueOf(ph.getProperty("vidWidth"));
		this.height = Integer.valueOf(ph.getProperty("vidHeight"));

	}
	
	public int findBitCount(String filename) {
		byte[] ary = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + filename);
		return ary.length * 8;
	}
	
	public double findPSNR(byte[] originalY, byte[] decodedY) {
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
	
	public void bitCountVSquality(int qp, boolean append, String outFileName1) {
		PropertyHelper ph = new PropertyHelper();
		ph.setProperty("quantizationPar", Integer.toString(qp));
		EncoderExercise4 en = new EncoderExercise4();
		// find the execution time
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
		long encodingTime = endTimeMs - startTimeMs;
		System.out.println("For qp = " + qp + ", blockSize i = " + ph.getProperty("blkSizeI") + " Execution time (ms) = " + encodingTime);
		int bitCount = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
					 + findBitCount(ph.getProperty("mvEntropyEncodedFileName"));
		DecoderExercise4 dec = new DecoderExercise4();
		dec.startDecoding();
		byte[] original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		//byte[] decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		byte[] decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		double psnr = findPSNR(original_y_only_yuv, decoded_y_only_yuv);
		generateGraph1(bitCount, psnr, encodingTime, append, outFileName1);
	}
	
	public void bitCountVaryingIPeriod(int i_period) {
		PropertyHelper ph = new PropertyHelper();
		ph.setProperty("blkSizeI", "8");
		ph.setProperty("rangeR", "2");
		Exercise4Report ex4rep = new Exercise4Report();
		// i = 8 and qp = 0, 3, 6, 9
		if (i_period == 1)
			ex4rep.bitCountVSquality(0, false, "bitcountVSquality.csv");
		else 
			ex4rep.bitCountVSquality(0, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 8, qp = 0");
		ex4rep.bitCountVSquality(3, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 8, qp = 3");
		ex4rep.bitCountVSquality(6, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 8, qp = 6");
		ex4rep.bitCountVSquality(9, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 8, qp = 9");
		// i = 16 and qp = 1, 4, 7, 10
		ph.setProperty("blkSizeI", "16");
		ex4rep.bitCountVSquality(1, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 16, qp = 1");
		ex4rep.bitCountVSquality(4, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 16, qp = 4");
		ex4rep.bitCountVSquality(7, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 16, qp = 7");
		ex4rep.bitCountVSquality(10, true, "bitcountVSquality.csv");
		System.out.println("Done for I_Period = " + i_period + ", i = 16, qp = 10");
		
	}
	
	public void generateGraph1(int bitcount, double psnr, long encodingTime, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "i,qp,i-period,bitcount,psnr,time\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
			tmp = ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			bitcount + "," + psnr + "," + encodingTime + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
		else {
			String tmp = ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			bitcount + "," + psnr + "," + encodingTime + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
	}
	
	public void bitCountVSFrameIndex(boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		int[] bitCount = new int[10];
		for (int i = 1; i < 11; i++) {
			ph.setProperty("frameCount", String.valueOf(i));
			EncoderExercise4 en = new EncoderExercise4();
			en.startEncoding();
			bitCount[i - 1] = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
						 + findBitCount(ph.getProperty("mvEntropyEncodedFileName"));
		}
		int[] diff = new int[10];
		diff[0] = bitCount[0];
		for (int i = 1; i < 10; i++) {
			diff[i] = bitCount[i] - bitCount[i - 1];
		}
		generateGraph2(diff, append, outFileName);
	}
	
	public void generateGraph2(int[] bitCount, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "i,qp,i-period,frame index,bitcount\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
		}
		for (int i = 0; i < 10; i++) {
			String tmp = ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			i + "," + bitCount[i] + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
	}
	
	public static void main(String[] args) {
		PropertyHelper ph = new PropertyHelper();
		ph.setProperty("vidName", "foreman_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
		ph.setProperty("frameCount", "10");

		Exercise4Report ex4rep = new Exercise4Report();
		ph.setProperty("I_Period", "1");
		ex4rep.bitCountVaryingIPeriod(1);
		ph.setProperty("I_Period", "4");
		ex4rep.bitCountVaryingIPeriod(4);
		ph.setProperty("I_Period", "10");
		ex4rep.bitCountVaryingIPeriod(10);
		
		/*
		// plot the bit-count vs frameIndex, using i = 8, qp = 3, i-period = 1, 4, 10
		ph.setProperty("blkSizeI", "8");
		ph.setProperty("quantizationPar", "3");
		ph.setProperty("I_Period", "1");
		ex4rep.bitCountVSFrameIndex(false, "bitcountVSframeIndex.csv");
		ph.setProperty("I_Period", "4");
		ex4rep.bitCountVSFrameIndex(true, "bitcountVSframeIndex.csv");
		ph.setProperty("I_Period", "10");
		ex4rep.bitCountVSFrameIndex(true, "bitcountVSframeIndex.csv");
		// plot the bit-count vs frameIndex, using i = 16, qp = 4, i-period = 1, 4, 10
		ph.setProperty("blkSizeI", "16");
		ph.setProperty("quantizationPar", "4");
		ph.setProperty("I_Period", "1");
		ex4rep.bitCountVSFrameIndex(true, "bitcountVSframeIndex.csv");
		ph.setProperty("I_Period", "4");
		ex4rep.bitCountVSFrameIndex(true, "bitcountVSframeIndex.csv");
		ph.setProperty("I_Period", "10");
		ex4rep.bitCountVSFrameIndex(true, "bitcountVSframeIndex.csv");
		*/
	}
}

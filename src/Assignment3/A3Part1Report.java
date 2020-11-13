package Assignment3;

import java.time.ZonedDateTime;

public class A3Part1Report {

	public A3Part1Report() {	}
    
    // PART 1: PSNR vs Frame ////////////////////////////////////////////////////////////////////////////////////
	public double findPSNRofFrame(int frameIndex, byte[] originalY, byte[] decodedY) {
        PropertyHelper ph = new PropertyHelper();
		int width = Integer.valueOf(ph.getProperty("vidWidth"));
        int height = Integer.valueOf(ph.getProperty("vidHeight"));

		double mseSum = 0;
		for (int i = 0; i < width * height; i++) {
			double sub = (int)(originalY[frameIndex * width * height + i] & 0xFF)
					   - (int)(decodedY[frameIndex * width * height + i] & 0xFF);
			mseSum += Math.pow(sub, 2);
		}
		double mse = mseSum / width / height;
		return 10 * Math.log10(Math.pow(Math.pow(2, 8) - 1, 2) / mse);
	}
	  
    public void generateGraph1(double[] PSNR, boolean append, String outFileName) {
        PropertyHelper ph = new PropertyHelper();
        int frameCount = Integer.valueOf(ph.getProperty("frameCount"));
		if (append == false) {
			//Write first line
			String tmp = "RC,File Name,Blksize,I-Period,Frame Index,PSNR\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
		}
		for (int i = 0; i < frameCount; i++) {
            String tmp = ph.getProperty("RCflag") + "," + ph.getProperty("vidName") + "," + ph.getProperty("blkSizeI") + "," + 
                         ph.getProperty("I_Period") + "," + i + "," + PSNR[i] + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
    }
    
    public void PSNRVSFrameIndex(boolean append, String outFileName) {
        PropertyHelper ph = new PropertyHelper();
        int frameCount = 21;

        Encoder en = new Encoder();
		en.startEncoding();
		Decoder dec = new Decoder();
        dec.startDecoding();

		byte[] original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
        byte[] decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
        
        double[] PSNR = new double[frameCount];
        for (int i = 0; i < frameCount; i++) {
            PSNR[i] = findPSNRofFrame(i, original_y_only_yuv, decoded_y_only_yuv);
		}

		generateGraph1(PSNR, append, outFileName);
    }

    // PART 2: Encoded Bit Stream vs Frame ////////////////////////////////////////////////////////////////////////
    public int findBitCount(String filename) {
		byte[] ary = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + filename);
		return ary.length * 8;
	}

    public void generateGraph2(int[] bitCount, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "RC,File Name,Blksize,I-Period,Frame Index,Bitcount\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
		}
		for (int i = 0; i < 21; i++) {
			String tmp = ph.getProperty("RCflag") + "," + ph.getProperty("vidName") + "," + ph.getProperty("blkSizeI") + "," + 
                         ph.getProperty("I_Period") + "," + i + "," + bitCount[i] + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
	}

    public void bitCountVSFrameIndex(boolean append, String outFileName) {
        PropertyHelper ph = new PropertyHelper();
        int frameCount = 21;
        int[] bitCount = new int[frameCount];
        
		for (int i = 1; i < frameCount+1; i++) {
			ph.setProperty("frameCount", String.valueOf(i));
			Encoder en = new Encoder();
			en.startEncoding();
			bitCount[i - 1] = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
                            + findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
                            + findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
            if (ph.getProperty("RCflag") == "1") bitCount[i-1] += findBitCount(ph.getProperty("qpEntropyEncodedFileName"));
		}
		int[] diff = new int[frameCount];
		diff[0] = bitCount[0];
		for (int i = 1; i < frameCount; i++) {
			diff[i] = bitCount[i] - bitCount[i - 1];
		}
		generateGraph2(diff, append, outFileName);
	}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void Part1_CIF_RC_On(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "1");
        ph.setProperty("vidName", "CIF_y_only.yuv");
        ph.setProperty("targetBR", "2400000");
		ph.setProperty("vidWidth", "352");
        ph.setProperty("vidHeight", "288");
        
        ph.setProperty("I_Period", "1");
        PSNRVSFrameIndex(append, "PSNR_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 1, RC = 1 DONE - PSNR vs Frame");

        ph.setProperty("I_Period", "4");
        PSNRVSFrameIndex(true, "PSNR_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 4, RC = 1 DONE - PSNR vs Frame");

        ph.setProperty("I_Period", "21");
        PSNRVSFrameIndex(true, "PSNR_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 21, RC = 1 DONE - PSNR vs Frame");
    }

    public void Part1_QCIF_RC_On(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "1");
        ph.setProperty("vidName", "QCIF_y_only.yuv");
        ph.setProperty("targetBR", "960000");
		ph.setProperty("vidWidth", "176");
        ph.setProperty("vidHeight", "144");
        
        ph.setProperty("I_Period", "1");
        PSNRVSFrameIndex(append, "PSNR_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 1, RC = 1 DONE - PSNR vs Frame");

        ph.setProperty("I_Period", "4");
        PSNRVSFrameIndex(true, "PSNR_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 4, RC = 1 DONE - PSNR vs Frame");

        ph.setProperty("I_Period", "21");
        PSNRVSFrameIndex(true, "PSNR_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 21, RC = 1 DONE - PSNR vs Frame");
    }

    public void Part1_CIF_RC_Off(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "0");
        ph.setProperty("quantizationPar", "6");
        ph.setProperty("vidName", "CIF_y_only.yuv");
		ph.setProperty("vidWidth", "352");
        ph.setProperty("vidHeight", "288");
        
        ph.setProperty("I_Period", "21");
        PSNRVSFrameIndex(append, "PSNR_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 21, RC = 0, FixedQP = 6 DONE - PSNR vs Frame");
    }

    public void Part1_QCIF_RC_Off(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "0");
        ph.setProperty("quantizationPar", "6");
        ph.setProperty("vidName", "QCIF_y_only.yuv");
		ph.setProperty("vidWidth", "176");
		ph.setProperty("vidHeight", "144");
        
        ph.setProperty("I_Period", "21");
        PSNRVSFrameIndex(append, "PSNR_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 21, RC = 0, FixedQP = 6 DONE - PSNR vs Frame");
    }
    
    public void Part2_CIF_RC_On(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "1");
        ph.setProperty("vidName", "CIF_y_only.yuv");
        ph.setProperty("targetBR", "2400000");
		ph.setProperty("vidWidth", "352");
        ph.setProperty("vidHeight", "288");
        
        ph.setProperty("I_Period", "1");
        bitCountVSFrameIndex(append, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 1, RC = 1 DONE - Bitcount vs Frame");

        ph.setProperty("I_Period", "4");
        bitCountVSFrameIndex(true, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 4, RC = 1 DONE - Bitcount vs Frame");

        ph.setProperty("I_Period", "21");
        bitCountVSFrameIndex(true, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 21, RC = 1 DONE - Bitcount vs Frame");
    }

    public void Part2_QCIF_RC_On(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "1");
        ph.setProperty("vidName", "QCIF_y_only.yuv");
        ph.setProperty("targetBR", "960000");
		ph.setProperty("vidWidth", "176");
        ph.setProperty("vidHeight", "144");
        
        ph.setProperty("I_Period", "1");
        bitCountVSFrameIndex(append, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 1, RC = 1 DONE - Bitcount vs Frame");

        ph.setProperty("I_Period", "4");
        bitCountVSFrameIndex(true, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 4, RC = 1 DONE - Bitcount vs Frame");

        ph.setProperty("I_Period", "21");
        bitCountVSFrameIndex(true, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 21, RC = 1 DONE - Bitcount vs Frame");
    }

    public void Part2_CIF_RC_Off(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "0");
        ph.setProperty("quantizationPar", "6");
        ph.setProperty("vidName", "CIF_y_only.yuv");
		ph.setProperty("vidWidth", "352");
        ph.setProperty("vidHeight", "288");
        
        ph.setProperty("I_Period", "21");
        bitCountVSFrameIndex(append, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [CIF_y_only.yuv] I_PERIOD = 21, RC = 0, FixedQP=6 DONE - Bitcount vs Frame");
    }

    public void Part2_QCIF_RC_Off(boolean append) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("RCflag", "0");
        ph.setProperty("quantizationPar", "6");
        ph.setProperty("vidName", "QCIF_y_only.yuv");
		ph.setProperty("vidWidth", "176");
        ph.setProperty("vidHeight", "144");
        
        ph.setProperty("I_Period", "21");
        bitCountVSFrameIndex(append, "Bitcount_vs_Frame.csv");
        System.out.println("-------> [QCIF_y_only.yuv] I_PERIOD = 21, RC = 0, FixedQP=6 DONE - Bitcount vs Frame");
    }

    public static void main(String[] args) {
        PropertyHelper ph = new PropertyHelper();
        A3Part1Report rep = new A3Part1Report();

        ph.setProperty("rangeR", "4");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "true");
        ph.setProperty("frameCount", "21");

        rep.Part1_CIF_RC_On(false);
        rep.Part1_QCIF_RC_On(true);
        rep.Part1_CIF_RC_Off(true);
        // rep.Part1_QCIF_RC_Off(true);
        System.out.println("-------------------------------PART 1 DONE-------------------------------");

        ph.setProperty("frameCount", "21");
        rep.Part2_CIF_RC_On(false);
        rep.Part2_QCIF_RC_On(true);
        rep.Part2_CIF_RC_Off(true);
        // rep.Part2_QCIF_RC_Off(true);
        System.out.println("-------------------------------PART 2 DONE-------------------------------");
	}
}

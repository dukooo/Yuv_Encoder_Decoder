package Assignment2;

import java.time.ZonedDateTime;

public class A2Report {

	int width;
	int height;
    int frameNumSynthetic;
	public A2Report() {
		PropertyHelper ph = new PropertyHelper();
		this.width = Integer.valueOf(ph.getProperty("vidWidth"));
        this.height = Integer.valueOf(ph.getProperty("vidHeight"));
		this.frameNumSynthetic = 30;
	}
    
    // PART 1: PSNR vs Bitcount ////////////////////////////////////////////////////////////////////////////////////
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
	
	public void bitCountVSquality(int qp, boolean append, String outFileName1, int mode) {
		PropertyHelper ph = new PropertyHelper();
		ph.setProperty("quantizationPar", Integer.toString(qp));
		Encoder en = new Encoder();
		// find the execution time
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
        long encodingTime = endTimeMs - startTimeMs;
        // System.out.println("------>[Encoding Took " + encodingTime + "ms for QP = " + qp + ", I_Period = 8, blkSizeI = 16, rangeR = 4]");
		int bitCount = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
					 + findBitCount(ph.getProperty("mvEntropyEncodedFileName"));
		
		if (this.findMode(mode) == "VBS On" || this.findMode(mode) == "All Features On")
			bitCount += findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
			
		Decoder dec = new Decoder();
        dec.startDecoding();
		byte[] original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		byte[] decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		double psnr = findPSNR(original_y_only_yuv, decoded_y_only_yuv);
		generateGraph1(bitCount, psnr, encodingTime, append, outFileName1, mode);
	}
	
	public void bitCountVaryingMode(int mode) {
		PropertyHelper ph = new PropertyHelper();
        ph.setProperty("rangeR", "4");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("I_Period", "8");
        A2Report ex4rep = new A2Report();
        System.out.println("------>[[ Mode: " + findMode(mode)+" ]]");

        // i = 16 and qp = 1, 4, 7, 10
        if(mode == 1)
            ex4rep.bitCountVSquality(1, false, "RD_foreman_bitcountVSquality.csv", mode);
        else 
            ex4rep.bitCountVSquality(1, true, "RD_foreman_bitcountVSquality.csv", mode);
        System.out.println("------>[Calculation Done for QP = 1, I_Period = 8, blkSizeI = 16, rangeR = 4]");
		ex4rep.bitCountVSquality(4, true, "RD_foreman_bitcountVSquality.csv", mode);
        System.out.println("------>[Calculation Done for QP = 4, I_Period = 8, blkSizeI = 16, rangeR = 4]");
		ex4rep.bitCountVSquality(7, true, "RD_foreman_bitcountVSquality.csv", mode);
        System.out.println("------>[Calculation Done for QP = 7, I_Period = 8, blkSizeI = 16, rangeR = 4]");
		ex4rep.bitCountVSquality(10, true, "RD_foreman_bitcountVSquality.csv", mode);
        System.out.println("------>[Calculation Done for QP = 10, I_Period = 8, blkSizeI = 16, rangeR = 4]");
	}
	
	public void generateGraph1(int bitcount, double psnr, long encodingTime, boolean append, String outFileName, int mode) {
        String modeName = findMode(mode);
        
        PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "Mode,i,qp,i-period,bitcount,psnr,time\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
			tmp = modeName + "," + ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			bitcount + "," + psnr + "," + encodingTime + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
		else {
			String tmp = modeName + "," + ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			bitcount + "," + psnr + "," + encodingTime + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
    }
    
    public String findMode(int mode) {
        String modeName = "";
        switch (mode) {
            case 1:
                modeName = "No Feature On";
                break;
            case 2:
                modeName = "nRefFrame On (4)";
                break;
            case 3:
                modeName = "VBS On";
                break;
            case 4:
                modeName = "FME On";
                break;
            case 5:
                modeName = "FastME On";
                break;
            case 6:
                modeName = "All Features On";
                break;
        }
        return modeName;
    }

    // PART 2: SAD vs Frame ////////////////////////////////////////////////////////////////////////////////////
    /**
	 * Calculate SAD for each frame. Basically adding up the difference between each pixel.
	 * @param frame1
	 * @param frame2
	 * @return Integer SAD.
	 */
	public int getPerFrameSAD(byte [] frame1, byte[] frame2) {
		int sad = 0;
		for (int i = 0; i < frame1.length; i++) {
			sad += Math.abs(Helper.unsignedByteToInt(frame1[i]) - Helper.unsignedByteToInt(frame2[i]));
		}
		return sad;
	}

	/**
	 * Calculate SAD for X number of frames. X=this.frameNum which is 10 for the purpose of this report.
	 * @param vid1
	 * @param vid2
	 * @return An integer array of per frame SAD.
	 */
	public int[] getSADForAllFrames(byte[] vid1, byte[] vid2) {
		int [] ret = new int[this.frameNumSynthetic];
		for (int i = 0; i < this.frameNumSynthetic; i++) {
			// for each frame, calculate 1 SAD

			// Copy just 1 frame for calculation
			byte[] frame1 = new byte[this.height*this.width];
			byte[] frame2 = new byte[this.height*this.width];
			for (int j = 0; j < this.height*this.width; j++) {
				frame1[j] = vid1[i*this.height*this.width + j];
				frame2[j] = vid2[i*this.height*this.width + j];
			}
			ret[i] = this.getPerFrameSAD(frame1, frame2);
		}
		return ret;
    }

    	/**
	 * Create a CSV file of with frameNum,SAD
	 * @param outFileName
	 * @param SAD
	 */
	public void genSADGraph(String outFileName, int[] SAD, boolean append) {
		if (append == false) {
			//Write first line
			String tmp = "0," + Integer.toString(SAD[0]) + "\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);

			for (int i = 1; i < SAD.length; i++) {
				tmp = Integer.toString(i) + "," + Integer.toString(SAD[i]) + "\n";
				FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
			}
		}
		else {
			String tmp;
			for (int i = 0; i < SAD.length; i++) {
				tmp = Integer.toString(i) + "," + Integer.toString(SAD[i]) + "\n";
				FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
			}
		}
    }
    
    public void SADVarying_nRefFrames(int nRefFrames, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		byte[] original_y_only_yuv;
		byte[] decoded_y_only_yuv;
		Encoder en;
		Decoder dec;
		int [] sads;

        ph.setProperty("nRefFrames", Integer.toString(nRefFrames));
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "false");
        ph.setProperty("quantizationPar", "4");

		// enc and dec
		en = new Encoder();
		en.startEncoding();
		dec = new Decoder();
        dec.startDecoding();
        
		// read files
		original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		//gen SADs
		sads = getSADForAllFrames(original_y_only_yuv, decoded_y_only_yuv);
		genSADGraph(outFileName, sads, append);
    }
    
    // PART 3: Encoded Bit Stream vs Frame ////////////////////////////////////////////////////////////////////////////////////
    public void generateGraph2(int[] bitCount, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "nRefFrames, i,qp,i-period,frame index,bitcount\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
		}
		for (int i = 0; i < 30; i++) {
			String tmp = ph.getProperty("nRefFrames") + "," + ph.getProperty("blkSizeI") + "," + ph.getProperty("quantizationPar") + "," + ph.getProperty("I_Period") + "," + 
			i + "," + bitCount[i] + "\n";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
		}
	}

    public void bitCountVSFrameIndex(boolean append, String outFileName) {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "false");
		int[] bitCount = new int[30];
		for (int i = 1; i < 31; i++) {
			ph.setProperty("frameCount", String.valueOf(i));
			Encoder en = new Encoder();
			en.startEncoding();
			bitCount[i - 1] = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
						 + findBitCount(ph.getProperty("mvEntropyEncodedFileName"));
		}
		int[] diff = new int[30];
		diff[0] = bitCount[0];
		for (int i = 1; i < 30; i++) {
			diff[i] = bitCount[i] - bitCount[i - 1];
		}
		generateGraph2(diff, append, outFileName);
	}

	public static void main(String[] args) {
        PropertyHelper ph = new PropertyHelper();
        A2Report ex4rep = new A2Report();

    // PART 1 ////////////////////////////////////////////////////////////////////////////////////
		ph.setProperty("vidName", "foreman_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
        ph.setProperty("frameCount", "10");
        
        // PSNR vs Bitcount: I_Period = 8, QP = 1, 4, 7, 10, Block Size = 16, Search Range = 4
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "false");
        ex4rep.bitCountVaryingMode(1); // No feature on
        
        ph.setProperty("nRefFrames", "4");
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "false");
        ex4rep.bitCountVaryingMode(2); // nRefFrame on
        
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "false");
        ex4rep.bitCountVaryingMode(3); // VBS on
        
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "false");
        ex4rep.bitCountVaryingMode(4); // FME on

        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "0");
        ph.setProperty("FMEEnable", "false");
        ph.setProperty("fastME", "true");
        ex4rep.bitCountVaryingMode(5); // fastME on

        ph.setProperty("nRefFrames", "4");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "true");
        ex4rep.bitCountVaryingMode(6); // All features on
        System.out.println("               PART 1 DONE");

    // PART 2 ////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "synthetic_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
        ph.setProperty("frameCount", "30");
        ph.setProperty("rangeR", "4");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("I_Period", "40"); // infinite I_Period

        // Per-Frame SAD Plot: QP = 4, every features off except for nRefFrames
        ex4rep.SADVarying_nRefFrames(1, false, "SAD_synthetic_cif_varying_nRefFrames.csv");
        System.out.println("------> [nRefFrame = 1 Done]");

        ex4rep.SADVarying_nRefFrames(2, true, "SAD_synthetic_cif_varying_nRefFrames.csv");
        System.out.println("------> [nRefFrame = 2 Done]");

        ex4rep.SADVarying_nRefFrames(3, true, "SAD_synthetic_cif_varying_nRefFrames.csv");
        System.out.println("------> [nRefFrame = 3 Done]");
        
        ex4rep.SADVarying_nRefFrames(4, true, "SAD_synthetic_cif_varying_nRefFrames.csv");
        System.out.println("------> [nRefFrame = 4 Done]");
        System.out.println("               PART 2 DONE");
        
    // PART 3 ////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "synthetic_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
        ph.setProperty("frameCount", "30");
        ph.setProperty("quantizationPar", "4");
        ph.setProperty("rangeR", "4");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("I_Period", "40"); // infinite I Period 

        // Encoded Bitstream vs Frame: QP = 4, every features off except for nRefFrames
        ph.setProperty("nRefFrames", "1");
        ex4rep.bitCountVSFrameIndex(false, "BeatStream_synthetic_bitcountVSframeIndex.csv");
        System.out.println("------> [nRefFrame = 1 Done]");

        ph.setProperty("nRefFrames", "2");
        ex4rep.bitCountVSFrameIndex(true, "BeatStream_synthetic_bitcountVSframeIndex.csv");
        System.out.println("------> [nRefFrame = 2 Done]");

        ph.setProperty("nRefFrames", "3");
        ex4rep.bitCountVSFrameIndex(true, "BeatStream_synthetic_bitcountVSframeIndex.csv");
        System.out.println("------> [nRefFrame = 3 Done]");

        ph.setProperty("nRefFrames", "4");
        ex4rep.bitCountVSFrameIndex(true, "BeatStream_synthetic_bitcountVSframeIndex.csv");
        System.out.println("------> [nRefFrame = 4 Done]");
        System.out.println("               PART 3 DONE");
	}
}

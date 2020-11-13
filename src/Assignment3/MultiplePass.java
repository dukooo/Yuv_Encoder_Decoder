package Assignment3;

public class MultiplePass {
	
	//                            qp  =   0,      1,      2,      3,      4,      5,      6,     7,     8,     9,     10,    11
	final static int[] QCIF_THRESHOLD = {130000, 130000, 80000,  60000,  70000,  30000,  20000, 15000, 10000, 5000,  3000,  2100};
	final static int[] CIF_THRESHOLD  = {450000, 450000, 250000, 200000, 200000, 100000, 80000, 75000, 35000, 25000, 15000, 8000};
    int[] cif_i_updated, cif_p_updated, qcif_i_updated, qcif_p_updated;
	
    public MultiplePass() {
    	cif_i_updated = new int[12];
    	cif_p_updated = new int[12];
    	qcif_i_updated = new int[12];
    	qcif_p_updated = new int[12];
    }
    
	public static int findBitCount(String filename) {
		byte[] ary = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + filename);
		return ary.length * 8;
	}
	
	// used for generating functions of threshold and qp
    public void bitCountVSFrameIndex(boolean append, String outFileName) {
        PropertyHelper ph = new PropertyHelper();
		int[] bitCount = new int[3];
		for (int i = 12; i < 15; i++) { //for frame 5, 6, 7 or frame 12, 13, 14
			ph.setProperty("frameCount", String.valueOf(i + 1));
			Encoder en = new Encoder();
			en.startEncoding();
			bitCount[i - 12] = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
					+ findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
					+ findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));;
		}
		System.out.println("---------->Cif with qp = " + ph.getProperty("quantizationPar") + ": " + bitCount[0] + ", " + bitCount[1] + ", " + bitCount[2]);
		int[] diff = new int[3];
		diff[0] = bitCount[1] - bitCount[0]; // frame 6, 13 - not scene changing
		diff[1] = bitCount[2] - bitCount[1]; // frame 7, 14 - scene changing
		generateGraph_threshold(diff, append, outFileName);
	}
    
	// used for generating functions of threshold and qp
    public void generateGraph_threshold(int[] bitCount, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		if (append == false) {
			//Write first line
			String tmp = "qp,frame13,frame14\n";
			FileHelper.writeToFile(tmp.getBytes(), outFileName);
		}
		String tmp = ph.getProperty("quantizationPar") + "," + bitCount[0] + "," + bitCount[1] + "\n";
		FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
	}
    
	public void findThresholdFunction() {
        PropertyHelper ph = new PropertyHelper();
		ph.setProperty("quantizationPar", "4");
		ph.setProperty("blkSizeI", "16");
		ph.setProperty("I_Period", "21");
		ph.setProperty("nRefFrames", "1");
		ph.setProperty("vbsEnable", "1");
		ph.setProperty("FMEEnable", "true");
		ph.setProperty("fastME", "true");
		ph.setProperty("RCflag", "0");
		boolean append = false;
		for (int qp = 0; qp < 12; qp++) {
			if (qp == 1)
				append = true;
			ph.setProperty("quantizationPar", "" + qp);
	        // generate bitcount for frame 6 and 7(scene changing) for cif
			ph.setProperty("vidName", "CIF_y_only.yuv");
			ph.setProperty("vidWidth", "352");
			ph.setProperty("vidHeight", "288");
			System.out.println("----------> working on cif with qp = " + qp);
			bitCountVSFrameIndex(append, "cif_threshold.csv");
			
			// generate bitcount for frame 6 and 7(scene changing) for qcif
			ph.setProperty("vidName", "QCIF_y_only.yuv");
			ph.setProperty("vidWidth", "176");
			ph.setProperty("vidHeight", "144");
			System.out.println("----------> working on qcif with qp = " + qp);
			bitCountVSFrameIndex(append, "qcif_threshold.csv");
			
		}
	}
	
	public static int findThreshold(int height, int qp) {
		if (height == 288)
			return CIF_THRESHOLD[qp];
		else if (height == 144)
			return QCIF_THRESHOLD[qp];
		else {
			System.out.println("Error when finding threshold.");
			System.exit(1);
		}
		return -1;
	}
	
	// find bitcount for one frame
    public static int findFrameBitcount(String mv_perFrame, int[] qtc_perFrame, int width, int height, byte[] vbs_perFrame) {
        // Entropy Encoding
        byte[] mvEncoded_perFrame = EntropyEncDec.expGolombEnc(Helper.shortArrToIntArr(FileHelper.readShortFile(System.getProperty("user.dir") + "/output/" + mv_perFrame)));
        int[] qtcDiagReorded_perFrame = EntropyEncDec.diagReorder(qtc_perFrame, width, height);
        int[] qtcRle_perFrame = EntropyEncDec.runLevelEnc(qtcDiagReorded_perFrame);
        byte[] qtcEncoded_perFrame = EntropyEncDec.expGolombEnc(qtcRle_perFrame);
        byte[] vbsEncoded_perFrame = EntropyEncDec.expGolombEnc(vbs_perFrame);

        // finds bitcounts
        int mvBitcount_perFrame = mvEncoded_perFrame.length*8;
        int qtcBitcount_perFrame = qtcEncoded_perFrame.length*8;
        int vbsBitcount_perFrame = vbsEncoded_perFrame.length*8;

		return mvBitcount_perFrame + qtcBitcount_perFrame + vbsBitcount_perFrame;
    }
    
    // calculate proportion by rowBitCount / sum and store in rowBitCount2
    public static void setrowBitCount2(int frameIndex, int[][] rowBitCount1, double[][] rowBitCount2) {
    	int n = rowBitCount1[frameIndex].length, sum = 0;
    	for (int i = 0; i < n; i++) 
    		sum += rowBitCount1[frameIndex][i];
    	for (int i = 0; i < n; i++) {
    		rowBitCount2[frameIndex][i] = (double)rowBitCount1[frameIndex][i] / sum;
    	}
    }
    
    // find bitcount after first pass.
    public static int findAvgRowBitcount(int[][] rowBitcount) {
    	/*
    	PropertyHelper ph = new PropertyHelper();
		int bitcount = findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
				 + findBitCount(ph.getProperty("qtcEntropyEncodedFileName"));
		if (VariableBlockSize.isVBSEnabled()) {
			bitcount += findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
		}
    	int n = rowBitcount.length;
    	int m = rowBitcount[0].length;
    	return (int)bitcount / (n * m);
    	*/
    	int n = rowBitcount.length;
    	int m = rowBitcount[0].length;
    	int sum = 0;
    	for (int i = 0; i < n; i++) {
    		for (int j = 0; j < m; j++)
    			sum += rowBitcount[i][j];
    	}
    	return (int)sum / (n * m);
    }
    
    // update table based on the average bit count per row from the first pass
    public void updataTable(int height, int avgBitcount, int qp) {
    	if (height >= 288) {
    		double scale = (double)avgBitcount / (double)RateControl.CIF_P_TABLE[qp];
    		for (int i = 0; i < cif_i_updated.length; i++) {
    			cif_i_updated[i] = (int) (RateControl.CIF_I_TABLE[i]  * scale);
    			cif_p_updated[i] = (int) (RateControl.CIF_P_TABLE[i]  * scale);
    		}
    	}
    	else if (height >= 144) {
    		double scale = (double)avgBitcount / (double)RateControl.QCIF_P_TABLE[qp];
    		System.out.println(scale);
    		for (int i = 0; i < qcif_i_updated.length; i++) {
    			qcif_i_updated[i] = (int) (RateControl.QCIF_I_TABLE[i]  * scale);
    			qcif_p_updated[i] = (int) (RateControl.QCIF_P_TABLE[i]  * scale);
    		}
    	}
    	else {
    		System.out.println("error when updating tables");
    		System.exit(1);
    	}
    }
    
    public static int getRowBitBudget(int remainingBudgetPerFrame, int frameIndex, double[][] rowBitproportion, int rowNumber) {
        return (int)((double)remainingBudgetPerFrame * rowBitproportion[frameIndex][rowNumber]);
    }
    
    public static double[][] updateProportion(int frameIndex, double[][] rowBitproportion, int rowIndex) {
    	PropertyHelper ph = new PropertyHelper();
    	int totalRowNum = Integer.valueOf(ph.getProperty("vidHeight"))
    			/ Integer.valueOf(ph.getProperty("blkSizeI"));
    	double temp = rowBitproportion[frameIndex][rowIndex];
    	rowBitproportion[frameIndex][rowIndex] = 0;
    	int remainingRowNum = totalRowNum - rowIndex + 1;
    	temp /= remainingRowNum;
    	for (int i = rowIndex + 1; i < totalRowNum; i++) {
    		rowBitproportion[frameIndex][i] += temp;
    	}
    	return rowBitproportion;
    }
    
    public int findQP(double bitBudget_perRow, int height, boolean I_Frame) {
        int[] table = null;
        int QP = 0;
        if (height == 288) { // uses the table for CIF
            if (I_Frame) table = cif_i_updated;
            else table = cif_p_updated; 
        }
        else if (height == 144) { // uses the table for QCIF
            if (I_Frame) table = qcif_i_updated;
            else table = qcif_p_updated;
        }

        
        for (int i = 0; i < table.length; i++) {
            QP = i;
            if (table[i] < bitBudget_perRow) break; // finds QP that will not exceed the target bit budget
        }
        return QP;
    }
    
    // method for pass 3
    public static int[] setPrevFrameMVnotSplit(int[] mv, int[] prevFrameMVnotSplit, int y, int x, int blockSize, int width) {
    	int index = ((y / blockSize) * (width / blockSize) + (x / blockSize)) * 3;
    	prevFrameMVnotSplit[index] = mv[0];
    	prevFrameMVnotSplit[index + 1] = mv[1];
    	prevFrameMVnotSplit[index + 2] = mv[2];
    	return prevFrameMVnotSplit;
    }
    
    // method for pass 3
    public static int[] setPrevFrameMVsplit(int[] mv, int[] prevFrameMVnotSplit, int y, int x, int blockSize, int width) {
    	int index = ((y / blockSize) * (width / blockSize) + (x / blockSize)) * 12;
    	for (int i = 0; i < 4; i++) {
	    	prevFrameMVnotSplit[index] = mv[0 + i * 3];
	    	prevFrameMVnotSplit[index + 1] = mv[1 + i * 3];
	    	prevFrameMVnotSplit[index + 2] = mv[2 + i * 3];
	    	index += 3;
    	}
    	return prevFrameMVnotSplit;
    }
	
	public static void main(String[] args) {
		//MultiplePass mp = new MultiplePass();
		//mp.findThresholdFunction();
	}
}

package Assignment3;

import java.io.File;

public class RateControl {
    /////////////////////////////////// Experimentally Obtained Bitcounts ///////////////////////////////////////////
    //                            QP=  0       1      2      3     4      5     6     7     8     9   10    11
    final static int[] CIF_I_TABLE = {25688, 20875, 16062, 14125, 12188, 5196, 4146, 3247, 1744, 816, 333, 306};
    final static int[] CIF_P_TABLE = {25609, 20632, 15655, 13915, 12176, 4972, 3979, 3674, 2096, 1229,712, 335};
    final static int[] QCIF_I_TABLE = {14070,11637, 9205,  8326,  7447,  3728, 3009, 2446, 1355, 625, 208, 179};
    final static int[] QCIF_P_TABLE = {14053,11554, 9056,  8306,  7556,  3586, 2952, 2631, 1443, 863, 340, 194};
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int getBitBudget_perRow() { // finds bit budget per row based on fps and bitrate
        PropertyHelper ph = new PropertyHelper();
        int targetBR = Integer.valueOf(ph.getProperty("targetBR"));
        int blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
        int height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
        int FPS = 30;
        int bitBudget_perRow = (int) ((double)targetBR/FPS)/(height/blockSize);
        return bitBudget_perRow;
    }

    public static int getBitBudget_perFrame() { // finds bit budget per frame based on fps and bitrate
        PropertyHelper ph = new PropertyHelper();
        int targetBR = Integer.valueOf(ph.getProperty("targetBR"));
        int FPS = 30;
        int bitBudget_perFrame = targetBR/FPS;
        return bitBudget_perFrame;
    }

    /**
     * finds the QP value to use in order to meet the target per-row bit-budget
     * @param bitBudget_perRow target bit budget per row
     * @param height height of the video (to determine if the file is QCIF or CIF)
     * @param I_Frame if true, use the I Frame QP table
     * @return QP value
     */
    public static int findQP(double bitBudget_perRow, int height, boolean I_Frame) {
        int[] table = null;
        int QP = 0;
        if (height == 288) { // uses the table for CIF
            if (I_Frame) table = CIF_I_TABLE;
            else table = CIF_P_TABLE; 
        }
        else if (height == 144) { // uses the table for QCIF
            if (I_Frame) table = QCIF_I_TABLE;
            else table = QCIF_P_TABLE;
        }

        for (int i = 0; i < table.length; i++) {
            QP = i;
            if (table[i] < bitBudget_perRow) break; // finds QP that will not exceed the target bit budget
        }
        return QP;
    }

    /**
     * 
     * @param mv_perRow temporary file for storing mv/mode
     * @param qtc_perRow qtc values per row
     * @param y row index to find current row number
     * @param width width of the file
     * @param blockSize size of block
     * @param vbs_perRow vbs values per row
     * @return
     */
    public static int getActualBitSpent(String mv_perRow, int[] qtc_perRow, int y, int width, int blockSize, byte[] vbs_perRow) {
        // Entropy Encoding
        byte[] mvEncoded_perRow = EntropyEncDec.expGolombEnc(Helper.shortArrToIntArr(FileHelper.readShortFile(System.getProperty("user.dir") + "/output/" + mv_perRow)));
        int[] qtcDiagReorded_perRow = EntropyEncDec.diagReorder(qtc_perRow, width, blockSize);
        int[] qtcRle_perRow = EntropyEncDec.runLevelEnc(qtcDiagReorded_perRow);
        byte[] qtcEncoded_perRow = EntropyEncDec.expGolombEnc(qtcRle_perRow);
        byte[] vbsEncoded_perRow = EntropyEncDec.expGolombEnc(vbs_perRow);

        // finds bitcounts
        int mvBitcount_perRow = mvEncoded_perRow.length*8;
        int qtcBitcount_perRow = qtcEncoded_perRow.length*8;
        int vbsBitcount_perRow = vbsEncoded_perRow.length*8;
        int totalBitcount_perRow = mvBitcount_perRow + qtcBitcount_perRow + vbsBitcount_perRow;

        // System.out.println(" Bitcount of Row " + y/blockSize + "-> QTC: " + qtcBitcount_perRow +" MV: "+ mvBitcount_perRow +" VBS: "+vbsBitcount_perRow + " Total: " + totalBitcount_perRow);
		return totalBitcount_perRow;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // finds qp table

    public void generateTalbe2(boolean Iframe, int bitCount, String outFileName, int QP) { // testing
        PropertyHelper ph = new PropertyHelper();
        String tmp;
        if (Iframe) tmp = ph.getProperty("vidName") + " - I Frames" + " QP" + String.valueOf(QP) + ",";
        else tmp = ph.getProperty("vidName") + " - P Frames" + " QP" + String.valueOf(QP) + ",";
        FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
        
        
        tmp = Integer.toString(bitCount) + "\n";
        FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
    }

    public void generateTalbe(boolean append, boolean Iframe, int[] bitCount, String outFileName) {
        if (!append) FileHelper.writeToFile("NEW FILE\n".getBytes(), outFileName);
        PropertyHelper ph = new PropertyHelper();
        String tmp;
        if (Iframe) tmp = ph.getProperty("vidName") + " - I Frames" + "\nQP Value, 0,1,2,3,4,5,6,7,8,9,10,11\nAvg bitcount per row,";
        else tmp = ph.getProperty("vidName") + " - P Frames" +"\nQP Value, 0,1,2,3,4,5,6,7,8,9,10,11\nAvg bitcount per row,";
        FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
        
        for (int qp = 0; qp < 12; qp++) {
            tmp = Integer.toString(bitCount[qp]) + ",";
			FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
        }
        tmp = "\n\n";
        FileHelper.writeToFileAppend(tmp.getBytes(), outFileName);
    }

    public int findBitCount(String filename) {
		byte[] ary = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + filename);
		return ary.length * 8;
	}
    
    /**
     * finds average bit count per row when encoded with given fixed QP
     */
    public int getAvgBitcountRow(boolean Iframe, int QP_Value) {
        PropertyHelper ph = new PropertyHelper();
        int frameLength = 21;
		int blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
        int height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
        ph.setProperty("quantizationPar", String.valueOf(QP_Value));
        int avgBitCount_perRow;

        if (Iframe) {
            ph.setProperty("I_Period", "1");
            ph.setProperty("frameCount", String.valueOf(frameLength));
            Encoder en = new Encoder();
            en.startEncoding();
            int totalBitCount = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
                              + findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
                              + findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
            int numbRow = (height/blockSize) * frameLength;
            avgBitCount_perRow = totalBitCount / numbRow;
        }
        else {
            ph.setProperty("I_Period", "21"); // I_Period = 21 for exercise 1
            ph.setProperty("frameCount", "1");
            Encoder en = new Encoder();
            en.startEncoding();
			int I_frameBitCount = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
						        + findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
                                + findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
            
            ph.setProperty("frameCount", String.valueOf(frameLength));
            Encoder en2 = new Encoder();
            en2.startEncoding();
            int totalBitCount = findBitCount(ph.getProperty("qtcEntropyEncodedFileName"))
                              + findBitCount(ph.getProperty("mvEntropyEncodedFileName"))
                              + findBitCount(ph.getProperty("vbsEntropyEncodedFileName"));
            
            int numbRow = (height/blockSize) * (frameLength-1);
            avgBitCount_perRow = (totalBitCount - I_frameBitCount) / numbRow;
        }

        return avgBitCount_perRow;
    }
    
    public void generateAvgBitcountTable_providedSequences() {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "true");
        ph.setProperty("rangeR", "4");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("frameCount", "21");
        ph.setProperty("RCflag", "0");
        String outFileName = "RC Table.csv";

        // CIF //////////////////////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "CIF_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");

        int[] CIF_I_Frame_Bitcounts = new int[12]; // for QP = 0 to 11 
        int[] CIF_P_Frame_Bitcounts = new int[12];
        for (int QP_Value = 0; QP_Value < 12; QP_Value++) {
            CIF_I_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(true, QP_Value); // avg bitcount per row for I frames
            generateTalbe2(true, CIF_I_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> CIF I frame at QP "+QP_Value+" Done");

            CIF_P_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(false, QP_Value); // avg bitcount per row for P frames
            generateTalbe2(false, CIF_P_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> CIF P frame at QP "+QP_Value+" Done");
        }

        generateTalbe(false, true, CIF_I_Frame_Bitcounts, outFileName);
        generateTalbe(true, false, CIF_P_Frame_Bitcounts, outFileName);

        // QCIF //////////////////////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "QCIF_y_only.yuv");
		ph.setProperty("vidWidth", "176");
		ph.setProperty("vidHeight", "144");

        int[] QCIF_I_Frame_Bitcounts = new int[12]; // for QP = 0 to 11 
        int[] QCIF_P_Frame_Bitcounts = new int[12];
        for (int QP_Value = 0; QP_Value < 12; QP_Value++) {
            QCIF_I_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(true, QP_Value); // avg bitcount per row for I frames
            generateTalbe2(true, QCIF_I_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> QCIF I frame at QP "+QP_Value+" Done");

            QCIF_P_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(false, QP_Value); // avg bitcount per row for P frames
            generateTalbe2(false, QCIF_P_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> QCIF P frame at QP "+QP_Value+" Done");
        }

        generateTalbe(true, true, QCIF_I_Frame_Bitcounts, outFileName);
        generateTalbe(true, false, QCIF_P_Frame_Bitcounts, outFileName);
    }

    public void generateAvgBitcountTable_foremanSequences() {
        PropertyHelper ph = new PropertyHelper();
        ph.setProperty("nRefFrames", "1");
        ph.setProperty("vbsEnable", "1");
        ph.setProperty("FMEEnable", "true");
        ph.setProperty("fastME", "true");
        ph.setProperty("rangeR", "16");
        ph.setProperty("blkSizeI", "16");
        ph.setProperty("frameCount", "21");
        ph.setProperty("RCflag", "0");
        String outFileName = "RC Table_foreman.csv";

        // CIF //////////////////////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "foreman_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");

        int[] CIF_I_Frame_Bitcounts = new int[12]; // for QP = 0 to 11 
        int[] CIF_P_Frame_Bitcounts = new int[12];
        for (int QP_Value = 0; QP_Value < 12; QP_Value++) {
            CIF_I_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(true, QP_Value); // avg bitcount per row for I frames
            generateTalbe2(true, CIF_I_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> CIF I frame at QP "+QP_Value+" Done");

            CIF_P_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(false, QP_Value); // avg bitcount per row for P frames
            generateTalbe2(false, CIF_P_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> CIF P frame at QP "+QP_Value+" Done");
        }

        generateTalbe(false, true, CIF_I_Frame_Bitcounts, outFileName);
        generateTalbe(true, false, CIF_P_Frame_Bitcounts, outFileName);

        // QCIF //////////////////////////////////////////////////////////////////////////////////////////////////////
        ph.setProperty("vidName", "foreman_qcif_y_only.yuv");
		ph.setProperty("vidWidth", "176");
		ph.setProperty("vidHeight", "144");

        int[] QCIF_I_Frame_Bitcounts = new int[12]; // for QP = 0 to 11 
        int[] QCIF_P_Frame_Bitcounts = new int[12];
        for (int QP_Value = 0; QP_Value < 12; QP_Value++) {
            QCIF_I_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(true, QP_Value); // avg bitcount per row for I frames
            generateTalbe2(true, QCIF_I_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> QCIF I frame at QP "+QP_Value+" Done");

            QCIF_P_Frame_Bitcounts[QP_Value] = getAvgBitcountRow(false, QP_Value); // avg bitcount per row for P frames
            generateTalbe2(false, QCIF_P_Frame_Bitcounts[QP_Value], "test.csv", QP_Value); //testing
            System.out.println("----------> QCIF P frame at QP "+QP_Value+" Done");
        }

        generateTalbe(true, true, QCIF_I_Frame_Bitcounts, outFileName);
        generateTalbe(true, false, QCIF_P_Frame_Bitcounts, outFileName);
    }

    public static void main(String[]args) {
        RateControl rc = new RateControl();
        // rc.generateAvgBitcountTable_providedSequences(); // finds avg bit count table for provided CIF and QCIF files
        // rc.generateAvgBitcountTable_foremanSequences(); // finds avg bit count table for foreman cif and qcif sized files
        
    }
}
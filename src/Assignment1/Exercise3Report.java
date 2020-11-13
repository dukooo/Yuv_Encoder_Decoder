package Assignment1;

import java.time.ZonedDateTime;

public class Exercise3Report {

	int frameNum;
	int vidWidth;
	int vidHeight;
	public Exercise3Report() {
		PropertyHelper ph = new PropertyHelper();
		this.frameNum = 10;

		this.vidWidth = Integer.valueOf(ph.getProperty("vidWidth"));
		this.vidHeight = Integer.valueOf(ph.getProperty("vidHeight"));

	}

	/**
	 * Calculate SAD for each frame. Basically adding up the difference between each pixel.
	 * @param frame1
	 * @param frame2
	 * @return Integer SAD.
	 */
	public int getPerFrameSAD(byte [] frame1, byte[] frame2) {

		int sad = 0;
		for (int i = 0; i < frame1.length; i++) {
			sad += Math.abs(EncoderExercise3.unsignedByteToInt(frame1[i]) - EncoderExercise3.unsignedByteToInt(frame2[i]));
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

		int [] ret = new int[this.frameNum];
		for (int i = 0; i < this.frameNum; i++) {
			// for each frame, calculate 1 SAD

			// Copy just 1 frame for calculation
			byte[] frame1 = new byte[this.vidHeight*this.vidWidth];
			byte[] frame2 = new byte[this.vidHeight*this.vidWidth];
			for (int j = 0; j < this.vidHeight*this.vidWidth; j++) {
				frame1[j] = vid1[i*this.vidHeight*this.vidWidth + j];
				frame2[j] = vid2[i*this.vidHeight*this.vidWidth + j];
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

	public void calcResMagnitude() {
		PropertyHelper ph = new PropertyHelper();
		int maxRes = Integer.MIN_VALUE;
		int minRes = Integer.MAX_VALUE;

		byte[] res = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("residualFileName"));
		for (int i=0; i < res.length; i++) {
			maxRes = Math.max(maxRes, (int)res[i]);
			minRes = Math.min(minRes, (int)res[i]);
		}
		System.out.println("Maximum Residual = " + maxRes + "Minimum Residual = " + minRes);
 	}

	public void SADVaryingI(int blockSize, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		byte[] original_y_only_yuv;
		byte[] decoded_y_only_yuv;
		EncoderExercise3 en;
		DecoderExercise3 dec;
		int [] sads;


		ph.setProperty("blkSizeI", Integer.toString(blockSize));
		// enc and dec
		en = new EncoderExercise3();
		// also calculate encoding time.
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("For blockSize i = " + blockSize + " Encoding time (ms) = " + (endTimeMs - startTimeMs));
		calcResMagnitude(); // assuming *.arb is already generated.

		dec = new DecoderExercise3();
		dec.startDecoding();
		// read files
		original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		//gen SADs
		sads = getSADForAllFrames(original_y_only_yuv, Helper.stripPadding(Integer.valueOf(ph.getProperty("vidWidth")),
				Integer.valueOf(ph.getProperty("vidHeight")), blockSize, Integer.valueOf(ph.getProperty("frameCount")), decoded_y_only_yuv));
		genSADGraph(outFileName, sads, append);


	}


	public void SADVaryingR(int r, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		byte[] original_y_only_yuv;
		byte[] decoded_y_only_yuv;
		EncoderExercise3 en;
		DecoderExercise3 dec;
		int [] sads;

		ph.setProperty("rangeR", Integer.toString(r));
		// enc and dec
		en = new EncoderExercise3();
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		en.startEncoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("For r = " + r + " Encoding time (ms) = " + (endTimeMs - startTimeMs));
		calcResMagnitude(); // assuming *.arb is already generated.




		dec = new DecoderExercise3();
		dec.startDecoding();
		// read files
		original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		//gen SADs
		sads = getSADForAllFrames(original_y_only_yuv, decoded_y_only_yuv);
		genSADGraph(outFileName, sads, append);
	}

	public void SADVaryingN(int n, boolean append, String outFileName) {
		PropertyHelper ph = new PropertyHelper();
		byte[] original_y_only_yuv;
		byte[] decoded_y_only_yuv;
		EncoderExercise3 en;
		DecoderExercise3 dec;
		int [] sads;

		ph.setProperty("multipleN", Integer.toString(n));
		// enc and dec
		en = new EncoderExercise3();
		en.startEncoding();
		dec = new DecoderExercise3();
		dec.startDecoding();
		// read files
		original_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName"));
		decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		//gen SADs
		sads = getSADForAllFrames(original_y_only_yuv, decoded_y_only_yuv);
		genSADGraph(outFileName, sads, append);
	}


	public void getFirstTenFrame(int vidWidth, int vidHeight, int blockSize) {

		PropertyHelper ph = new PropertyHelper();
		EncoderExercise3 en;
		DecoderExercise3 dec;
		// enc and dec
		en = new EncoderExercise3();
		en.startEncoding();
		dec = new DecoderExercise3();
		dec.startDecoding();

		// get first 10 frame of decoded
		byte[] decoded_y_only_yuv = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("decReconstructedFileName"));
		byte[] first_ten_decoded_y_only_yuv = new byte[vidWidth * vidHeight * 10]; //Store first 10 frames
		// get the first 10 frame and write to output
		for (int i = 0; i < first_ten_decoded_y_only_yuv.length; i++) {
			first_ten_decoded_y_only_yuv[i] = decoded_y_only_yuv[i];
		}
		FileHelper.writeToFile(first_ten_decoded_y_only_yuv, "10_frame_" + ph.getProperty("decReconstructedFileName"));

		// need to do the same for motion vectors
		byte[] mv = FileHelper.readByteFile((System.getProperty("user.dir") + "/output/" + ph.getProperty("mvFileName")));

		int numBlocksPerFrame = (Helper.getHeightAfterPadding(vidHeight, blockSize) / blockSize) * (Helper.getWidthAfterPadding(vidWidth, blockSize) / blockSize);
		byte[] first_ten_mv = new byte[numBlocksPerFrame * 2 * 10]; // Every block has 2 motion vectors, x and y. And we need 10 frames.

		for (int i = 0; i < first_ten_mv.length; i++) {
			first_ten_mv[i] = mv[i];
		}
		FileHelper.writeToFile(first_ten_mv, "10_frame_" + ph.getProperty("mvFileName"));

	}

	public static void main(String[] args) {

		System.out.println("Generating Report Data");


		Exercise3Report ex3rep = new Exercise3Report();
		PropertyHelper ph = new PropertyHelper();

		// first one is for foreman cif
		ph.setProperty("vidName", "foreman_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
		// first SAD graph should be fixed r=4, n=3, varying i
		ph.setProperty("rangeR", "4");
		ph.setProperty("multipleN", "3");
		ex3rep.SADVaryingI(2, false, "foreman_cif_varying_i.csv");
		ex3rep.SADVaryingI(8, true, "foreman_cif_varying_i.csv");
		ex3rep.SADVaryingI(64, true, "foreman_cif_varying_i.csv");

		// second graph = fixed i=8, n=3, varying r
		ph.setProperty("blkSizeI", "8");
		ph.setProperty("multipleN", "3");
		ex3rep.SADVaryingR(1, false, "foreman_cif_varying_r.csv");
		ex3rep.SADVaryingR(4, true, "foreman_cif_varying_r.csv");
		ex3rep.SADVaryingR(8, true, "foreman_cif_varying_r.csv");
//
//		// third graph graph = fixed i=8, r=4, varying n
//		ph.setProperty("blkSizeI", "8");
//		ph.setProperty("rangeR", "4");
//		ex3rep.SADVaryingN(1, false, "foreman_cif_varying_n.csv");
//		ex3rep.SADVaryingN(2, true, "foreman_cif_varying_n.csv");
//		ex3rep.SADVaryingN(3, true, "foreman_cif_varying_n.csv");
//
//		//////////////////////////////////////////////////////////////////////////////
//
//		// now do akiyo qcif
//		ph.setProperty("vidName", "akiyo_qcif_y_only.yuv");
//		ph.setProperty("vidWidth", "176");
//		ph.setProperty("vidHeight", "144");
//		// first SAD graph should be fixed r=4, n=3, varying i
//		ph.setProperty("rangeR", "4");
//		ph.setProperty("multipleN", "3");
//		ex3rep.SADVaryingI(2, false, "akiyo_qcif_varying_i.csv");
//		ex3rep.SADVaryingI(8, true, "akiyo_qcif_varying_i.csv");
//		ex3rep.SADVaryingI(64, true, "akiyo_qcif_varying_i.csv");
//
//		// second graph = fixed i=8, n=3, varying r
//		ph.setProperty("blkSizeI", "8");
//		ph.setProperty("multipleN", "3");
//		ex3rep.SADVaryingR(1, false, "akiyo_qcif_varying_r.csv");
//		ex3rep.SADVaryingR(4, true, "akiyo_qcif_varying_r.csv");
//		ex3rep.SADVaryingR(8, true, "akiyo_qcif_varying_r.csv");
//
//		// third graph graph = fixed i=8, r=4, varying n
//		ph.setProperty("blkSizeI", "8");
//		ph.setProperty("rangeR", "4");
//		ex3rep.SADVaryingN(1, false, "akiyo_qcif_varying_n.csv");
//		ex3rep.SADVaryingN(2, true, "akiyo_qcif_varying_n.csv");
//		ex3rep.SADVaryingN(3, true, "akiyo_qcif_varying_n.csv");
//
//		////////////////////////////////////////////////////////////////////////////////
//
//		//Need to do get first 10 frames of everything for foreman_cif
//		ph.setProperty("vidName", "foreman_cif_y_only.yuv");
//		ph.setProperty("vidWidth", "352");
//		ph.setProperty("vidHeight", "288");
//		ph.setProperty("rangeR", "4");
//		ph.setProperty("multipleN", "3");
//		ph.setProperty("blkSizeI", "8");
//		ex3rep.getFirstTenFrame(352, 288, 8);

		////////////////////////////////////////////////////////////////////////////////

		// For 2 I cases, obtain residual before, after, and predictor frame
		ph.setProperty("vidName", "foreman_cif_y_only.yuv");
		ph.setProperty("vidWidth", "352");
		ph.setProperty("vidHeight", "288");
		ph.setProperty("rangeR", "4");
		ph.setProperty("multipleN", "1");
		ph.setProperty("blkSizeI", "2");
		ex3rep.getFirstTenFrame(352, 288, 8);





		// Calculate residual magnitude and encoding time


		System.out.println("Finished Generating Report Data");

    }
}

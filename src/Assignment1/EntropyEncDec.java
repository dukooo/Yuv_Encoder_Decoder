package Assignment1;

import java.util.ArrayList;

public class EntropyEncDec {
	
	/**
	 * README:
	 * 
	 * There are 2 flows for entropy encoding & decoding. 1 flow for MV, 1 flow for QTC (quantized transformed coefficients).
	 * 
	 * For Motion/Mode vector values, we only need to go through expGolombEnc:
	 * 1. After encoding is done for all frames, encoder will perform expGolombEnc and write
	 * output to "mvEntropyEncodedFileName"
	 * 2. Decoder can do the following to get the bytes back:
	 * 
	 * byte[] inputFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvEntropyEncodedFileName"));
	   byte[] decoded = Helper.intArrToByteArr(EntropyEncDec.expGolombDec(inputFile));
	 * 
	 * For Quantized Coefficients, we need to go through 3 stage process, RLE and then expGolomb:
	 * 1. Per frame operation is needed for Diag Re-Ordering. This is done by Encoder.
	 * 2. Encoder will produce "qtcDiagOrderedFileName".
	 * 3. After encoding is done for all frames, encoder will perform RLE (run-level encoding) AND exp-Golomb encoding and write "qtcEntropyEncodedFileName".
	 * 4. Decoder can do the following to get the bytes back:
	 * ASSUMING -> vidWidth = 352, vidHeight = 288, frameCnt = 3
	 
	 * byte[] inputFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("qtcEntropyEncodedFileName"));
    	int[] expGolDec = EntropyEncDec.expGolombDec(inputFile);
    	int[] rleDec = EntropyEncDec.runLevelDec(expGolDec, 352, 288, 3);
    	//for each frame, reverse the diagonal order
    	for (int i = 0; i < 3; i++) {
    		int[] oneFrame = new int[352*288];
    		
    		for (int j = 0; j < oneFrame.length; j++) {
    			oneFrame[j] = rleDec[i*352*288 + j];
    		}
    		// You get 1 frame of QTC values
    		int[] qtc = EntropyEncDec.diagReorderReverse(oneFrame, 352, 288);
    	}
	 * 
	 */

	public static boolean encodingCalculated = false;
	public static String [] encoding;

	
	public EntropyEncDec () {
		return;
	}
	
	public static void _populateExpGolombEncoding () {
		// 16 bit signed short has a max capacity of -32768  to 32767, compute all of them ahead of time.
		encoding = new String[Math.abs(Short.MIN_VALUE)*2 +1];
		for (int i = Short.MIN_VALUE; i <=Short.MAX_VALUE; i++) {
			encoding[i+Math.abs(Short.MIN_VALUE)] = expGolombEncInt(i);
		}
		encodingCalculated=true;
	}
	
	public static String getExpGolombEncoding(int a) {
		if (encodingCalculated == false) {
			_populateExpGolombEncoding();
		}
		
		// if we have pre-calculated value, serve that back
		// else we calculate on the spot
		if (a + Math.abs(Short.MIN_VALUE) < encoding.length)
			return encoding[a+Math.abs(Short.MIN_VALUE)];
		else
			return expGolombEncInt(a);
	}
	
	/**
	 * TODO: Everything here can be optimized. We can have a look up table that contains 
	 * encoding for [-128..128] which is enough for our purpose. Encoding and decoding will be O(1) constant time.
	 */
	/**
	 * Encode a single input integer using exp-golomb as described here 
	 * https://en.wikipedia.org/wiki/Exponential-Golomb_coding#Extension_to_negative_numbers
	 * @param a, one integer
	 * @return String representation of encoded input a
	 */
	public static String expGolombEncInt(int a) {
		int enc;
		String ret = "";
		if (a <= 0) {
			enc = (-2) * a;
		}
		else {
			enc = 2*a -1;
		}
		String binStr = Integer.toBinaryString(enc+1);
		int numZerosNeeded = binStr.length() - 1;
		

		ret += Helper.zeroPadding[numZerosNeeded];
		
		ret += binStr;
		return ret;
	}
	
	/**
	 * Encode an array of integers using exp-golomb as described here 
	 * https://en.wikipedia.org/wiki/Exponential-Golomb_coding#Extension_to_negative_numbers
	 * @param a, integer array to be encoded
	 * @return byte[] representation of encoded input a
	 */
	public static byte [] expGolombEnc(int[] a) {

		StringBuilder sb = new StringBuilder();
		for (int i =0; i < a.length; i++) {
			sb.append(getExpGolombEncoding((int)a[i]));
		}
		//System.out.println(sb.toString());
		return Helper.bitStrToByteArr(sb.toString());
	}
	
	/**
	 * Encode an array of integers using exp-golomb as described here 
	 * https://en.wikipedia.org/wiki/Exponential-Golomb_coding#Extension_to_negative_numbers
	 * @param a, short (16bit signed) array to be encoded
	 * @return byte[] representation of encoded input a, ready to be written to output.
	 */
	public static byte [] expGolombEnc(short[] a) {
		StringBuilder sb = new StringBuilder();

		for (int i =0; i < a.length; i++) {
			sb.append(getExpGolombEncoding((int)a[i]));
		}
		//System.out.println(sb.toString());
		return Helper.bitStrToByteArr(sb.toString());
	}
	
	/**
	 * Get bit count for encoding of an array of integers using exp-golomb as described here 
	 * https://en.wikipedia.org/wiki/Exponential-Golomb_coding#Extension_to_negative_numbers
	 * @param a, short (16bit signed) array to be encoded
	 * @return int, bit count of encoded content
	 */
	public static int expGolombEncBitCount(short[] a) {
		StringBuilder sb = new StringBuilder();

		for (int i =0; i < a.length; i++) {
			sb.append(getExpGolombEncoding((int)a[i]));
		}
		
		return sb.length();
	}
	
	
	/**
	 * Encode an array of signed byte values using exp-golomb as described here 
	 * https://en.wikipedia.org/wiki/Exponential-Golomb_coding#Extension_to_negative_numbers
	 * @param a, byte array to be encoded
	 * @return byte[] representation of encoded input a
	 */
	public static byte [] expGolombEnc(byte[] a) {

		StringBuilder sb = new StringBuilder();
		for (int i =0; i < a.length; i++) {
			sb.append(getExpGolombEncoding((int)a[i]));
		}
//		System.out.println(sb.toString());
		return Helper.bitStrToByteArr(sb.toString());
	}
	
	/**
	 * exp-Golomb Dec, the reverse process of exp-golomb enc function above.
	 * @param a, byte array of a previously exp-golomb encoded array.
	 * @return int[] of decoded values, which will have length >= a.length
	 */
	public static int[] expGolombDec(byte[] a) {
		
		// need a temporary arraylist because we don't know how many integers we will end up getting from string
		ArrayList<Integer> temp = new ArrayList<Integer>();
		String aStr = Helper.byteArrToBitString(a);
//		System.out.println(aStr);
		int ind = 0;
		int zeroCnt = 0;
		while(ind < aStr.length()) {
			if (aStr.charAt(ind) == '0') {
				// accumulate and count
				zeroCnt += 1;
				ind += 1;
			}
			else {
				String tempStr = aStr.substring(ind, ind+zeroCnt+1);
//				System.out.println(tempStr);
				temp.add(Integer.parseUnsignedInt(tempStr, 2)-1); // binary is always x+1
//				System.out.println(temp.get(temp.size()-1));
				ind += zeroCnt+1;
				zeroCnt = 0;
			}
		}
		
		// now need to convert negatives to correct values
		int [] ret = new int[temp.size()];
		for (int i =0; i < temp.size(); i++) {
			// even ones were negative originally
			// odd ones were positive originally
			if (temp.get(i) % 2 == 0) {
				ret[i] = temp.get(i) / (-2);
			}
			else {
				ret[i] = (temp.get(i) + 1)/2;
			}
		}
		return ret;
		
	}
	
	public static int[] diagReorder(int [] in, int width, int height) {
		int[] out = new int[in.length];
		int copyCnt = 0, col = 0, row = 0, i =0;
		
		while(i < width) {
			col = i;
			row = 0;
			while (col >= 0 && row < height) {
				out[copyCnt] = in[row * width + col];
				copyCnt += 1;
				row += 1;
				col -= 1;
			}
			i+=1;
		}
		
		i = 1;
		while (i < height) {
			col = width - 1;
			row = i;
			while (row < height && col >=0) {
				out[copyCnt] = in[row * width + col];
				copyCnt += 1;
				row += 1;
				col -= 1;
			}
			i+=1;
		}
		return out;
		
	}
	
	/**
	 * Revert diagonally ordered integer array of values. 
	 * @param in, diagonally order integer array of values.
	 * @param width, video width
	 * @param height, video height
	 * @return raster ordered integer array of values.
	 */
	public static int[] diagReorderReverse(int [] in, int width, int height) {
		int [] out = new int[in.length];
		int copyCnt = 0, col = 0, row = 0, i =0;
		
		while(i < width) {
			col = i;
			row = 0;
			while (col >= 0 && row < height) {
				out[row * width + col] = in[copyCnt];
				copyCnt += 1;
				row += 1;
				col -= 1;
			}
			i+=1;
		}
		
		i = 1;
		while (i < height) {
			col = width - 1;
			row = i;
			while (row < height && col >=0) {
				out[row * width + col] = in[copyCnt];
				copyCnt += 1;
				row += 1;
				col -= 1;
			}
			i+=1;
		}
		return out;
	}
	/**
	 * Helper function. Check if there are 3 0s start from start Ind. 
	 * DO NOT CALL OUTSIDE OF THIS FILE
	 * @param in
	 * @param startInd
	 * @return
	 */
	public static boolean _runLevelEncCheckThreeZeros(int [] in, int startInd) {
	
		if (startInd < in.length && in[startInd] == 0 && (startInd+1) <in.length && 
				in[startInd+1] == 0 && (startInd+2) < in.length &&
				in[startInd+2] == 0) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Helper function, DO NOT CALL OUTSIDE OF THIS FILE.
	 * @param in
	 * @param startInd
	 * @return
	 */
	public static int _runLevelEncLookAhead(int [] in, int startInd) {
		
		int countZeros = 0;
		int countNonZeros = 0;
		
		while (startInd < in.length) {
			
			if (in[startInd] == 0)
			{
				if (countNonZeros != 0) {
					// If there are less than 3 0s. We can optimize by counting 0 as
					// a non-zero value.
					if (_runLevelEncCheckThreeZeros(in, startInd)) {
						// there are 3 0s, no need to keep counting non-zeros.
						return (-1) * countNonZeros;
					}
					else if (startInd == in.length-1) {
						// already at the end, we don't count the 0 at the end 
						// as a non-zero value
						return (-1) * countNonZeros;
					}
					else {
						countNonZeros += 1;
					}
				}
				else {
					countZeros += 1;
				}
				
			}
			else {
				//non-zero
				if (countZeros != 0) {
					return countZeros;
				}
				else {
					countNonZeros += 1;
				}
				
			}
			startInd += 1;
		}
		if (countZeros != 0)
			return countZeros;
		return (-1) * countNonZeros;
		
	}
	
	/**
	 * Run Level Encode. 
	 * @param in, integer array of values to be encoded
	 * @return, run level encoded integer array, the returned int[] length will be <= than in.length.
	 */
	public static int[] runLevelEnc(int [] in) {
		
		// output is variable length, so need arraylist which is not fixed sized
		ArrayList<Integer> temp = new ArrayList<Integer>();
		int []ret;
		
		int i = 0;
		while (i < in.length) {
			// look ahead to see how many 0 or non 0s
			int lookAhead = _runLevelEncLookAhead(in, i);
			
			if (i+lookAhead >= in.length) {
				// everything is 0 until the end.
				temp.add(0);
				break;
			}
			
			temp.add(lookAhead);
			
			if (lookAhead < 0) {
				lookAhead = Math.abs(lookAhead);
				for (int j=0; j<lookAhead; j++) {
					temp.add(in[i+j]);
				}
			}
	
			i+=lookAhead;
		}
		
		ret = new int[temp.size()];
		for (int j=0; j<temp.size(); j++) {
			ret[j] = temp.get(j);
		}
		return ret;
	}
	
	/**
	 * Run level decode. This function will always return int[] of size 
	 * vidWidth*vidHeight*frameCnt
	 * @param in
	 * @param vidWidth
	 * @param vidHeight
	 * @param frameCnt
	 * @return int[] of run level decoded coefficients.
	 */
	public static int [] runLevelDec(int [] in, int vidWidth, int vidHeight, int frameCnt) {
		// output is variable length, so need arraylist which is not fixed sized
		ArrayList<Integer> temp = new ArrayList<Integer>();
		int[] ret;
		int i=0;
		
		while (i < in.length) {
			// treat in[i] as a prefix
			if (in[i] < 0) {
				// copy i number of elements from in
				for (int j=0; j < ((-1) * in[i]); j++) {
					temp.add(in[i+1+j]); // start copying from i+1 element
				}
				i += (-1) * in[i];
			}
			else {
				// copy i number of 0s
				for (int j=0; j < in[i]; j++) {
					temp.add(0);
				} 
				if (i == in.length-1) {
					// last digit is a 0, so we keep adding 0
					while(temp.size() < vidWidth*vidHeight*frameCnt)
						temp.add(0);
				}
			}
			i+=1;
		}
		
		ret = new int[temp.size()];
		// copy it over
		for (int j=0; j<temp.size(); j++) {
			ret[j] = temp.get(j);
		}
		
		
		return ret;
		
	}

	
    public static void main(String[] args) {
    	
    	PropertyHelper ph = new PropertyHelper();
    	System.out.println("Test Start");
    	
    	////////////////// EXP Golomb Enc Dec ///////////////////
    	// Testing every number from -128 to 128 which should be enough
    	/*
    	int [] test = new int[128*2 + 1];
    	
    	for (int i = -128; i < 129; i++) {
    		test[i+128] = i;
    	}
    	
    	int [] a = EntropyEncDec.coeffEnc(new int[] {-31,9,8,4,-4,1,4,0,-3,2,4,0,4,0,-4,0}, 4, 4);
    	for (int i = 0; i < a.length; i++) {
		System.out.println(a[i]);
    	}
    	*/
    	////////////////////////////////////////////////////////////

    	
    	
    	////////////// Diag ReOrder Enc Dec /////////////////////
//    	int [] reordered = EntropyEncDec.diagReorder(new int[] {-31,9,8,4,-4,1,4,0,-3,2,4,0,4,0,-4,0}, 4, 4);
//    	int [] reversed  = EntropyEncDec.diagReorderReverse(reordered, 4,4);
//    	for (int i = 0; i < reversed.length; i++) {
//    		System.out.println(reversed[i]);
//    	}
    	
    	/////////////////////////////////////////////////////////
    	
    	
    	
    	
  	
    	////////////// Run Level Enc Dec ////////////////////////
    	/*
    	int[] encoded = EntropyEncDec.runLevelEnc(new int[]{-31, 9, -4, 8, 1, -3, 4, 4, 2, 4, 0, 4, 0, 0, -4, 0});
    	int[] decoded = EntropyEncDec.runLevelDec(encoded, 4, 4, 1);
    	for (int i = 0; i < decoded.length; i++) {
    		System.out.println(decoded[i]);
    	}
    	*/
    	///////////////////////////////////////////////////////
    	
    	
    	
    	
    	////////////// Encode and Decode for Co-efficients ////////////////////////
    	
    	/*
    	int[] dr = EntropyEncDec.diagReorder(new int[]{-31,9,8,4,-4,1,4,0,-3,2,4,0,4,0,-4,0},4,4);
    	int[] rle = EntropyEncDec.runLevelEnc(dr);
    	byte[] encoded = EntropyEncDec.expGolombEnc(rle);
    	
    	int[] decoded = EntropyEncDec.expGolombDec(encoded);
    	int [] rleDec = EntropyEncDec.runLevelDec(decoded, 4, 4);
    	int [] drDec = EntropyEncDec.diagReorderReverse(rleDec, 4, 4, 1);
    	for (int i = 0; i < drDec.length; i++) {
    		System.out.println(drDec[i]);
    	}
    	*/
    	
    	//Full test on output of Encoder. the output "testentropycodec.qtc" should match qtc.qtc
    	
    	byte[] inputFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("qtcEntropyEncodedFileName"));
    	int[] expGolDec = EntropyEncDec.expGolombDec(inputFile);
    	int[] rleDec = EntropyEncDec.runLevelDec(expGolDec, 352, 288, 10);
    	//for each frame, reverse the diagonal order
    	for (int i = 0; i < 10; i++) {
    		int[] oneFrame = new int[352*288];
    		
    		for (int j = 0; j < oneFrame.length; j++) {
    			oneFrame[j] = rleDec[i*352*288 + j];
    		}
    		int[] qtc = EntropyEncDec.diagReorderReverse(oneFrame, 352, 288);
    		if(i == 0) {
    			FileHelper.writeToFile(Helper.intArrToShortArr((qtc)), "testentropycodec.qtc");
    		}
    		else {
    			FileHelper.writeToFileAppend(Helper.intArrToShortArr(qtc), "testentropycodec.qtc");
    		}
    	}
    	
    	
    
    	///////////////////////////////////////////////////////////////////////////
    	
    	
    	//////////////Encode and Decode for expGolomb (MV) ////////////////////////

    	
    	// Simplest test.

    	byte[] encoded = EntropyEncDec.expGolombEnc(new byte[] {1,-4,12,123,-128,0,127, -99});
    	int[] decoded = EntropyEncDec.expGolombDec(encoded);
    	for (int i = 0; i < decoded.length; i++) {
    		System.out.println((int) decoded[i]);
    	}

    	

    	// Test both encoding and decoding. Not needed because encoder will do encoding now. Run the test below.

    	/*
    	byte[] inputFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvFileName"));
    	byte[] encodedMv = EntropyEncDec.expGolombEnc(inputFile);
    	
    	byte[] decoded = Helper.intArrToByteArr(EntropyEncDec.expGolombDec(encoded));
    	FileHelper.writeToFile(decoded, "testEntropycodec.mv");
    	*/
    	
    	
    	// Full test on output from encoder. The decoded output "testEntropycodec.mv" should match original motionvector3.mv
    	
    	byte[] mvinputFile = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvEntropyEncodedFileName"));
    	   
    	byte[] mvdecoded = Helper.intArrToByteArr(EntropyEncDec.expGolombDec(mvinputFile));
    	FileHelper.writeToFile(mvdecoded, "testEntropycodec.mv");
    	
    	///////////////////////////////////////////////////////////////////////////
    	
       System.out.println("Test Complete");
    }

}

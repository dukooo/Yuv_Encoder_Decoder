package Assignment1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class Helper {

	public static String[] zeroPadding = {"", 
										  "0", 
										  "00", 
										  "000", 
										  "0000", 
										  "00000", 
										  "000000", 
										  "0000000", 
										  "00000000", 
										  "000000000", 
										  "0000000000", 
										  "00000000000",
										  "000000000000", 
										  "0000000000000", 
										  "00000000000000", 
										  "000000000000000", 
										  "0000000000000000"
										  };
	public static HashMap<String, Integer> _bitStrToIntegerMap;
	public static boolean _bitStrToIntegerMapReady = false;
    public Helper() {
       return;
    }

    /**
     * Helper function to get width of frame after applying padding on block sized blockSize x blockSize
     * @param width, original frame width without padding
     * @param blockSize, blockSize x blockSize blocks
     * @return width with padding
     */
    public static int getWidthAfterPadding(int width, int blockSize) {
        if (width % blockSize != 0) return (int)(width / blockSize + 1) * blockSize;
        else return width;
    }

    /**
     * Helper function to get height of frame after applying padding on block sized blockSize x blockSize
     * @param height, original frame height without padding
     * @param blockSize, blockSize x blockSize blocks
     * @return height with padding
     */
    public static int getHeightAfterPadding(int height, int blockSize) {
        if (height % blockSize != 0) return (int)(height / blockSize + 1) * blockSize;
        else return height;
    }

    /**
     * Helper function to turn a padded video stream into unpadded video stream of vidWidth * vidHeight
     * @param vidWidth, the expected vidWidth (should be same as original input vid)
     * @param vidHeight, the expected vidHeight (should be same as original input vid)
     * @param blockSize, the blocksize used to do padding
     * @param padded, byte [] of padded video
     * @return byte[] of unpadded video
     */
    public static byte[] stripPadding(int vidWidth, int vidHeight, int blockSize, int frameCount, byte[] padded) {
    	byte[] unpadded = new byte[vidWidth*vidHeight*frameCount];

    	for (int i=0; i < frameCount; i++) {
    		int unpaddedFrameOffset = i * vidWidth * vidHeight; // Offset into the right frame
    		int paddedFrameOffset = i * getWidthAfterPadding(vidWidth, blockSize) * getHeightAfterPadding(vidHeight, blockSize);
    		for (int row=0; row<vidHeight; row++) {
        		for (int col=0; col<vidWidth; col++) {
        			unpadded[unpaddedFrameOffset + (row*vidWidth) + col] = padded[paddedFrameOffset + (row*getWidthAfterPadding(vidWidth, blockSize)) + col];
        		}
        	}
    	}


    	return unpadded;
    }

    public static byte[] intArrToByteArr(int [] a) {
    	byte[] ret = new byte[a.length];
    	for (int i = 0; i < a.length; i++) {
    		if (a[i] < -128 || a[i] > 127) {
    			System.out.println("Number " + a[i] + " cannot be converted to byte without losing bits");
    			System.exit(1);
    		}

    		ret[i] = (byte) a[i];
    	}
    	return ret;
    }
    
    public static short[] intArrToShortArr(int [] a) {
    	short[] ret = new short[a.length];
    	for (int i = 0; i < a.length; i++) {
    		if (a[i] < -32768 || a[i] > 32767) {
    			System.out.println("Number " + a[i] + " cannot be converted to short without losing bits");
    			System.exit(1);
    		}

    		ret[i] = (short) a[i];
    	}
    	return ret;
    }

    public static int[] byteArrToIntArr(byte [] a) {
    	int[] ret = new int[a.length];
    	for (int i = 0; i < a.length; i++) {
    		ret[i] = (int) a[i];
    	}
    	return ret;
    }
    
    public static int[] shortArrToIntArr(short [] a) {
    	int[] ret = new int[a.length];
    	for (int i = 0; i < a.length; i++) {
    		ret[i] = (int) a[i];
    	}
    	return ret;
    }

    public static void _populateBitStrTable() {
    	_bitStrToIntegerMap = new HashMap<String, Integer>();
    	for (int i = 0 ; i <=255; i++) {
    		String tmp =  Integer.toBinaryString(i);
    		_bitStrToIntegerMap.put(Helper.zeroPadding[8-tmp.length()] + tmp, i);
    	}
    	_bitStrToIntegerMapReady = true;
    }
    
    /**
     * Convert a bit String of 0s and 1s into byte array
     * @param a
     * @return
     */
    public static byte[] bitStrToByteArr(String a) {
    	
    	if (_bitStrToIntegerMapReady == false)
    			_populateBitStrTable();
    	
    	byte[] ret = new byte[(int) Math.ceil((a.length()/8.0))];
    	int temp;
    	String tempStr;

    	for (int i = 0; i < a.length(); i+=8) {
    		if (i+8 <= a.length()) {
    			// have at least 8 bits
    			temp = _bitStrToIntegerMap.get(a.substring(i, i+8));
    		}
    		else {
    			tempStr = a.substring(i, a.length());
    			// Add in 0s to make it a full byte
    			// while (tempStr.length() < 8)
				tempStr += Helper.zeroPadding[8-tempStr.length()];
    			temp = Integer.parseInt(tempStr, 2);

    		}
    		ret[i / 8] = (byte) temp;
    	}
    	return ret;
    }

    /**
     * Convert a byte array into bit string of 0s and 1s
     * @param a
     * @return
     */
    public static String byteArrToBitString(byte [] a) {
    	//String ret = "";
    	StringBuilder sb = new StringBuilder();
    	String tempStr;

    	for (int i =0; i < a.length; i++) {
    		tempStr = Integer.toBinaryString(0xff & a[i]);
    		// while(tempStr.length() < 8)
    		tempStr = Helper.zeroPadding[8-tempStr.length()] + tempStr;
    		sb.append(tempStr);
    	}
    	return sb.toString();
    }
    
    public static int unsignedByteToInt(byte b) { return (int) b & 0xFF; }
    
    /**
     * Calculate the SAD for 2 blocks.
     * @param b1, byte array of data for block 1
     * @param w1, block 1 top left pixel horizontal coordinate
     * @param h1, block 1 top left pixel vertical coordinate
     * @param b2, byte array of data for block 2
     * @param w2, block 2 top left pixel horizontal coordinate
     * @param h2, block 2 top left pixel vertical coordinate
     * @param blockSize, block size (from config most likely)
     * @param vidWidth, width of video frame
     * @return
     */
    public static int calculateSAD(byte[] b1, int w1, int h1, byte[] b2, int w2, int h2, int blockSize, int vidWidth) {
        int sad = 0;
        for (int k = 0; k < blockSize; k++) {
            int wOfCur = w1 + k;
            int wOfRef = w2 + k;
            for (int j = 0; j < blockSize; j++) {
                int hOfCur = h1 + j;
                int hOfRef = h2 + j;
                sad += Math.abs(unsignedByteToInt(b1[hOfCur * vidWidth + wOfCur])
                        - unsignedByteToInt(b2[hOfRef * vidWidth + wOfRef]));
            }
        }
        return sad;
    }
    
    public static int[] extractBlockFromFrame(byte[] frame, int w1, int h1, int blockSize, int vidWidth) {
    	int[] blk = new int[blockSize*blockSize];
    	int index=0;
    	
    	for (int k = 0; k < blockSize; k++) {
            int wOfCur = w1 + k;
            for (int j = 0; j < blockSize; j++) {
                int hOfCur = h1 + j;
                blk[index] = frame[hOfCur*vidWidth + wOfCur];
                index+=1;
            }
        }
    	
    	return blk;
    }
}

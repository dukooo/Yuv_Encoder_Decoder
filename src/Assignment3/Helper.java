package Assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
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
										  "0000000000000000",
										  "00000000000000000",
										  "000000000000000000"
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
    
    public static short[] intArrListToShortArr(ArrayList<Integer> a) {
    	short[] ret = new short[a.size()];
    	for (int i = 0; i < a.size(); i++) {
    		if (a.get(i) < -32768 || a.get(i) > 32767) {
    			System.out.println("Number " + a.get(i) + " cannot be converted to short without losing bits");
    			System.exit(1);
    		}

    		ret[i] = (short) a.get(i).shortValue();
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
	
    public static int [] getDiffEncoding(int [] curr, int [] prev) {
    	
    	int [] diff = new int[curr.length];
    	for (int i = 0; i < prev.length; i++) {
    		diff[i] = curr[i] - prev[i];
    	}
    	
    	
    	if (curr.length == prev.length) {
    		return diff;
    	}
    	else {
    		for (int i=prev.length; i<curr.length; i+=3) {
    			diff[i] = curr[i] - curr[i-3];
    			diff[i+1] = curr[i+1] - curr[i-2];
    			diff[i+2] = curr[i+2] - curr[i-1];
    		}
    	}
    	
    	return diff;
    }
    
	public int getBlockIndex(int pixel_i, int width , int blockSize) {
		// first get row and col of pixel
		int row = pixel_i/width;
		int col = pixel_i % width;

		int block_row = row/blockSize;
		int block_col = col/blockSize;

		int block_num = block_row*(width/blockSize) + block_col;
		return block_num;
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
    
    public static int[] extractBlockFromFrame(int[] frame, int w1, int h1, int blockSize, int vidWidth) {
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
    public static Object[] clone(Object[] array) {
        if (array == null) {
            return null;
        }
        return (Object[]) array.clone();
    }
    
    public static int[] addAll(int[] array1, int[] array2) {
            if (array1 == null) {
                return array2.clone();
            } else if (array2 == null) {
                return array1.clone();
            }
            int[] joinedArray = new int[array1.length + array2.length];
            System.arraycopy(array1, 0, joinedArray, 0, array1.length);
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;
    }
    
    /**
     * Combine 2 sets of data into combined. 
     * @param d1
     * @param d2
     * @param combined
     * @param start1, index of d1 where copying should start, element at start1 is copied
     * @param end1, index of d1 where copying should end, element at end1 is NOT copied
     * @param start2, index of d2 where copying should start, element at start2 is copied
     * @param end2, index of d2 where copying should end, element at end2 is NOT copied
     */
    public static void combineTwo(byte[] d1, byte[] d2, byte[] combined, int start1, int end1, int start2, int end2) {
    	// In place combination
    	for (int i = start1; i < end1; i++) {
    		combined[i] = d1[i];
    	}
    	
    	for (int i = start2; i < end2; i++) {
    		combined[i] = d2[i];
    	}
    }
    
    /**
     * Combine 2 sets of data into combined. 
     * @param d1
     * @param d2
     * @param combined
     * @param start1, index of d1 where copying should start, element at start1 is copied
     * @param end1, index of d1 where copying should end, element at end1 is NOT copied
     * @param start2, index of d2 where copying should start, element at start2 is copied
     * @param end2, index of d2 where copying should end, element at end2 is NOT copied
     */
    public static void combineTwo(int[] d1, int[] d2, int[] combined, int start1, int end1, int start2, int end2) {
    	// In place combination
    	for (int i = start1; i < end1; i++) {
    		combined[i] = d1[i];
    	}
    	
    	for (int i = start2; i < end2; i++) {
    		combined[i] = d2[i];
    	}
    }
    
    public static ArrayList<Integer> combineMVsForOneFrame(ArrayList<Integer> l1, ArrayList<Integer> l2, int numItemsPerRow, int numElementsPerItem) {
    	int readCnt1 =0;
    	int readCnt2 =0;
    	ArrayList<Integer> ret = new ArrayList<Integer>();
    	
    	if (l1.size() != l2.size()) {
    		System.out.println("combine two list of MVs but they don't have same size.");
    		System.exit(1);
    	}
    	
    	while (readCnt1 < l1.size() && readCnt2 < l2.size()) {
    		
    		// read X num of elements from l1, and then read X num of elements from l2 (both list must be same size)
    		for (int i = 0; i < numItemsPerRow; i++) {
    			for (int j = 0; j < numElementsPerItem; j++) {
    				ret.add(l1.get(readCnt1));
        			readCnt1 += 1;
    			}
    		}
    		
    		for (int i = 0; i < numItemsPerRow; i++) {
    			for (int j = 0; j < numElementsPerItem; j++) {
    				ret.add(l2.get(readCnt2));
    				readCnt2 += 1;
    			}
    		}
    	}
    	
    	return ret;
    }
}

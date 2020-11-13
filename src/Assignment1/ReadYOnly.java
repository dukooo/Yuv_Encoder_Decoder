/***
 * exercise 2 of assignment 1, ECE 1783
 */

package Assignment1;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReadYOnly {

	int width, height, frameLength;
	DataInputStream yuv;
	byte[] currFrame;
	byte[] prevFrame;
	
	/***
	 * 
	 * @param width, width of image
	 * @param height, height of image
	 * @param frameLength, the number of frame it contains
	 * @param i the YOnly file is divided into (ixi) blocks
	 */
	public ReadYOnly(String filename, int width, int height, int frameLength) {
		this.width = width;
		this.height = height;
		this.frameLength = frameLength;
		prevFrame = null;
		currFrame = null;
		startReading(filename);
		this.frameLength = frameLength;
	}

	/***
	 * start reading
	 * @param filename
	 */
	public void startReading(String filename) {
		try {
			yuv = new DataInputStream(new BufferedInputStream(
					new FileInputStream("yuvFiles/" + filename)));
		} catch(Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * read one frame at a time.
	 * store y values in oneFrame in bytes and then convert it to int[]
	 */
	public void readOneFrame() {
		setPrevFrame();
		currFrame = new byte[width * height];
		try {
			int yuvNum = width * height;
			yuv.read(currFrame);
			// uncomment when using yuv 4:2:2 file
			//byte[] uv = new byte[width * height * 1/2];
			//yuv.read(uv);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * set the previous frame
	 */
	public void setPrevFrame() {
		if (currFrame != null) {
			prevFrame = new byte[currFrame.length];
			for (int i = 0; i < currFrame.length; i++) {
				prevFrame[i] = currFrame[i];
			}
		}
	}

	/**
	 * convert unsigned byte to int
	 * @param b
	 * @return int value of b
	 */
	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}
	
	/**
	 * 
	 * @param i, each block is of size ixi
	 * @return the width of the frame after padding;
	 */
	public int getWidthAfterPadding(int i) {
		if (width % i != 0)
			return (int)(width / i + 1) * i;
		else
			return width;
	}
	
	/**
	 * 
	 * @param i, each block is of size ixi
	 * @return the height of the frame after padding;
	 */
	public int getHeightAfterPadding(int i) {
		if (height % i != 0)
			return (int)(height / i + 1) * i;
		else
			return height;
	}
	
	/**
	 * Helper function to get total number of blocks in this video file.
	 * @param i, the block size (each block is ixi)
	 * @return
	 */
	public int getTotalNumOfBlocks(int i) {
		
		return (int)(getHeightAfterPadding(i)/i*getWidthAfterPadding(i)/i);
	}
	
	/** 2a, b
	 * convert fron byte[] to int[], store only the Y value
	 * paddin if the width and/or height of the frame is not divisible by i
	 * @param i, each block is of size ixi
	 * @return padded y-only file
	 */
	public int[] paddingYOnly(byte[] frame, int i) {
		int widthAfterPadding = getWidthAfterPadding(i);
		int heightAfterPadding = getHeightAfterPadding(i);
		int[] yOnly = new int[widthAfterPadding * heightAfterPadding];
		for (int j = 0; j < heightAfterPadding; j++) {
			for (int k = 0; k < widthAfterPadding; k++) {
				if (k < width && j < height)
					yOnly[j * widthAfterPadding + k] = unsignedByteToInt(frame[j * width + k]);
				else
					yOnly[j * widthAfterPadding + k] = 128; // pad with gray
			}
		}
		return yOnly;
	}
	
	/** 2c
	 * split each frame into (ixi) blocks and calculate the average of the sample value in each block
	 * @param i, each block is of size ixi
	 * @return int array with the average value of each block
	 */
	public int[] getBlockAve(byte[] frame, int i) {
		int[] yOnly = paddingYOnly(frame, i);
		int widthAfterPadding = getWidthAfterPadding(i);
		int heightAfterPadding = getHeightAfterPadding(i);
		int[] block = new int[(widthAfterPadding / i) * (heightAfterPadding / i)];
		for (int q = 0; q < heightAfterPadding; q+=i) {
			for (int p = 0; p < widthAfterPadding; p+=i) {
				int index = q / i * widthAfterPadding / i + p / i;
				block[index] = 0;
				for (int s = 0; s < i; s++) {
					for (int r = 0; r < i; r++) {
						block[index] += yOnly[(q + r) * widthAfterPadding + (p + s)];
					}
				}
				block[index] = (int) (block[index] / i / i);
				}
		}
		return block;
	}

	/** 2d
	 * replace every (ixi) block with another (ixi) block of identical elements of this average value
	 * @param i, each block is of size ixi
	 * @return the Y-only-block-averaged file
	 */
	public int[] getYOnlyBlockAverage(byte[] frame, int i) {
		int widthAfterPadding = getWidthAfterPadding(i);
		int heightAfterPadding = getHeightAfterPadding(i);
		int[] block = getBlockAve(frame, i);
		int[] yOnlyBlockAverage = new int[widthAfterPadding * heightAfterPadding];
		for (int q = 0; q < heightAfterPadding; q++) {
			for (int p = 0; p < widthAfterPadding; p++) {
				int blockWidth = (int)p / i;
				int blockHeight = (int)q / i;
				yOnlyBlockAverage[q * widthAfterPadding + p] = block[blockHeight * (widthAfterPadding / i) + blockWidth];
			}
		}
		return yOnlyBlockAverage;
	}
	
	/** 2e
	 * subjectively compare every original Y-only file with its corresponding Y-only-block-averaged one
	 * @param i, each block is of size ixi
	 * @return the subjective difference
	 */
	public int[] getSubDifference(byte[] frame, int i) {
		int[] yOnly = paddingYOnly(frame, i);
		int[] yOnlyBlockAverage = getYOnlyBlockAverage(frame, i);
		int[] sub = new int[yOnly.length];
		if (yOnlyBlockAverage.length != yOnly.length) {
			System.exit(1);
		}
		for (int n = 0; n < yOnlyBlockAverage.length; i++) {
			sub[n] = yOnly[n] - yOnlyBlockAverage[n];
		}
		return sub;
	}
	
	/** 2f
	 * get the psnr difference
	 * @param i, each block is of size ixi
	 * @return the psnr difference
	 */
	public double getPSNRDifference(byte[] frame, int i) {
		int widthAfterPadding = getWidthAfterPadding(i);
		int heightAfterPadding = getHeightAfterPadding(i);
		int[] sub = getSubDifference(frame, i);
		int mseSum = 0;
		for (int n = 0; n < sub.length; n++) {
			mseSum += sub[n];
		}
		int mse = mseSum / widthAfterPadding / heightAfterPadding;
		double psnr = 10 * logbase10(Math.pow((Math.pow(2, sub.length) - 1), 2) / mse);
		return psnr;
	}
	
    /**
     * Quick alias to get log in base 10.
     * @param x Input number.
     * @return Returns the log10(x).
     */
    public static double logbase10(double x) {
        return Math.log(x) / Math.log(10);
    }
    

	
	
}

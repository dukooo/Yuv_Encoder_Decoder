/***
 * exercise 1 of assignment 1, ECE 1783
 */

package Assignment1;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

import javax.imageio.ImageIO;

public class ReadYUV {
	
	DataInputStream yuv;
	int width, height, frameLength;
	byte[] currFrame; // yuv value in bytes
	int[] yuv420; // yuv value in int, contains all Y values followed by all U values and V values
	int[] yuv444;
	int[] rgb; // contains all R values followed by all G values and B values;
	
	/***
	 * 
	 * @param width, width of image
	 * @param height, height of image
	 */
	public ReadYUV(int width, int height, int frameLength) {
		this.width = width;
		this.height = height;
		this.frameLength = frameLength;
	}
	
	/***
	 * start reading. initiate a few attributes.
	 * @param filename
	 */
	public void startReading(String filename) {
		try {
			yuv = new DataInputStream(new BufferedInputStream(
					new FileInputStream("yuvFiles/" + filename)));
			int yuvNum = width * height * 3/2; // 3/2 = 1 (y) + 1/4 (u) + 1/4 (v)
			int yuvNum444 = width * height * 3;
			currFrame = new byte[yuvNum];
			yuv420 = new int[yuvNum];
			yuv420 = new int[yuvNum444];
		} catch(Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * read one frame at a time.
	 * store yuv values in oneFrame in bytes and then convert it to int[]
	 * upscale it to 4:4:4 and convert to corresponding RGB file
	 */
	public void readOneFrame() {
		try {
		int yuvNum = width * height * 3/2; // 3/2 = 1 (y) + 1/4 (u) + 1/4 (v)
		int len = yuv.read(currFrame);
		if (len != yuvNum) {
			System.exit(1);
		}
		convertOneFrameToYUV420();
		convertYUV420ToYUV440();
		generateRGB();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/**
	 * convert from byte[] to int[]
	 */
	public void convertOneFrameToYUV420() {
		int arraySize = height * width;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int k = unsignedByteToInt(currFrame[j * width + i]);
				yuv420[j * width + i] = k;
				yuv420[(j/2) * (width/2) + i/2 + arraySize] = 
						unsignedByteToInt(currFrame[(j/2) * (width/2) + i/2 + arraySize]);
				yuv420[(j/2) * (width/2) + i/2 + arraySize + arraySize/4] = 
						unsignedByteToInt(currFrame[(j/2) * (width/2) + i/2 + arraySize + arraySize/4]);
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
	 * upscale a 4:2:0 video sequence to 4:4:4
	 */
	public void convertYUV420ToYUV440() {
		int arraySize = height * width;
		yuv444 = new int[arraySize * 3];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				yuv444[j * width + i] = 
						yuv420[j * width + i];
				if (j % 2 == 0 && i % 2 == 0) {
					
					yuv444[arraySize + j * width + i] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize];
					yuv444[arraySize + j * width + i + 1] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize];
					yuv444[arraySize + (j + 1) * width + i] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize];
					yuv444[arraySize + (j + 1) * width + i + 1] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize];
					
					yuv444[arraySize + j * width + i + arraySize] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize + arraySize/4];
					yuv444[arraySize + j * width + i + 1 + arraySize] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize + arraySize/4];
					yuv444[arraySize + (j + 1) * width + i + arraySize] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize + arraySize/4];
					yuv444[arraySize + (j + 1) * width + i + 1 + arraySize] = 
							yuv420[(j/2) * (width/2) + i/2 + arraySize + arraySize/4];
				}
			}
		}
	}
	
	/**
	 * print the image into a .png file
	 * @param filename, name of the png file
	 */
	public BufferedImage printImage(String filename) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					int R = rgb[j * width + i];
					int G = rgb[width * height + j * width + i];
					int B = rgb[width * height * 2 + j * width + i];
					int rColor = (0xff << 24) | (R << 16) | (G << 8) | B;
					image.setRGB(i, j, rColor);
				}
			}
			ImageIO.write(image, "png", new File("results/" + filename));
			return image;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * convert yuv to rgb 
	 */
	public void generateRGB() {
		int arraySize = height * width;
		rgb = new int[arraySize * 3];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int Y = yuv444[y * width + x];
				int U = yuv444[y * width + x + arraySize];
				int V = yuv444[y * width + x + arraySize * 2];
				
				int R = (int)((Y - 16) * 1.164 + (V - 128) * 1.596);
				int G = (int)((Y - 16) * 1.164 - (U - 128) * 0.391 - (V - 128) * 0.813);
				int B = (int)((Y - 16) * 1.164 + (U - 128) * 2.018);
				
				if (R > 255) R = 255;
				if (G > 255) G = 255;
				if (B > 255) B = 255;
				
				if (R < 0) R = 0;
				if (G < 0) G = 0;
				if (B < 0) B = 0;
				
				rgb[y * width + x] = R;
				rgb[arraySize + y * width + x] = G;
				rgb[arraySize * 2 + y * width + x] = B;
				
			}
		}
	}

//--------------------Exercise 1/c-----------------------//
	/**
	 * add random Gaussian noise to Y component
	 */
	public void SetYWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = 0 ; i < arraySize ; i++) {
			yuv444[i] += rand.nextGaussian();
		}
	}	

	/**
	 * add random Gaussian noise to U component
	 */
	public void SetUWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = arraySize ; i < arraySize * 2 ; i++) {
			yuv444[i] += rand.nextGaussian();
		}
	}	

	/**
	 * add random Gaussian noise to V component
	 */
	public void SetVWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = arraySize * 2 ; i < arraySize * 3 ; i++) {
			yuv444[i] += rand.nextGaussian();
		}
	}

	/**
	 * add random Gaussian noise to R component
	 */
	public void SetRWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = 0 ; i < arraySize ; i++) {
			rgb[i] += rand.nextGaussian();
		}
	}	

	/**
	 * add random Gaussian noise to G component
	 */
	public void SetGWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = arraySize ; i < arraySize * 2 ; i++) {
			rgb[i] += rand.nextGaussian();
		}
	}	

	/**
	 * add random Gaussian noise to B component
	 */
	public void SetBWithNoise() {
		Random rand = new Random();
		int arraySize = width * height;
		for (int i = arraySize * 2 ; i < arraySize * 3 ; i++) {
			rgb[i] += rand.nextGaussian();
		}
	}

}

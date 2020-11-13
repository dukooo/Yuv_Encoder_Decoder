package Assignment2;

import java.util.ArrayList;

public class VisualizeChange {

	//                               blue,       green,    red,     yellow
	private static byte[] COLORu = {(byte)255, (byte)42, (byte)90, (byte)17};
	private static byte[] COLORv = {(byte)127, (byte)44, (byte)240, (byte)146};
	int blockSize, width, height;
	
	public VisualizeChange() {
		PropertyHelper ph = new PropertyHelper();
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
	}
	
	public void VisualizePart1(byte[] reconstructedFrame, byte[] visualizePart1, int col, int row, int[] mv) {
		for (int y = row; y < row + blockSize; y++) {
			for (int x = col; x < col + blockSize; x++) {
				visualizePart1[y * width + x] = reconstructedFrame[y * width + x];
				visualizePart1[width * height + y * width + x] = COLORu[mv[0]];
				visualizePart1[width * height * 2 + y * width + x] = COLORv[mv[0]];
			}
		}
	}
}

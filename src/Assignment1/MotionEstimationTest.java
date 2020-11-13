package Assignment1;

import java.util.*;

public class MotionEstimationTest {

	public static void main(String[] args) {
		// Print the motion vector for the first 10 frames taking i = 64, r = 1;
		BasicMotionEstimation yuv = new BasicMotionEstimation("akiyo_qcif.yuv", 176, 144, 300);
		int i = 0;
		while (i < 10) {
			yuv.readOneFrame();
			ArrayList<int[]> mv = yuv.findBestPrediction(64, 1);
			int a = i + 1;
			System.out.println("For frame " + a);
			for (int[] item : mv) {
				System.out.println("x = " + item[0] + ", y = " + item[1]);
			}
			i++;
		}
	}
}

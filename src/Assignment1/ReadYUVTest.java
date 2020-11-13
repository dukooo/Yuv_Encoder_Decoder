package Assignment1;

class ReadYUVTest {
	
	public static void main(String[] arg) {
		

		//--------------------Exercise 1/a, b-----------------------//
		
		
		ReadYUV yuv = new ReadYUV(176, 144, 300);
		yuv.startReading("akiyo_qcif.yuv");
		int i = 0;
		while (i < yuv.frameLength) {
			yuv.readOneFrame();
			int a = i + 1;
			yuv.printImage("akiyo_qcif" + a + ".png");
			i++;
		}
		

		//--------------------Exercise 1/c-----------------------//

		/*
		while (i < yuv.frameLength) {
			yuv.readOneFrame();
			int a = i + 1;
			yuv.SetYWithNoise();
			yuv.printImage("ynoise_akiyo_qcif" + a + ".png");
			i++;
		}
		*/
	}
}

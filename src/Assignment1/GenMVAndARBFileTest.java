package Assignment1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

public class GenMVAndARBFileTest {

	public static void main(String[] args) {
		
		PropertyHelper ph = new PropertyHelper();
		
		/*
		 * GenerateMVFile mvFile = new GenerateMVFile("akiyo_qcif.yuv", 176, 144, 300,
		 * 64, 1); mvFile.generateMVFile("test.test");
		 */
	
		
		ResidualBlockGenerator rbg = new ResidualBlockGenerator(ph.getProperty("vidName"), "MotionVector.mv", Integer.valueOf(ph.getProperty("vidWidth")), 
				Integer.valueOf(ph.getProperty("vidHeight")), Integer.valueOf(ph.getProperty("blkSizeI")), Integer.valueOf(ph.getProperty("multipleN")));
		
	}
}

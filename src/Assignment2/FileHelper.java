package Assignment2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileHelper {
	
	public static boolean fileExist(String filePath) {
		File outFile = new File(filePath);
		return outFile.exists();
	}
	
	/**
	 * Write any byte array to file. The output file will appear in output/
	 * Note: This function will first DELETE the file if it already exist.
	 * @param bytes : the byte array with data to be written
	 * @param outputFileName : filename to be used, just give filename, do not need file path
	 */
	public static void writeToFile(byte [] bytes, String outputFileName) {
		
		String outFilePath = System.getProperty("user.dir") + "/output/" + outputFileName;
		File outFile = new File(outFilePath);
		if (outFile.exists()) {
				System.out.println(outFilePath + " already exist, will try to delete first.");
            if(outFile.delete()) {
    				System.out.println(outFilePath + " deleted.");
            }
            else {
            	System.out.println("Cannot delete file: " + outFilePath);
            	System.exit(1);
            }
        }
		try {
			outFile.createNewFile();
            FileOutputStream fis = new FileOutputStream(outFile, true);

            fis.write(bytes);
            
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
			System.exit(1);
        }
	}

	/**
	 * Write any byte array to file. The output file will appear in output/
	 * This function will append to an existing file (if it already exist), if not it will create the file and write to it.
	 * @param bytes : the byte array with data to be written
	 * @param outputFileName : filename to be used, just give filename, do not need file path
	 */
	public static void writeToFileAppend(byte [] bytes, String outputFileName) {
		
		String outFilePath = System.getProperty("user.dir") + "/output/" + outputFileName;
		File outFile = new File(outFilePath);
		
		try {
			outFile.createNewFile();
            FileOutputStream fis = new FileOutputStream(outFile, true);

            fis.write(bytes);
            
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
			System.exit(1);
        }
	}
	
	/**
	 * Write any byte array to file. The output file will appear in output/
	 * Note: This function will first DELETE the file if it already exist.
	 * @param shorts : the short (16 bit signed) array with data to be written
	 * @param outputFileName : filename to be used, just give filename, do not need file path
	 */
	public static void writeToFile(short [] shorts, String outputFileName) {
		
		String outFilePath = System.getProperty("user.dir") + "/output/" + outputFileName;
		File outFile = new File(outFilePath);
		if (outFile.exists()) {
            System.out.println(outFilePath + " already exist, will try to delete first.");
            if(outFile.delete()) {
                System.out.println(outFilePath + " deleted.");
            }
            else {
            	System.out.println("Cannot delete file: " + outFilePath);
            	System.exit(1);
            }
        }
		try {
			outFile.createNewFile();
            FileOutputStream fis = new FileOutputStream(outFile, true);
            DataOutputStream dos = new DataOutputStream(fis);
            for (short s : shorts)
            	dos.writeShort(s);
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
			System.exit(1);
        }
	}

	/**
	 * Write any short array to file. The output file will appear in output/
	 * This function will append to an existing file (if it already exist), if not it will create the file and write to it.
	 * @param bytes : the byte array with data to be written
	 * @param outputFileName : filename to be used, just give filename, do not need file path
	 */
	public static void writeToFileAppend(short [] shorts, String outputFileName) {
		
		String outFilePath = System.getProperty("user.dir") + "/output/" + outputFileName;
		File outFile = new File(outFilePath);
		
		try {
			outFile.createNewFile();
            FileOutputStream fis = new FileOutputStream(outFile, true);
            DataOutputStream dos = new DataOutputStream(fis);
            
            for (short s : shorts)
            	dos.writeShort(s);
            
            dos.close();
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
			System.exit(1);
        }
	}
	
	/**
	 * Helper function to read the Motion Vector file
	 * @param filePath, full path to file 
	 * @return int [] of motion vectors
	 */
	public static int[] readMVFile(String filePath) {
		
        File inFile = new File(filePath);
        byte [] byteRet = null;
        int [] intRet = null;
        
        try {
            FileInputStream fis = new FileInputStream(inFile);
            byteRet = fis.readAllBytes();
            
            // Convert everything to int[] since Motion Vectors are always integers
            intRet = new int[byteRet.length];
            for (int i =0; i < byteRet.length; i++) {
            	intRet[i] = (int) byteRet[i];
            }
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
            System.exit(1);
        }
        return intRet;
	}
	
	/**
	 * Helper function to read a file with byte value
	 * @param filePath, full path to file 
	 * @return byte [] of motion vectors
	 */
	public static byte[] readByteFile(String filePath) {
		
        File inFile = new File(filePath);
        byte [] byteRet = null;
        
        try {
            FileInputStream fis = new FileInputStream(inFile);
            byteRet = fis.readAllBytes();
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
            System.exit(1);
        }
        return byteRet;
	}
	
	/**
	 * Helper function to read a file with short (16bit signed) values
	 * @param filePath
	 * @return short []
	 */
	public static short[] readShortFile(String filePath) {
		
        File inFile = new File(filePath);
        short [] shortRet = null;
        int readCnt = 0;
        
        try {
            FileInputStream fis = new FileInputStream(inFile);
            DataInputStream dis = new DataInputStream(fis);
            shortRet = new short[dis.available()/2];
            while (dis.available() != 0) {
            	shortRet[readCnt] = dis.readShort();
            	readCnt++;
            }
            dis.close();
            fis.close();
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
            System.exit(1);
        }
        return shortRet;
	}
	
	
	
}

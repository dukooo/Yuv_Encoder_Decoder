package Assignment3;

import java.util.ArrayList;
import java.util.Arrays;

public class InterpolationBuffer {
    int width, height, numRef, blockSize;
    
    public InterpolationBuffer() {
        PropertyHelper ph = new PropertyHelper();
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		numRef = Integer.valueOf(ph.getProperty("nRefFrames"));
        
    }

    public byte[][] getInterpolatedBuffer(byte[] reference) {
        /*
        ex. block size = 2
         ref         "expanded"
        1 2 3        1 2 | 2 3
        4 5 6    ==> 4 5 | 5 6
        7 8 9        ---------
                     4 5 | 5 6
                     7 8 | 8 9
        */
        int numbBlcok_width_expanded = 0; // number of blocks in width of expanded ref frame
        int numbBlcok_height_expanded = 0; // number of blocks in height of expanded ref frame
        /* delete later
        for (int h = 0; h < height; h++) {
            if (h == 0) {
                for (int w = 0; w < width; w++)
                    if (w + blockSize <= width && h + blockSize <= height) numbBlcok_width_expanded++;
            }
            if (h + blockSize <= height) numbBlcok_height_expanded++;
        }
        */

        // finds number of blocks in width and height of expanded
        for (int h = 0; h < height; h++) if (h + blockSize <= height) numbBlcok_height_expanded++;
        for (int w = 0; w < width; w++) if (w + blockSize <= width) numbBlcok_width_expanded++;
        if (numbBlcok_height_expanded <= 0 || numbBlcok_width_expanded <= 0) System.out.println("Block Size is bigger than Frame Size");
        
        int numbBlcok_width_buffer = numbBlcok_width_expanded * 2 - 1; // number of blocks in width of interpolated buffer
        int numbBlcok_height_buffer = numbBlcok_height_expanded * 2 - 1; // number of blocks in height of interpolated buffer
        
        int width_buffer = blockSize * numbBlcok_width_buffer; // width of the buffer
        int height_buffer = blockSize * numbBlcok_height_buffer; // height of the buffer

        byte[][] buffer = new byte[height_buffer][width_buffer]; // 2-D buffer matrix 
        // byte[][] refMatrix = new byte[height][width]; 
        // for (int h = 0; h < height; h++) { // converts 1-D reference frame to 2-D matrix
        //     for (int w = 0; w < width; w++) {
        //         refMatrix[h][w] = reference[h * width + w];
        //     }
        // }

        // Updates the buffer with only original reference pixels first
        /* 
        ex.    Ref           Interpolated Buffer
            25 28 29        25 28 | *  * | 25 29
            50 57 53   ==>  50 57 | *  * | 57 53
            44 52 56        ---------------------
                            *  *  | *  * | *  *
                            *  *  | *  * | *  *
                            ---------------------
                            50 57 | *  * | 57 53
                            44 52 | *  * | 52 56
        */
        for (int h = 0; h < numbBlcok_height_buffer; h++) { // block operation
            for (int w = 0; w < numbBlcok_width_buffer; w++) {
                if (w % 2 == 0 && h % 2 == 0) { // if the block position is for reference pixels (even position)
                    int heightIndex = 0;
                    for (int y = blockSize*h; y < blockSize*h + blockSize; y++) { // pixel operation
                        int widthIndex = 0;
                        for (int x = blockSize*w; x < blockSize*w + blockSize; x++) {
							// buffer[y][x] = refMatrix[h/2+heightIndex][w/2+widthIndex++]; // copies original reference pixel to the buffer
							buffer[y][x] = reference[(h/2+heightIndex)*width + (w/2+widthIndex++)];
                        }
                        heightIndex++;
                    }
                }
            }
        }

        // Updates the buffer with interpolated pixels (using average of reference pixels)
        /* 
        ex.    Ref           Interpolated Buffer
            25 28 29        25 28 | 27 29 | 25 29
            50 57 53   ==>  50 57 | 54 55 | 57 53
            44 52 56        ---------------------
                            38 43 | *   * | 43 41
                            47 55 | *   * | 55 55
                            ---------------------
                            50 57 | 54 55 | 57 53
                            44 52 | 48 54 | 52 56
        */
        for (int h = 0; h < numbBlcok_height_buffer; h++) { // block operation
            for (int w = 0; w < numbBlcok_width_buffer; w++) {
                // if the block position is for interpolated pixels that DONT NEED averaging of other interpoalted pixels
                if ((h % 2 != 0 || w % 2 != 0) && (h+w) % 2 != 0) { 
                    if (h % 2 == 0) { // needs horizontal averaging
                        for (int y = blockSize*h; y < blockSize*h + blockSize; y++) { // pixel operation
                            for (int x = blockSize*w; x < blockSize*w + blockSize; x++) {
                                int sum = Helper.unsignedByteToInt(buffer[y][x - blockSize]) + 
                                          Helper.unsignedByteToInt(buffer[y][x + blockSize]);
                                int avg = (int) Math.ceil((double) sum / 2);
                                buffer[y][x] = (byte) avg;
                            }
                        }
                    }
                    else if (h % 2 != 0) { // needs vertical averaging
                        for (int y = blockSize*h; y < blockSize*h + blockSize; y++) { // pixel operation
                            for (int x = blockSize*w; x < blockSize*w + blockSize; x++) {
                                int sum = Helper.unsignedByteToInt(buffer[y - blockSize][x]) + 
                                          Helper.unsignedByteToInt(buffer[y + blockSize][x]);
                                int avg = (int) Math.ceil((double) sum / 2);
                                buffer[y][x] = (byte) avg;
                            }
                        }
                    }
                }
                
            }
        }

        // Updates the buffer with interpolated pixel (using average of other interpolated pixels)
        /* 
        ex.    Ref           Interpolated Buffer
            25 28 29        25 28 | 27 29 | 25 29
            50 57 53   ==>  50 57 | 54 55 | 57 53
            44 52 56        ---------------------
                            38 43 | 41 42 | 43 41
                            47 55 | 51 55 | 55 55
                            ---------------------
                            50 57 | 54 55 | 57 53
                            44 52 | 48 54 | 52 56
        */
        for (int h = 0; h < numbBlcok_height_buffer; h++) { // block operation
            for (int w = 0; w < numbBlcok_width_buffer; w++) {
                // if the block position is for interpolated pixels that NEED averaging of other interpoalted pixels
                if ((h % 2 != 0 && w % 2 != 0) && (h+w) % 2 == 0) { 
                    for (int y = blockSize*h; y < blockSize*h + blockSize; y++) { // pixel operation
                        for (int x = blockSize*w; x < blockSize*w + blockSize; x++) {
                            // finds average of other interpolated pixels
                            int sum = Helper.unsignedByteToInt(buffer[y - blockSize][x]) + 
                                      Helper.unsignedByteToInt(buffer[y][x - blockSize]) +
                                      Helper.unsignedByteToInt(buffer[y][x + blockSize]) +
                                      Helper.unsignedByteToInt(buffer[y + blockSize][x]);
                            int avg = (int) Math.ceil((double) sum / 4);
                            buffer[y][x] = (byte) avg;
                        }
                    }
                }
                
            }
        }
        // System.out.println();
        // System.out.println("Reference");
        // System.out.println(Arrays.deepToString(refMatrix).replace("], ", "]\n"));
        // System.out.println();
        // System.out.println("Buffer");
        // System.out.println(Arrays.deepToString(buffer).replace("], ", "]\n"));
        return buffer;
    }

    public ArrayList<byte[][]> buildBuffers(byte[][] reference) {
        ArrayList<byte[][]> buffers = new ArrayList<>();

        for (int f = 0; f < numRef; f++) { 
            buffers.add(getInterpolatedBuffer(reference[f]));
        }
        return buffers;
    }

    public static void main (String[] args) {
        InterpolationBuffer a = new InterpolationBuffer();
        a.blockSize = 2;
        a.width = 4;
        a.height = 4;
        byte[] reference = {25, 28, 29, 45, 50, 57, 53, 21,44,52,56,78,94,33,63,10};
        a.getInterpolatedBuffer(reference);
    }
}
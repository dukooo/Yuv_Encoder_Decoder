package Assignment2;

import java.util.ArrayList;

public class VisualizeChangePartC {
    // Draws Motion Vector - Arrow Head: Beige & Arrow Body: Black
    // Assuming FME is Enabled
    public static void drawMv(boolean split, int blockSize, int width, int height, int frameNumb, 
                              byte[] reconstructedFrame, byte[] visualizePart3, int col, int row, int[] mvOnly) {
        int currFrame = 3*frameNumb*width*height;

        if (split) { // Divided Block
            ArrayList<int[]> path_top_left = getDiagonalPath(col, row, col+mvOnly[0]/2, row+mvOnly[1]/2); 
            drawArrow(path_top_left, currFrame, width, height, visualizePart3);
            ArrayList<int[]> path_top_right = getDiagonalPath(col, row+blockSize/2, col+mvOnly[2]/2, row+blockSize/2+mvOnly[3]/2); 
            drawArrow(path_top_right, currFrame, width, height, visualizePart3);
            ArrayList<int[]> path_bot_left = getDiagonalPath(col+blockSize/2, row, col+blockSize/2+mvOnly[4]/2, row+mvOnly[5]/2); 
            drawArrow(path_bot_left, currFrame, width, height, visualizePart3);
            ArrayList<int[]> path_bot_right = getDiagonalPath(col+blockSize/2, row+blockSize/2, col+blockSize/2+mvOnly[6]/2, row+blockSize/2+mvOnly[7]/2); 
            drawArrow(path_bot_right, currFrame, width, height, visualizePart3);
        }
        else { // Whole Block
            ArrayList<int[]> path = getDiagonalPath(col, row, col+mvOnly[0]/2, row+mvOnly[1]/2); // gets the pixel coordinates of the path
            drawArrow(path, currFrame, width, height, visualizePart3);
        }

        // Normal Pixels
        for (int y = row; y < row + blockSize; y++) {
            for (int x = col; x < col + blockSize; x++) {
                int y_index = currFrame+y * width + x;
                int u_index = currFrame+width * height + y * width + x;
                int v_index = currFrame+width * height * 2+ y * width + x;

                if (visualizePart3[u_index] == (byte)0 && visualizePart3[v_index] == (byte)0) { //if the pixel is along the path, don't update with normal pixel values
                    visualizePart3[y_index] = reconstructedFrame[frameNumb*width*height + y * width + x];
                    visualizePart3[u_index] = (byte) 128;
                    visualizePart3[v_index] = (byte) 128;
                }
            }
        }
    }

    public static void drawArrow(ArrayList<int[]> path, int currFrame, int width, int height, byte[] visualizePart3 ) {
        for (int i = 0; i < path.size(); i++) {
            int x = path.get(i)[0];
            int y = path.get(i)[1];
            
            if (i == 0) { // Arrow Head (just colors pixels in beige): ex. ------**
                          //													 **
                // Beige for arrow head
                visualizePart3[currFrame+y * width + x] = (byte) 172;
                visualizePart3[currFrame+width * height + y * width + x] = (byte) 119;
                visualizePart3[currFrame+width * height * 2+ y * width + x] = (byte) 141;
                visualizePart3[currFrame+y * width + x+1] = (byte) 172;
                visualizePart3[currFrame+width * height + y * width + x+1] = (byte) 119;
                visualizePart3[currFrame+width * height * 2+ y * width + x+1] = (byte) 141;
                visualizePart3[currFrame+(1+y) * width + x] = (byte) 172;
                visualizePart3[currFrame+width * height + (1+y) * width + x] = (byte) 119;
                visualizePart3[currFrame+width * height * 2+ (1+y) * width + x] = (byte) 141;
                visualizePart3[currFrame+(1+y) * width + x+1] = (byte) 172;
                visualizePart3[currFrame+width * height + (1+y) * width + x+1] = (byte) 119;
                visualizePart3[currFrame+width * height * 2+ (1+y) * width + x+1] = (byte) 141;
            }
            else {// Arrow Body: ex. ------
                // Black for arrow body
                visualizePart3[currFrame+y * width + x] = (byte) 23;
                visualizePart3[currFrame+width * height + y * width + x] = (byte) 124;
                visualizePart3[currFrame+width * height * 2+ y * width + x] = (byte) 131;
            }
        }
    }
 
    /**
	 * finds the pixel coordinates along the path from start to end 
	 * @param start_x x value of start pixel (top left of block)
	 * @param start_y y value of start pixel (top left of block)
	 * @param end_x x value of end pixel (top left of block)
	 * @param end_y x value of end pixel (top left of block)
	 * @return array list of pixel coordinates with format of (x, y)
	 */
	public static ArrayList<int[]> getDiagonalPath(int start_x, int start_y, int end_x, int end_y) {
        ArrayList<int[]> path = new ArrayList<>();
        int diff_x = Math.abs(end_x - start_x);
        int diff_y = Math.abs(end_y - start_y);
        int direction_x = start_x < end_x ? 1 : -1;
        int direction_y = start_y < end_y ? 1 : -1;
        int error = diff_x - diff_y;
        int error2;
        while (true) {
            int[] coordinate = {start_x, start_y};
            path.add(coordinate);
            if (start_x == end_x && start_y == end_y) break;
            
            error2 = 2*error;
            if (error2 > -diff_y) {
                error -= diff_y;
                start_x += direction_x;
            }

            if (error2 < diff_x) {
                error += diff_x;
                start_y += direction_y;
            }
        }
        return path;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Draws Mode - Vertical Mode: Green & Horizontal Mode: Blue
    public static void drawMode(boolean split, int blockSize, int width, int height, int frameNumb, 
                                byte[] reconstructedFrame, byte[] visualizePart3, int col, int row, int[] mode) {
        int currFrame = 3*frameNumb*width*height;
        if (split) { // Divided Block
            int i = 0;
            for (int y = row; y < row + blockSize; y+=blockSize/2) {
                for (int x = col; x < col + blockSize; x+=blockSize/2) {
                    for (int y2 = y; y2 < y+blockSize/2; y2++) {
                        for (int x2 = x; x2 < x+blockSize/2; x2++) {
                            int y_index = currFrame + y2 * width + x2;
                            int u_index = currFrame + width * height + y2 * width + x2;
                            int v_index = currFrame + width * height * 2+ y2 * width + x2;
                            visualizePart3[y_index] = reconstructedFrame[y2 * width + x2];
                            if (mode[i] == 0) {
                                visualizePart3[u_index] = (byte) 255;
                                visualizePart3[v_index] = (byte) 127;
                            }
                            else {
                                visualizePart3[u_index] = (byte) 42;
                                visualizePart3[v_index] = (byte) 44;
                            }
                        }
                    }
                    i++;
                }
            }
        }
        else { // Whole Block
            for (int y = row; y < row + blockSize; y++) {
                for (int x = col; x < col + blockSize; x++) {
                    int y_index = currFrame + y * width + x;
                    int u_index = currFrame + width * height + y * width + x;
                    int v_index = currFrame + width * height * 2+ y * width + x;
                    visualizePart3[y_index] = reconstructedFrame[y * width + x];
                    if (mode[0] == 0) {
					    visualizePart3[u_index] = (byte) 255;
                        visualizePart3[v_index] = (byte) 127;
                    }
                    else {
                        visualizePart3[u_index] = (byte) 42;
                        visualizePart3[v_index] = (byte) 44;
                    }
                }
            }
        }    
    }
   
	public static void main(String[] args) {
        // Overlays a map of MVs on the top of P-frame --> Arrow Head: Beige & Arrow Body: Black
        // Overlays a map of MODEs on the top of I-frame --> Vertical Mode: Green & Horizontal Mode: Blue
        // Generated file is in YUV 4:4:4 format
        // Run Encoder and first before runnig this
		PropertyHelper ph = new PropertyHelper();
		String inputYUV = ph.getProperty("encReconstructedFileName");
        String inputVBS = ph.getProperty("vbsFileName");
        String inputMV = "visualPartC.mv";
		int frameCnt = Integer.parseInt(ph.getProperty("frameCount"));
		int vidWidth = Integer.parseInt(ph.getProperty("vidWidth"));
		int vidHeight = Integer.parseInt(ph.getProperty("vidHeight"));
        int blkSize = Integer.parseInt(ph.getProperty("blkSizeI"));
        int iPeriod = Integer.parseInt(ph.getProperty("I_Period"));

        byte[] yuvBytes = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + inputYUV);
        byte[] vbsBytes = FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + inputVBS);
        short[] mvBytes = FileHelper.readShortFile(System.getProperty("user.dir") + "/output/" + inputMV);
        byte[] visualizePart3 = new byte[vidWidth * vidHeight * 3 * frameCnt];

        int blkInd = 0;
        int mvmode = 0;

		// per frame
		for (int frame = 0; frame < frameCnt; frame++) {
			for (int row = 0; row < vidHeight ; row+=blkSize) {
				for (int col = 0; col < vidWidth; col+= blkSize) { 
                    if (frame % iPeriod == 0) { // I Frame
                        if (vbsBytes[blkInd] == 1) { // Divided Block
                            int mode_top_left = mvBytes[mvmode++];
                            int mode_top_right = mvBytes[mvmode++];
                            int mode_bot_left = mvBytes[mvmode++];
                            int mode_bot_right = mvBytes[mvmode++];
                            drawMode(true, blkSize, vidWidth, vidHeight, frame, yuvBytes, visualizePart3, col, row, new int[] {mode_top_left, mode_top_right, mode_bot_left, mode_bot_right});
                        }
                        else { // Whole Block
                            int mode = mvBytes[mvmode++];
                            drawMode(false, blkSize, vidWidth, vidHeight, frame, yuvBytes, visualizePart3, col, row, new int[] {mode});
                        }
                    }
                    else { // P Frame
                        if (vbsBytes[blkInd] == 1) { // Divided Block
                            int refFrame_top_left = mvBytes[mvmode++];
                            int x_top_left = mvBytes[mvmode++];
                            int y_top_left = mvBytes[mvmode++];
                            int refFrame_top_right = mvBytes[mvmode++];
                            int x_top_right= mvBytes[mvmode++];
                            int y_top_right = mvBytes[mvmode++];
                            int refFrame_bot_left = mvBytes[mvmode++];
                            int x_bot_left = mvBytes[mvmode++];
                            int y_bot_left = mvBytes[mvmode++];
                            int refFrame_bot_right = mvBytes[mvmode++];
                            int x_bot_right = mvBytes[mvmode++];
                            int y_bot_right = mvBytes[mvmode++];
                            int[] mvOnly = {x_top_left, y_top_left, x_top_right, y_top_right, x_bot_left, y_bot_left, x_bot_right, y_bot_right};
                            drawMv(true, blkSize, vidWidth, vidHeight, frame, yuvBytes, visualizePart3, col, row, mvOnly);
                        }
                        else { // Whole Block
                            int refFrame = mvBytes[mvmode++];
                            int x = mvBytes[mvmode++];
                            int y = mvBytes[mvmode++];
                            int[] mvOnly = {x,y};
                            drawMv(false, blkSize, vidWidth, vidHeight, frame, yuvBytes, visualizePart3, col, row, mvOnly);
                        }
                    }
                    blkInd++;
				}
			}
        }
        
		FileHelper.writeToFile(visualizePart3, "visualizePartC.yuv");
	} 

}


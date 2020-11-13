package Assignment1;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class EncoderExercise4 {

	byte[] yOnly;// the original file after padding
	int width, height; // after padding
	int frameLength, blockSize, multipleN, rangeR, qp;
	int frameIndex;
	byte[] currFrame;
	byte[] predictor;
	String mvFileName;
	String mvEntropyEncodedFileName;
	// for geenrating the graph
	String residualAfterMVFileName;
	String residualBeforeMVFileName;
	// for the decoder side
	String residualFileName;
	String qtcFileName, qtcDiagOrderedFileName, qtcRleEncodedFileName, qtcEntropyEncodedFileName;
	String reconstructedFileName;
	String test4aFileName = "test4a.yuv";
	byte[] motionvector, testMV;
	int I_Period;
    int[] modesInFrame;
	// byte[] intraPredictor;

	public EncoderExercise4() {
		PropertyHelper ph = new PropertyHelper();
		frameLength = Integer.valueOf(ph.getProperty("frameCount"));
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
		yOnly = addPaddingToYOnly(Integer.valueOf(ph.getProperty("vidWidth")),
								  Integer.valueOf(ph.getProperty("vidHeight")),
								  FileHelper.readByteFile(System.getProperty("user.dir") + "/yuvFiles/" + ph.getProperty("vidName")));
		multipleN = Integer.valueOf(ph.getProperty("multipleN"));
		rangeR = Integer.valueOf(ph.getProperty("rangeR"));
		qp = Integer.valueOf(ph.getProperty("quantizationPar"));
        mvFileName = ph.getProperty("mvFileName");
        mvEntropyEncodedFileName = ph.getProperty("mvEntropyEncodedFileName");
        residualAfterMVFileName = ph.getProperty("residualAfterMVFileName");
        residualBeforeMVFileName = ph.getProperty("residualBeforeMVFileName");
        residualFileName = ph.getProperty("residualFileName");
        qtcFileName = ph.getProperty("qtcFileName");
        qtcEntropyEncodedFileName = ph.getProperty("qtcEntropyEncodedFileName");
        qtcDiagOrderedFileName = ph.getProperty("qtcDiagOrderedFileName");
        qtcRleEncodedFileName = ph.getProperty("qtcRleEncodedFileName");
		reconstructedFileName = ph.getProperty("encReconstructedFileName");
		I_Period = Integer.valueOf(ph.getProperty("I_Period"));
		setPredictor();
		currFrame = new byte[width * height];
	}

	public byte[] addPaddingToYOnly(int origWidth, int origHeight, byte[] yOnly) {
		byte[] newYOnly = new byte[width * height * frameLength];
		for (int i = 0; i < frameLength; i++) {
			for (int y = 0; y < height; y++) {
				if (y < origHeight) {
					for ( int x = 0; x < width; x++) {
						if (x < origWidth)
							newYOnly[i * width * height + y * width + x] = yOnly[i * origWidth * origHeight + y * origWidth + x];
						else
							newYOnly[i * width * height + y * width + x] = (byte)128;
					}
				}
				else {
					for ( int x = 0; x < width; x++)
						newYOnly[i * width * height + y * width + x] = (byte)128;
				}
			}
		}
		return newYOnly;
	}

	public void setPredictor() {
		predictor = new byte[width * height];
		for (int i = 0; i < width * height; i++) predictor[i] = (byte) 128;
	}

	public static int unsignedByteToInt(byte b) { return (int) b & 0xFF; }

	//-------------------------------------------------------------------------
	//----------------------------MOTION ESTIMATION----------------------------
	//-------------------------------------------------------------------------

	// return an arraylist containing the motion vector of the currFrame
	public byte[] generateMV() {
		ArrayList<int[]> mv = new ArrayList<int[]>();
		for (int h1 = 0; h1 < height; h1 += blockSize) { // for each block
			for (int w1 = 0; w1 < width; w1 += blockSize) {
				int minSAD = Integer.MAX_VALUE, minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
				for (int h2 = h1 - rangeR; h2 <= h1 + rangeR; h2++) { // check SAD value with each reference block
					for (int w2 = w1 - rangeR; w2 <= w1 + rangeR; w2++) {
						// ignore blocks falling partially or totally outside the previous frame
						if (w2 >= 0 && w2 + blockSize <= width && h2 >= 0 && h2 + blockSize <= height) {
							int sad = calculateSAD(w1, h1, w2, h2);
							if (sad < minSAD) {
								minSAD = sad;
								minx = w2 - w1;
								miny = h2 - h1;
							}
							else if (sad == minSAD) {
								if ((Math.abs(w2 - w1) + Math.abs(h2 - h1) < Math.abs(minx) + Math.abs(miny))
								|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
								 	&& Math.abs(h2 - h1) < Math.abs(miny))
								|| (Math.abs(w2 - w1) + Math.abs(h2 - h1) == Math.abs(minx) + Math.abs(miny)
								 	&& Math.abs(h2 - h1) == Math.abs(miny) && Math.abs(w2 - w1) < Math.abs(minx))) {
									minx = w2 - w1;
									miny = h2 - h1;
								}
							}
						}
					}
				}
				int[] xy = new int[2];
				xy[0] = minx;
				xy[1] = miny;
				/*//for test
				if (minx != 0 || miny != 0)
					System.out.println("frame: " + frameIndex + ", block(" + w1 + ", " + h1 + "): minx = " + minx + ", miny = " + miny);
				*/
				mv.add(xy);
			}
		}
		/*
		// for test on mv encoding and decoding
		testMV = new byte[width * height / blockSize / blockSize * 2];
		int writeCount = 0;
		for (int[] item : mv) {
			testMV[writeCount] = (byte) item[0];
			writeCount++;
			testMV[writeCount] = (byte) item[1];
			writeCount++;
		}
		*/
		byte[] mvBytes = writeToMVFile(encodeMV(mv));
		return mvBytes;
	}

	// differential encoding motion vector.
	public ArrayList<int[]> encodeMV(ArrayList<int[]> mv) {
		ArrayList<int[]> diff = new ArrayList<int[]>();
		int[] prev = new int[2];
		prev[0] = 0;
		if (frameIndex % I_Period == 0) prev[1] = 1;
		else prev[1] = 0;
		for (int[] curr : mv) {
			int[] sub = new int[2];
			sub[0] = curr[0] - prev[0];
			sub[1] = curr[1] - prev[1];
			diff.add(sub);
			prev[0] = curr[0];
			prev[1] = curr[1];
		}
		return diff;
	}

	// calculate Sum of Absolute Difference (SAD) between two blocks
	public int calculateSAD(int w1, int h1, int w2, int h2) {
		int sad = 0;
		for (int k = 0; k < blockSize; k++) {
			int wOfCur = w1 + k;
			int wOfRef = w2 + k;
			for (int j = 0; j < blockSize; j++) {
				int hOfCur = h1 + j;
				int hOfRef = h2 + j;
				sad += Math.abs(unsignedByteToInt(currFrame[hOfCur * width + wOfCur])
						- unsignedByteToInt(predictor[hOfRef * width + wOfRef]));
			}
		}
		return sad;
	}

	// generate mv file, return byte[] for further use
	public byte[] writeToMVFile(ArrayList<int[]> mv) {
		byte[] mvBytes = new byte[width * height / blockSize / blockSize * 2];
		int writeCount = 0;
		for (int[] item : mv) {
			mvBytes[writeCount] = (byte) item[0];
			writeCount++;
			mvBytes[writeCount] = (byte) item[1];
			writeCount++;
		}
		if (frameIndex == 0) {
			FileHelper.writeToFile(mvBytes, mvFileName);
//			FileHelper.writeToFile(EntropyEncDec.expGolombEnc(mvBytes), this.mvEntropyEncodedFileName);
		}
		else {
			FileHelper.writeToFileAppend(mvBytes, mvFileName);
//			FileHelper.writeToFileAppend(EntropyEncDec.expGolombEnc(mvBytes), this.mvEntropyEncodedFileName);
		}
		return mvBytes;
	}



	//--------------------------------------------------------------------------------
	//----------------------------RESIDUAL BLOCK GENERATER----------------------------
	//--------------------------------------------------------------------------------

	// return the one after motion compensation for further use
	public short[] generateResidualBlock() {
		if (frameIndex % I_Period == 0) { // Intra Prediction on every I frame
            short[] residualFrame = new short[currFrame.length];
			short[] intraPredictor = new short[currFrame.length];
			this.motionvector = decodeMV(generateModeVector(intraPredictor, residualFrame)); //update intraPredicotr and generate mode vector: (0, mode1, 0, mode2 ...)

			// Write residualBlock.arb and residualBlockAfterMV3.yuv
			writeToResidualFile(residualFrame);
			return residualFrame;
        }
        else { // Inter Prediction for the rest
			short[] residualFrameAfterMV = new short[currFrame.length];
			this.motionvector = decodeMV(generateMV());
			int blockNum = 0;
			for (int row = 0; row < height; row+=blockSize) {
				for (int col = 0; col < width; col+=blockSize) {
					residualFrameAfterMV = calcResidualBlockAfterMV(residualFrameAfterMV, this.motionvector, row, col, blockNum);
					blockNum++;
				}
			}
			writeToResidualFile(residualFrameAfterMV);
			return residualFrameAfterMV;
		}
	}

	// decode motion vector
	public byte[] decodeMV(byte[] diff) {
		byte[] mv = new byte[diff.length];
		byte[] prev = new byte[2];
		prev[0] = 0;
		if (frameIndex % I_Period == 0) prev[1] = 1;
		else prev[1] = 0;
		mv[0] = (byte)(diff[0] + prev[0]);
		mv[1] = (byte)(diff[1] + prev[1]);
		for (int i = 2; i < diff.length; i+=2) {
			mv[i] = (byte)(diff[i] + mv[i - 2]);
			mv[i + 1] = (byte)(diff[i + 1] + mv[i - 1]);
		}
		/*
		// for test on mv encoding and decoding
		for (int i = 0; i < mv.length; i++) {
			if (mv[i] != testMV[i])
				System.out.println("mv error!");
		}
		*/
		return mv;
	}

	////////////////////INTRA//////////////
	/**
	 * finds mode for each block and calculates residual frame
	 * @param intraPredictorFrame intra predictor frame
	 * @param residualFrame residual frame
	 * @return mode vector in byte sequence
	 */
	public byte[] generateModeVector(short[] intraPredictorFrame, short[] residualFrame) {
		/*
		intra predictor block = calculated from reconstructed block (128 for absent samples)
		residual block = current block - intra predictor block
	 	transformed residual block = residual block -> transf -> quant -> rescaling -> inv trans
	 	reconstructed block = transformed residual block + intra predictor block
		*/
		ArrayList<int[]> modeVector = new ArrayList<int[]>();
        short[] reconstructedFrame = new short[width*height];
        int[] tranfFrame = new int[width*height];
        int[] qauntFrame = new int[width*height];
        int[] rescalFrame = new int[width*height];
        short[] transformedFrame = new short[width*height];

		// Block Based Operation
		for (int h = 0; h < height; h += blockSize) {
			for (int w = 0; w < width; w += blockSize) {
				int[] xy = new int[2];
				xy[0] = 0; // (x = 0, y = mode)

				//gets mode for the block and updates intra predictor frame and residual frame
				xy[1] = getMode(reconstructedFrame, intraPredictorFrame, residualFrame, h, w); 
                modeVector.add(xy);

				// transforms the residual frame
                transfOnBlock(residualFrame, tranfFrame, w, h);
                quantOnBlock(tranfFrame, qauntFrame, w, h);
                rescaleOnBlock(qauntFrame, rescalFrame, w, h);
				invTransfOnBlock(rescalFrame, transformedFrame, w, h);
				
				// updates reconstructed frame
                buildTransformedReconstructedOnBlock(reconstructedFrame, transformedFrame, intraPredictorFrame, h, w);
			}
		}
		byte[] modeVectorBytes = writeToMVFile(encodeMV(modeVector));
		return modeVectorBytes;
	}

	/**
	 * updates current block in reconstructed frame using the transformed residual frame and intra predictor frame
	 * @param reconstructed reconstructed frame
	 * @param transformedResidual transformed residual frame(residual -> transform -> quant -> rescaling -> inv trans)
	 * @param intraPredicotr intra predictor frame
	 * @param hIndex y coordinate of top left element in current block
	 * @param wIndex x coordinate of top left element in current block
	 */
	public void buildTransformedReconstructedOnBlock(short[] reconstructed, short[] transformedResidual, short[] intraPredicotr, int hIndex, int wIndex) {
        for (int h = hIndex; h < hIndex + blockSize; h++) {
            for (int w = wIndex; w < wIndex + blockSize; w++) {
                reconstructed[h*width+w] = (byte) ((transformedResidual[h*width+w]) + (intraPredicotr[h*width+w]));
            }
        }
	}
	
	 /***
     * update intraPredictor (every ixi block in an I frame is encoded using intra prediction) & residual frame
	 * @param reconstructed reconstructed frame 
     * @param intraPredictor intra prediction compensated predictor frame (elements for corresponding block get updated)
	 * @param residualFrame residual frame
     * @param hIndex y coordinate of top left element of a block
     * @param wIndex x coordinate of top left element of a block
     * @return mode of the block: 0 for horizontal intra, 1 for vertical intra
     */
    public int getMode(short[] reconstructed, short[] intraPredictor, short[] residualFrame, int hIndex, int wIndex) {
        int horizontalSAD = 0, verticalSAD = 0, index = 0;
        int mode;
        short[] horizontalPredictor = new short[blockSize*blockSize];
        short[] verticalPredictor = new short[blockSize*blockSize];

		// Block based operation
		// finds SAD for horizontal and vertical for current block 
        for (int h = hIndex; h < hIndex + blockSize; h++) {
            for (int w = wIndex; w < wIndex + blockSize; w++) {
                // Horizontal Intra
                if (wIndex == 0) // uses 128 for absent samples at left frame border
                    horizontalPredictor[index] = (byte) 128;
                else // uses left border of the block
                    horizontalPredictor[index] = reconstructed[h*width + wIndex - 1];

                horizontalSAD += Math.abs((horizontalPredictor[index])
                                        - (currFrame[h*width + w]));

                // Vertical Intra
                if (hIndex == 0) // uses 128 for absent samples at top frame border
                    verticalPredictor[index] = (byte) 128;
                else    // uses top border of the block
                    verticalPredictor[index] = reconstructed[(hIndex-1)*width + w];

                verticalSAD += Math.abs((verticalPredictor[index])
                                      - (currFrame[h*width + w]));
                index++;
            }
        }

        // determines the mode by comparing SAD values found above
        if (verticalSAD < horizontalSAD) mode = 1;  // mode that gives lowest SAD cost is selected (1 -> vertical)
        else mode = 0;  // (0 -> horizontal)

        // updates intra predictor according to the mode
        index = 0;
        for (int h = hIndex; h < hIndex + blockSize; h++) {
            for (int w = wIndex; w < wIndex + blockSize; w++) {
                if (mode == 0) intraPredictor[h*width + w] = horizontalPredictor[index++];
				else intraPredictor[h*width + w] = verticalPredictor[index++];
				
				// updates residual block in residual frame 
				residualFrame[h*width+w] = (short) (unsignedByteToInt(currFrame[h*width+w]) - (intraPredictor[h*width+w]));
            }
        }
        return mode;
    }

	/**
	 * Calculate the residual block after motion compensation
	 * @param rowCoordinate, Y component of top left pixel of block
	 * @param colCoordinate, X component of top left pixel of block
	 * @param blockNum, block index so we know which motion vector value to use
	 */
	public short[] calcResidualBlockAfterMV(short[] residualFrame, byte[] motionVector, int rowCoordinate, int colCoordinate, int blockNum) {

		for (int i = rowCoordinate; i < rowCoordinate+blockSize; i++){
            for (int j = colCoordinate; j < colCoordinate+blockSize; j++) {
                int curFrameBlkR = i; // Row coordinate of current frame block's top left pixel
                int curFrameBlkC = j; // Col coordinate of current frame block's top left pixel
                int predictedFrameBlkR = curFrameBlkR + (int)motionVector[blockNum * 2 + 1]; // Assuming Y motion is movement in vertical row direction
                int predictedFrameBlkC = curFrameBlkC + (int)motionVector[blockNum * 2]; // Assuming X motion is movement in horizontal direction

            	// Need to check if I'm going outside of the frame
            	if (curFrameBlkR >= height || curFrameBlkR < 0 ||
            		curFrameBlkC >= width || curFrameBlkC < 0 ||
            		predictedFrameBlkR >= height || predictedFrameBlkR < 0 ||
            		predictedFrameBlkC >= width || predictedFrameBlkC < 0) {
            		System.out.println(curFrameBlkR);
            		System.out.println(curFrameBlkC);
            		System.out.println(predictedFrameBlkR);
            		System.out.println(predictedFrameBlkC);
            		if (i == rowCoordinate & j == colCoordinate) {
            			// For the first loop, predictedFrame is starting point of the ixi block,
            			// this should never be out of bounds. if it is, then our motion vector is wrong
            			// Special check on motion vector, if motion vector is taking me out of bounds then this should fail
                        System.out.println("Motion Vector points to out of bounds frame. Failing program.");
                        System.exit(1);
                    }
            		// Outside of frame.
            		continue;
            	}
            	int tempRes = (unsignedByteToInt(currFrame[curFrameBlkR * width + curFrameBlkC]) - unsignedByteToInt(predictor[predictedFrameBlkR * width + predictedFrameBlkC]));
	    		/*if (tempRes < -128 || tempRes > 127) {
	    			System.out.println("residual value " + tempRes + " cannot be converted to byte without losing bits");
	    			System.exit(1);
	    		}*/
            	residualFrame[i * width + j] = (short)tempRes;
            }
        }
		return residualFrame;
	}

	// Helper function to turn pixel index i to blocknum
	public int getBlockNumForI(int pixel_i) {

		// first get row and col of pixel
		int row = pixel_i/this.width;
		int col = pixel_i % this.width;

		int block_row = row/this.blockSize;
		int block_col = col/this.blockSize;

		int block_num = block_row*(this.width/this.blockSize) + block_col;
		return block_num;

	}

	// write to both residualBlockAfterMV3.yuv and residualBlockBeforeMV3.yuv
	public void writeToResidualFile(short[] residualFrameAfterMV) {
		if (frameIndex == 0) {
			FileHelper.writeToFile(residualFrameAfterMV, residualFileName);
		}
		else {
			FileHelper.writeToFileAppend(residualFrameAfterMV, residualFileName);
		}
	}

	//------------------------------------------------------------------------------------
	//----------------------------RESIDUAL BLOCK APPROXIMATION----------------------------
	//------------------------------------------------------------------------------------

	//----------------------------TRANSFORM----------------------------
	/**
	 * Apply transform on one frame
	 * @param f the residual value of one frame after padding
	 * @return the transformed coeff of one frame after padding
	 */
	public int[] transfOnFrame(short[] f) {

		int[] fTransf = new int[width * height];
		for (int x = 0; x < width; x += blockSize) {
			for (int y = 0; y < height; y += blockSize) // for every block
				transfOnBlock(f, fTransf, x, y);
		}
		return fTransf;
	}

	/**
	 * apply transform on each block.
	 * @param f, the residual value of the frame
	 * @param fTransf, the corresponding transform coefficient
	 * @param col, X component of top left pixel of block
	 * @param row, Y component of top left pixel of block
	 */
	public void transfOnBlock(short[] f, int[] fTransf, int col, int row) {
		int N = blockSize;
		for (int k1 = 0; k1 < N; k1++) {
			for (int k2 = 0; k2 < N; k2++) {
				double sum = 0;
				for (int n1 = 0; n1 < N; n1++) {
					for (int n2 = 0; n2 < N; n2++) {
						double cos1 = Math.cos(((2 * n1 + 1) * k1 * Math.PI) / (2 * N));
						double cos2 = Math.cos(((2 * n2 + 1) * k2 * Math.PI) / (2 * N));
						sum = sum + ((int)f[(n1 + row) * width + (n2 + col)]) * cos1 * cos2;
						// for test on original file, use:
						// sum = sum + unsignedByteToInt(f[(n1 + row) * width + (n2 + col)]) * cos1 * cos2;
					}
				}
				sum = sum / N;
				if (k1 != 0 && k2 != 0)
					fTransf[(k1 + row) * width + (k2 + col)] = (int)Math.round(2 * sum);
				else if (k2 != 0 || k1 != 0)
					fTransf[(k1 + row) * width + (k2 + col)] = (int)Math.round(Math.sqrt(2) * sum);
				else
					fTransf[(k1 + row) * width + (k2 + col)] = (int)Math.round(sum);
				//if (col == 224 && row == 168 && k1 == 0 && k2 == 0)
				//	System.out.println("(" + (k2 + col) + ", " + (k1 + row) + "): " + fTransf[k1 + row * width + (k2 + col)]);
			}
		}
	}

	//----------------------------QUANTIZATION----------------------------
	// Apply quantization on the current frame;
	/**
	 * Quantize on frame
	 * @param tcoeff, transformed coefficients
	 * @return quantized transformed coefficients.
	 */
	public int[] quantOnFrame(int[] tcoeff) {

		int[] qtc = new int[width * height];
		for (int y = 0; y < height; y += blockSize) {
			for ( int x = 0; x < width; x += blockSize) {
				quantOnBlock(tcoeff, qtc, x, y);
			}
		}
		return qtc;
	}

	/**
	 *  Apply quantization on one block
	 * @param tcoeff, transform coefficient
	 * @param qtc, calculated qtc value
	 * @param col, X component of top left pixel of block
	 * @param row, Y component of top left pixel of block
	 */
	public void quantOnBlock(int[] tcoeff, int[] qtc, int col, int row) {
		int i = blockSize;
		for (int y = row; y < row + i; y++) {
			for (int x = col; x < col + i; x++) {
				qtc[y * width + x] = (int)Math.round(tcoeff[y * width + x] / (double)findQ(x - col, y - row));
				//if (col == 224 && row == 168)
				//	System.out.println("(" + x + ", " + y + "): " + qtc[y * width + x] + ", q = " + findQ(x - col, y - row));
			}
		}
	}

	// helper function to find the Q matrix
	public int findQ(int x, int y) {
		if (qp < 0 || qp > log2(blockSize) + 7) {
			System.out.println("invalid quantization parameter: " + qp);
			System.exit(1);
		}
		if (x + y < blockSize - 1)
			return (int)Math.pow(2, qp);
		else if (x + y == blockSize - 1)
			return (int)Math.pow(2, qp + 1);
		else // (x + y > blockSize - 1)
			return (int)Math.pow(2, qp + 2);
	}

	/**
	 * Helper function to write to the diagonal re-ordered QTC
	 * @param qtcEnc
	 */
	public void writeToQtcDiagOrderedFile(short[] qtc) {
		if (frameIndex == 0)
			FileHelper.writeToFile(qtc, this.qtcDiagOrderedFileName);
		else
			FileHelper.writeToFileAppend(qtc, this.qtcDiagOrderedFileName);
	}

	/**
	 * Helper func to write raw Quantized TC values
	 * @param qtcEnc
	 */
	public void writeToQtcFile(short[] qtc) {
		if (frameIndex == 0)
			FileHelper.writeToFile(qtc, this.qtcFileName);
		else
			FileHelper.writeToFileAppend(qtc, this.qtcFileName);
	}

	// helper function to do log2()
	public int log2(int x) {
		return (int) (Math.log(x) / Math.log(2));
	}

	//-------------------------------------------------------------------------------------
	//----------------------------RECONSTRUCTED FRAME GENERATOR----------------------------
	//-------------------------------------------------------------------------------------

	//----------------------------RESCALING----------------------------
	// Apply rescaling on the current frame;
	/**
	 * Rescale on frame.
	 * @param qtc, the quantized transformed coefficients.
	 * @return rescaled transform coefficients.
	 */
	public int[] rescaleOnFrame(int [] qtc) {

		int[] tcoeff = new int[width * height];
		for (int y = 0; y < height; y += blockSize) {
			for ( int x = 0; x < width; x += blockSize) {
				rescaleOnBlock(qtc, tcoeff, x, y);
			}
		}
		return tcoeff;
	}

	/**
	 *  Apply rescaling on one block
	 * @param tcoeff, transform coefficient
	 * @param qtc, calculated qtc value
	 * @param col, X component of top left pixel of block
	 * @param row, Y component of top left pixel of block
	 */
	public void rescaleOnBlock(int[] qtc, int[] tcoeff, int col, int row) {
		int i = blockSize;
		for (int y = row; y < row + i; y++) {
			for (int x = col; x < col + i; x++) {
				tcoeff[y * width + x] = qtc[y * width + x] * findQ(x - col, y - row);
				//System.out.println("(" + x + ", " + y + "): " + tcoeff[y * width + x] + ", q = " + findQ(x - col, y - row));
			}
		}
	}

	//----------------------------INVERSE TRANSFORM----------------------------
	/**
	 * Apply inverse transform on one frame
	 * @param f the residual value of one frame after padding
	 * @return the transformed coefficient of one frame after padding
	 */
	public short[] invTransfOnFrame(int[] fTransf) {

		short[] f = new short[width * height];
		for (int x = 0; x < width; x += blockSize) {
			for (int y = 0; y < height; y += blockSize) // for every block
				invTransfOnBlock(fTransf, f, x, y);
		}

		return f;
	}

	/**
	 * apply inverse transform on each block.
	 * @param fTransf, the corresponding transform coefficient
	 * @param f, the residual value of the frame
	 * @param col, X component of top left pixel of block
	 * @param row, Y component of top left pixel of block
	 */
	public void invTransfOnBlock(int[] fTransf, short[] f, int col, int row) {
		int N = blockSize;
		for (int n1 = 0; n1 < N; n1++) {
			for (int n2 = 0; n2 < N; n2++) {
				double sum = 0;
				for (int k1 = 0; k1 < N; k1++) {
					for (int k2 = 0; k2 < N; k2++) {
						double val = 0;
						double cos1 = Math.cos(((2 * n1 + 1) * k1 * Math.PI) / (2 * N));
						double cos2 = Math.cos(((2 * n2 + 1) * k2 * Math.PI) / (2 * N));
						val = fTransf[(k1 + row) * width + k2 + col] * cos1 * cos2 / N;
						//if (fTransf[(k1 + row) * width + k2 + col] != 0)
						//	System.out.println("(" + (n2 + col) + ", " + (n1 + row) + "): " + fTransf[(k1 + row) * width + k2 + col]);

						if (k1 != 0)
							val *= Math.sqrt(2);
						if (k2 != 0)
							val *= Math.sqrt(2);
						sum += val;
					}
				}	    
				/*
				if (Math.round(sum) < -128 || Math.round(sum) > 127) {
	    			System.out.println("reconstructed residual value " + Math.round(sum) + " cannot be converted to byte without losing bits");
	    			System.exit(1);
	    		}*/
				f[(n1 + row) * width + n2 + col] = (short) Math.round(sum); // signed
				//System.out.println("(" + (n2 + col) + ", " + (n1 + row) + "): " + unsignedByteToInt(f[(n1 + row) * width + n2 + col]));
			}
		}
	}

	public void writeToTest4a(byte[] reconstructedFrame) {
		if (frameIndex == 0)
			FileHelper.writeToFile(reconstructedFrame, test4aFileName);
		else
			FileHelper.writeToFileAppend(reconstructedFrame, test4aFileName);
	}

	// Older generate the reconstructed frame carried over from Execise 3
	/*
	public byte[] generateReconstructedFrame() {
		int oneFrameSize = width * height;
        byte[] residual = generateResidualBlock();
        byte[] reconstructedFrame = new byte[currFrame.length];
        for (int i = 0; i < oneFrameSize; i++) {
        	int blockNum = this.getBlockNumForI(i);
        	int motion_x = (int)(this.motionvector[2*blockNum]);
        	int motion_y = (int)(this.motionvector[2*blockNum +1]);
        	int predictor_ind = i + motion_x + (motion_y * this.width);
            reconstructedFrame[i] = (byte)(unsignedByteToInt(predictor[predictor_ind]) + (int)(residual[i]));
        }
        writeToReconstructedFile(reconstructedFrame);
        return reconstructedFrame;
	}
	*/
	public byte[] generateReconstructedFrame() {
		int oneFrameSize = width * height;
		short[] f = generateResidualBlock();
		int[] tcoeff = transfOnFrame(f);
		int[] qtc = quantOnFrame(tcoeff);
		// This is needed on decoder side
		int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
		writeToQtcFile(Helper.intArrToShortArr(qtc));
		writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded));
		int[] fTransf = rescaleOnFrame(qtc);
		short[] transformedResidual = invTransfOnFrame(fTransf);
		byte[] reconstructedFrame = new byte[currFrame.length];
		byte[] intra_Predictor = new byte[currFrame.length];
        for (int i = 0; i < oneFrameSize; i++) {
			if (frameIndex % I_Period == 0) {// Intra
				int w = i%width;
				int h = i/width;
				if (w % blockSize == 0 && h % blockSize == 0) {
					int blockNum = this.getBlockNumForI(i);
					int mode = (int) (this.motionvector[2*blockNum + 1]);
					generateIntra(reconstructedFrame, intra_Predictor, transformedResidual, w, h, mode);
				}
				// TESTING
				// reconstructedFrame[i] = (byte) (unsignedByteToInt(this.intraPredictor[i]) + (int)(transformedResidual[i]));
			}
			else {
				int blockNum = this.getBlockNumForI(i);
				int motion_x = (int)(this.motionvector[2*blockNum]);
				int motion_y = (int)(this.motionvector[2*blockNum +1]);
				int predictor_ind = i + motion_x + (motion_y * this.width);
				reconstructedFrame[i] = (byte)(unsignedByteToInt(predictor[predictor_ind]) + (int)(transformedResidual[i]));
			}
        }
        writeToReconstructedFile(reconstructedFrame);
        return reconstructedFrame;
	}

	public void generateIntra(byte[] reconstructedFrame, byte[] intra_Predictor, short[] transformedResidual, int wIndex, int hIndex, int mode) {
		for (int h = hIndex; h < hIndex + blockSize; h++) {
            for (int w = wIndex; w < wIndex + blockSize; w++) {
                if (mode == 0) {
					if (wIndex == 0) intra_Predictor[h*width + w] = (byte) 128;
					else intra_Predictor[h*width + w] = reconstructedFrame[h*width + wIndex - 1];
				}
                else {
					if (hIndex == 0) intra_Predictor[h*width + w] = (byte) 128;
					else intra_Predictor[h*width + w] = reconstructedFrame[(hIndex-1)*width + w];
				}
				reconstructedFrame[h*width + w] = (byte) (unsignedByteToInt(intra_Predictor[h*width + w]) + (int)(transformedResidual[h*width + w]));
            }
		}
	}

	// write to reconstructedFile.yuv
	public void writeToReconstructedFile(byte[] reconstructedFrame) {
		if (frameIndex == 0)
			FileHelper.writeToFile(reconstructedFrame, reconstructedFileName);
		else
			FileHelper.writeToFileAppend(reconstructedFrame, reconstructedFileName);
	}


	//-------------------------------------------------------------------------
	//----------------------------ENCODER GENERATOR----------------------------
	//-------------------------------------------------------------------------

	// looping through all frames and do encoding
	public void startEncoding() {
		long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
		
		for (int i = 0; i < frameLength; i++) {
			 	System.out.println("Working on Frame # " + frameIndex);
				frameIndex = i;
	            setCurr();
				byte[] reconstructed = generateReconstructedFrame();
				predictor = reconstructed;
		}
		long generateMVAndResTime = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("Time to generate MV and Residual(seconds) = " + ((generateMVAndResTime - startTimeMs)/(double)1000));
		
		//Encoding is done, now we can write exp-golomb encoded files
		PropertyHelper ph = new PropertyHelper();
		ProcessMV.processmv();
		byte[] mv = EntropyEncDec.expGolombEnc(FileHelper.readByteFile(System.getProperty("user.dir") + "/output/" + ph.getProperty("mvFileName")));
		FileHelper.writeToFile(mv, this.mvEntropyEncodedFileName);
		long entEncMVTime = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("Time to Entropy Encode MV (seconds) = " + (double)((entEncMVTime - generateMVAndResTime)/(double)1000));

		// for QTC, we need to do 2 more steps, RLE and exp-golomb
		int[] qtcRle = EntropyEncDec.runLevelEnc(Helper.shortArrToIntArr(FileHelper.readShortFile(System.getProperty("user.dir") + "/output/" + this.qtcDiagOrderedFileName)));
		long rleEncQTCTime = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("Time to RunLevel Encode QTC (seconds) = " + (double)((rleEncQTCTime - entEncMVTime)/(double)1000));
		
		byte[] encoded = EntropyEncDec.expGolombEnc(qtcRle);
		FileHelper.writeToFile(encoded, this.qtcEntropyEncodedFileName);
		
		long entEncQTCTime = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("Time to Entropy Encode QTC (seconds) = " + (double)((entEncQTCTime - rleEncQTCTime)/(double)1000));
		System.out.println("Total Encoding time (seconds) = " + (double)((entEncQTCTime - startTimeMs)/ (double)1000));
	}

	// put the current frame y only value into currFrame
	public void setCurr() {
		for (int i = 0; i < width * height; i++) {
			currFrame[i] = yOnly[frameIndex * width * height + i];
		}
	}

	// test for tranform, quantization, rescaling, inverse transform on the original y-only file
	// need to change transfOnFrame, transfOnBlock, invTransfOnFrame to test version
	/*
	public void test4a() {
		for (int i = 0; i < 10; i++) {
			frameIndex = i;
			setCurr();
			byte[] f = generateResidualBlock();
			int[] tcoeff = transfOnFrame(f);
			// for test on original file
			// int[] tcoeff = transfOnFrame(this.currFrame);
			int[] qtc = quantOnFrame(tcoeff);
			int[] fTransf = rescaleOnFrame(qtc);
			byte[] tcoeffInv = invTransfOnFrame(fTransf);
			// for test on original
			// writeToTest4a(tcoeffInv);
		}
	}

	public void test4b() {
        for (int i = 0; i < frameLength; i++) {
			frameIndex = i;
            setCurr();
			byte[] reconstructed = generateReconstructedFrame2();
			predictor = reconstructed;
		}
	}
	*/

	public static void main(String[] args) {
		EncoderExercise4 en = new EncoderExercise4();
//		 en.test4a();
		en.startEncoding();
	}


}

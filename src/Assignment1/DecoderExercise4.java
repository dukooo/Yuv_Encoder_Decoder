package Assignment1;

import java.time.ZonedDateTime;

public class DecoderExercise4 {
    String qtcEntropyEncodedFileName;
    String mvEntropyEncodedFileName;
    String decodedFileName;
    int width, height, frameLength, blockSize, multipleN, rangeR, qp, I_Period;
    int frameIndex;
    int[] qtc;
    byte[] refFrame;
    byte[] mv;

    public DecoderExercise4() {
        PropertyHelper ph = new PropertyHelper();
        frameLength = Integer.valueOf(ph.getProperty("frameCount"));
		blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
        height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
        multipleN = Integer.valueOf(ph.getProperty("multipleN"));
		rangeR = Integer.valueOf(ph.getProperty("rangeR"));
        qp = Integer.valueOf(ph.getProperty("quantizationPar"));
        I_Period = Integer.valueOf(ph.getProperty("I_Period"));
        decodedFileName = ph.getProperty("decReconstructedFileName");

        qtcEntropyEncodedFileName = ph.getProperty("qtcEntropyEncodedFileName");
        mvEntropyEncodedFileName = ph.getProperty("mvEntropyEncodedFileName");
    }
    
    /**
     * decodes entropy encoded mv and qtc files and store them in arrays
     */
    public void decodeMV_QTC() {
        String outputDir = System.getProperty("user.dir") + "/output/";

        // Reverse entropy encoded mv file
        byte[] mvEntropyEncoded = FileHelper.readByteFile(outputDir + mvEntropyEncodedFileName); // reads mvEntropyEncodedFileName in bytes
        int[] mvEntropyDecoded = EntropyEncDec.expGolombDec(mvEntropyEncoded); // decodes entropy encoded mv sequence
        //this.mv = Helper.intArrToByteArr(mvEntropyDecoded); // converts int[] decoded mv to byte[]
        this.mv = ProcessMV.reverseProcessmv(Helper.intArrToByteArr(mvEntropyDecoded)); // converts int[] decoded mv to byte[]
    
        // Reverse entropy encoded / diag ordered qtc file
        byte[] qtcEntropyEncoded = FileHelper.readByteFile(outputDir + qtcEntropyEncodedFileName); // reads qtcEntropyEncodedFileName in bytes
     
        int[] qtcExpGolombDecoded = EntropyEncDec.expGolombDec(qtcEntropyEncoded); // decodes entropy encoded qtc sequence
    
        int[] qtcRLE_Decoded = EntropyEncDec.runLevelDec(qtcExpGolombDecoded, width, height, frameLength); // decodes RLEed qtc sequence
    
        this.qtc = new int[width*height*frameLength];
        for (int frame = 0; frame < frameLength; frame++) {
            int[] RLE_decodedFrame = new int[width*height];
            for (int element = 0; element < RLE_decodedFrame.length; element++) 
                RLE_decodedFrame[element] = qtcRLE_Decoded[frame * width * height + element];
            int[] qtcDiagReversed = EntropyEncDec.diagReorderReverse(RLE_decodedFrame, width, height); // reverses Diag Reordered qtc sequence (frame by frame)

            for (int i = 0; i < qtcDiagReversed.length; i++) qtc[frame * width * height + i] = qtcDiagReversed[i]; // copies decoded qtc of one frame to qtc array for the entire video
        }
    
        // FileHelper.writeToFile(Helper.intArrToByteArr(qtc), "DECODER_qtc.qtc");
        FileHelper.writeToFile(Helper.intArrToShortArr(qtc), "DECODER_qtc.qtc");
        FileHelper.writeToFile(mv, "DECODER_mv.mv");
    }

    public void decodeMV_QTC_usingFiles() {
        String outputDir = System.getProperty("user.dir") + "/output/";
        this.qtc = Helper.byteArrToIntArr(FileHelper.readByteFile(outputDir + "DECODER_qtc.qtc"));
        this.mv = FileHelper.readByteFile(outputDir + "DECODER_mv.mv");

        // this.qtc = Helper.byteArrToIntArr(FileHelper.readByteFile(outputDir + "qtc.qtc"));
        // this.mv = FileHelper.readByteFile(outputDir + "motionVector3.mv");
    }
    
    public void startDecoding() {
        int oneFrameSize = width*height;
        refFrame = new byte[oneFrameSize];
        byte[] reconstructedFrame = new byte[oneFrameSize];
        this.decodeMV_QTC(); // decod
        // this.decodeMV_QTC_usingFiles();
        System.out.println("MV & QCT Decoded");
        for (int frame = 0; frame < frameLength; frame++) {
            frameIndex = frame;
            if (frameIndex == 0) 
                for (int i = 0; i < oneFrameSize; i ++) refFrame[i] = (byte) 128; //first reference frame is filled with 128
            else 
                for (int i = 0; i < oneFrameSize; i ++) refFrame[i] = reconstructedFrame[i]; //use reconstructed frame as ref frame
            
            reconstructedFrame = getReconstructedFrame(); // generates reconstructed frame
            System.out.println("Frame " + frame + " Reconstructed");
        }
    }

    /**
     * applies rescaling and inv transform to qtc coeff and gerates predictor to create reconstructed frame
     * @return reconstructed frame
     */
    public byte[] getReconstructedFrame() {
        int oneFrameSize = width * height;
        int numbOfBlock_perFrame = width*height/blockSize/blockSize;
        byte[] reconstructedFrame = new byte[oneFrameSize];
        int[] qtc_oneFrame = new int[oneFrameSize]; //qtc for current frame
        byte[] mv_oneFrame = new byte[numbOfBlock_perFrame*2]; //mv for current frame
        byte[] intra_Predictor = new byte[oneFrameSize];
        // Frame Based Operation - get qtc and mv info for corresponding frame
        for (int element = 0; element < oneFrameSize; element++) {
            qtc_oneFrame[element] = qtc[frameIndex*oneFrameSize + element];
        }
        for (int block = 0; block < numbOfBlock_perFrame; block++) {
            mv_oneFrame[block*2] = mv[frameIndex*numbOfBlock_perFrame*2 + block*2];
            mv_oneFrame[block*2+1] = mv[frameIndex*numbOfBlock_perFrame*2 + block*2+1];
        }

        int[] fTransf = rescaleOnFrame(qtc_oneFrame); // applies rescaling
        byte[] transformedResidual = invTransfOnFrame(fTransf); // applies inverse transformation
        mv_oneFrame = decodeMV(mv_oneFrame); // differentially decodes mv
        
        // Element (pixel) based operation
        for (int i = 0; i < oneFrameSize; i++) {
            int blockNum = this.getBlockNumForI(i);
            if (frameIndex % I_Period == 0) { // Intra
                int w = i%width;
				int h = i/width;
				if (w % blockSize == 0 && h % blockSize == 0) { //block based operation
					int mode = (int) (mv_oneFrame[2*blockNum + 1]);
                    generateIntra(reconstructedFrame, intra_Predictor, transformedResidual, w, h, mode);
				}
            }
            else { // Inter
				int motion_x = mv_oneFrame[2*blockNum];
				int motion_y = mv_oneFrame[2*blockNum +1];
                int predictor_ind = i + motion_x + (motion_y * width); 
				reconstructedFrame[i] = (byte)(unsignedByteToInt(refFrame[predictor_ind]) + (int)(transformedResidual[i])); // inter predictor + residual
            }
        }
        writeToReconstructedFile(reconstructedFrame);
        return reconstructedFrame;
    }

    public void generateIntra(byte[] reconstructedFrame, byte[] intra_Predictor, byte[] transformedResidual, int wIndex, int hIndex, int mode) {
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
				reconstructedFrame[h*width + w] = (byte) (unsignedByteToInt(intra_Predictor[h*width + w]) + unsignedByteToInt(transformedResidual[h*width + w]));
            }
        }
	}
        
    // TRANSFORMATION
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
    /**
	 * Apply inverse transform on one frame
	 * @param f the residual value of one frame after padding
	 * @return the transformed coefficient of one frame after padding
	 */
	public byte[] invTransfOnFrame(int[] fTransf) {
		byte[] f = new byte[width * height];
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
	public void invTransfOnBlock(int[] fTransf, byte[] f, int col, int row) {
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
				f[(n1 + row) * width + n2 + col] = (byte) Math.round(sum); // signed
				//System.out.println("(" + (n2 + col) + ", " + (n1 + row) + "): " + unsignedByteToInt(f[(n1 + row) * width + n2 + col]));
			}
		}
	}

    // HELPER FUNCTIONS
    public static int unsignedByteToInt(byte b) { return (int) b & 0xFF; }
    public int getBlockNumForI(int pixel_i) {

		// first get row and col of pixel
		int row = pixel_i/this.width;
		int col = pixel_i % this.width;

		int block_row = row/this.blockSize;
		int block_col = col/this.blockSize;

		int block_num = block_row*(this.width/this.blockSize) + block_col;
		return block_num;

	}
    public void writeToReconstructedFile(byte[] reconstructedFrame) {
		if (frameIndex == 0)
			FileHelper.writeToFile(reconstructedFrame, this.decodedFileName);
		else
			FileHelper.writeToFileAppend(reconstructedFrame, this.decodedFileName);
	}
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
    public int log2(int x) {
		return (int) (Math.log(x) / Math.log(2));
    }

    /**
     * Differential decode differential encoded mv sequence
     * @param diff differential encoded mv
     * @return differential decoded mv
     */
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
		return mv;
	}

    public static void main(String[] args) {
        DecoderExercise4 decoder = new DecoderExercise4();
        // decoder.decodeMV_QTC();
        long startTimeMs = ZonedDateTime.now().toInstant().toEpochMilli();
        decoder.startDecoding();
		long endTimeMs  = ZonedDateTime.now().toInstant().toEpochMilli();
		System.out.println("Decoding time (ms) = " + (endTimeMs - startTimeMs));
    }
}
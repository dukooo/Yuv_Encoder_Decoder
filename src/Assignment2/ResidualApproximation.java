package Assignment2;

public class ResidualApproximation {

	int blocksize, width, height, qp;
	
	public ResidualApproximation() {
		PropertyHelper ph = new PropertyHelper();
		blocksize = Integer.valueOf(ph.getProperty("blkSizeI"));
		width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blocksize);
		height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blocksize);
		qp = Integer.valueOf(ph.getProperty("quantizationPar"));
	}
	
	public void setQP(int qp) {
		this.qp = qp;
	}
	
	public int getQP() {
		return this.qp;
	}
	
	public int[] transfOnBlock(short[] f, int[] fTransf, int col, int row, int bs) {
		int N = bs;
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
		return fTransf;
	}
	
	public int[] quantOnBlock(int[] tcoeff, int[] qtc, int col, int row, int bs) {
		int i = bs;
		int[] blockqtc = new int[bs * bs];
		int index = 0;
		for (int y = row; y < row + i; y++) {
			for (int x = col; x < col + i; x++) {
				qtc[y * width + x] = (int)Math.round(tcoeff[y * width + x] / (double)findQ(x - col, y - row, bs));
				blockqtc[index] = qtc[y * width + x];
				index++;
			}
		}
		// TODO: entropy encoding blockqtc, diagReorder?
		byte[] qtcRle = new byte[0];
		// write it to qtcBlock.qtc or qtcBrokenBlock.qtc
		/*
		if (blocksize == bs)
			FileHelper.writeToFile(qtcRle, "qtcBlock.qtc");
		else {
			if (col % blocksize == 0 && row % blocksize == 0)
				FileHelper.writeToFile(qtcRle, "qtcBrokenBlock.qtc"); 
			else 
				//TODO: find a way to append qtcBrokenBlock.qtc that let us know the bitcount for each sub-block
				FileHelper.writeToFileAppend(qtcRle, "qtcBrokenBlock.qtc"); 
		}
		*/
		return qtc;
	}
	

	// helper function to find the Q matrix
	public int findQ(int x, int y, int bs) {
		if (qp < 0 || qp > log2(bs) + 7) {
			System.out.println("invalid quantization parameter: " + qp);
			System.exit(1);
		}
		if (x + y < bs - 1)
			return (int)Math.pow(2, qp);
		else if (x + y == bs - 1)
			return (int)Math.pow(2, qp + 1);
		else // (x + y > blockSize - 1)
			return (int)Math.pow(2, qp + 2);
	}
	
	public int log2(int x) {
		return (int) (Math.log(x) / Math.log(2));
	}
	
	public int[] rescaleOnBlock(int[] qtc, int[] tcoeff, int col, int row, int bs) {
		int i = bs;
		for (int y = row; y < row + i; y++) {
			for (int x = col; x < col + i; x++) {
				tcoeff[y * width + x] = qtc[y * width + x] * findQ(x - col, y - row, bs);
				//System.out.println("(" + x + ", " + y + "): " + tcoeff[y * width + x] + ", q = " + findQ(x - col, y - row));
			}
		}
		return tcoeff;
	}
	
	public short[] invTransfOnBlock(int[] fTransf, short[] f, int col, int row, int bs) {
		int N = bs;
		for (int n1 = 0; n1 < N; n1++) {
			for (int n2 = 0; n2 < N; n2++) {
				double sum = 0;
				for (int k1 = 0; k1 < N; k1++) {
					for (int k2 = 0; k2 < N; k2++) {
						double val = 0;
						double cos1 = Math.cos(((2 * n1 + 1) * k1 * Math.PI) / (2 * N));
						double cos2 = Math.cos(((2 * n2 + 1) * k2 * Math.PI) / (2 * N));
						val = fTransf[(k1 + row) * width + k2 + col] * cos1 * cos2 / N;
						if (k1 != 0)
							val *= Math.sqrt(2);
						if (k2 != 0)
							val *= Math.sqrt(2);
						sum += val;
					}
				}
				f[(n1 + row) * width + n2 + col] = (short) Math.round(sum); // signed
			}
		}
		return f;
	}
}

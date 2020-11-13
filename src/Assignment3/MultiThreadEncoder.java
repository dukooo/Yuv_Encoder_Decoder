package Assignment3;


import java.util.ArrayList;

public class MultiThreadEncoder extends Thread{
    
    byte[] reconstructed;
    ArrayList<Integer> finalMV; int [] qtc; byte[] vbs;
    int [] prevMV;
    int [] prevMode;
    int y, x;
    EncoderBlockBased en;
    ArrayList<byte[][]> buffers;
    boolean isInter;
    public MultiThreadEncoder(EncoderBlockBased encoder, byte[] reconstructed, ArrayList<Integer> finalMV, int [] qtc, int [] prevMV, byte[] vbs,
            int y, int x, int [] prevMode, int frameIndex, boolean isInter) {
        this.reconstructed = reconstructed; 
        this.finalMV = finalMV;
        this.qtc = qtc;
        this.vbs = vbs;
        this.y = y;
        this.x = x;
        this.en = encoder;
        this.en.frameIndex = frameIndex;
        this.en.setCurr(frameIndex);
        this.prevMV = prevMV;
        this.prevMode = prevMode;
        this.buffers = this.en.buffers;
        this.isInter = isInter;
    }
    public MultiThreadEncoder() {
        return;
    }
    
    @Override
    public void run() { 
        if (this.isInter)
            this.en.encodeInter(qtc, finalMV, reconstructed, buffers, prevMV, y, x);
        else
            this.en.encodeIntra(qtc, finalMV, reconstructed, prevMode, y, x);
    } 
    
    public void startEncodingTypeZero() {
    	EncoderBlockBased enc1 = new EncoderBlockBased();
    	enc1.startEncoding();
    }
    
    public void startEncodingTypeOne(EncoderBlockBased enc1, EncoderBlockBased enc2, int height, int width, int blockSize) {
    	int writeCnt = 0;
    	MultiThreadEncoder t1;
		MultiThreadEncoder t2;
		byte[] blankFilled = new byte[width * height];

    	for (int i = 0; i < enc1.frameLength; i++) {
            System.out.println("Working on Frame # " + i);
            enc1.setCurr(i);
            enc1.buildBuffers();
            enc2.setCurr(i);
            enc2.buildBuffers();

            // Storage for entire frame
            byte[] reconstructed = new byte[width * height]; // per pixel
           
            
            int[] qtc = new int[width*height]; // per pixel
            byte [] vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
            ArrayList<Integer> finalMVT1 = new ArrayList<Integer>(); // 1 frame of MVs
            ArrayList<Integer> finalMVT2 = new ArrayList<Integer>(); // 1 frame of MVs
            ArrayList<Integer> combinedMV = new ArrayList<Integer>(); // combine the MVs collected from both threads.
            int [] prevMVT1 = new int[3]; // always 3 elements in mv
            int [] prevMVT2 = new int[3]; // always 3 elements in mv
            int [] prevModeT1 = new int[1];
            int [] prevModeT2 = new int[1];
            
            // Encode everything as inter frame.
            for (int row = 0; row < height; row+=blockSize) {
            	for (int col = 0; col < width-blockSize; col+=2*blockSize) {
            		
            		if (i % enc1.I_Period != 0) {
            			// Inter frame
            			enc1.useGreyRef = false;
            			enc2.useGreyRef = false;
            			t1 = new MultiThreadEncoder(enc1, reconstructed, finalMVT1, qtc, new int[] {0,0,0}, vbsBytes, row, col, new int[] {0}, i, true);
            			t2 = new MultiThreadEncoder(enc2, reconstructed, finalMVT2, qtc, new int[] {0,0,0}, vbsBytes, row, col+blockSize, new int[] {0}, i, true);
            		}
            		else {
            			// intra
            			enc1.useGreyRef = true;
            			enc2.useGreyRef = true;
            			t1 = new MultiThreadEncoder(enc1, reconstructed, finalMVT1, qtc, new int[] {0,0,0}, vbsBytes, row, col, new int[] {0}, i, true);
            			t2 = new MultiThreadEncoder(enc2, reconstructed, finalMVT2, qtc, new int[] {0,0,0}, vbsBytes, row, col+blockSize, new int[] {0}, i, true);
            		}
          			              
   
					t1.start();
					t2.start();
					try {
						t1.join();
						t2.join();
						combinedMV.addAll(finalMVT1);
		            	combinedMV.addAll(finalMVT2);
		            	finalMVT1 = new ArrayList<Integer>();
		            	finalMVT2 = new ArrayList<Integer>();
					} catch (InterruptedException e) {
       					// TODO Auto-generated catch block
       					e.printStackTrace();
   				    }    
            	}
            	
            }
            enc1.updateReference(reconstructed);
            enc2.updateReference(reconstructed);
            
            // Write data we care about to file
            FileHelper.writeToReconstructedFile(reconstructed, writeCnt);
			// FileHelper.writeToMVFile(combinedMV, writeCnt); // no need to write for type 1 encoding
			int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
			FileHelper.writeToQtcFile(Helper.intArrToShortArr(qtc), writeCnt);
			FileHelper.writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded), writeCnt);
			FileHelper.writeToVBSFile(vbsBytes, writeCnt);
			
			// writeCnt++ so we don't overwrite stuff from previous write above. 
			writeCnt += 1;
    	}
    }
    
    public void startEncodingTypeTwo(EncoderBlockBased enc1, EncoderBlockBased enc2, int height, int width, int blockSize) {
    	int writeCnt = 0;
    	MultiThreadEncoder t1;
		MultiThreadEncoder t2;
    	for (int i = 0; i < enc1.frameLength; i++) {
            System.out.println("Working on Frame # " + i);
            enc1.setCurr(i);
            enc1.buildBuffers();
            enc2.setCurr(i);
            enc2.buildBuffers();

            // Storage for entire frame
            byte[] reconstructed = new byte[width * height]; // per pixel
            int[] qtc = new int[width*height]; // per pixel
            byte [] vbsBytes = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
            ArrayList<Integer> finalMVT1 = new ArrayList<Integer>(); // 1 frame of MVs
            ArrayList<Integer> finalMVT2 = new ArrayList<Integer>(); // 1 frame of MVs
            ArrayList<Integer> combinedMV = new ArrayList<Integer>(); // combine the MVs collected from both threads.
            int [] prevMVT1 = new int[3]; // always 3 elements in mv
            int [] prevMVT2 = new int[3]; // always 3 elements in mv
            int [] prevModeT1 = new int[1];
            int [] prevModeT2 = new int[1];
            
            if (i % enc1.I_Period != 0) {
            	// P frame (inter frames)
	            for (int row = 0; row < height-blockSize; row+=2*blockSize) {
	            	for (int col = 0; col < width; col+=blockSize) {
	
	          			t1 = new MultiThreadEncoder(enc1, reconstructed, finalMVT1, qtc, prevMVT1, vbsBytes, row, col, prevModeT1, i, true);              
	          		 	t2 = new MultiThreadEncoder(enc2, reconstructed, finalMVT2, qtc, prevMVT2, vbsBytes, row+blockSize, col, prevModeT2, i, true);
						t1.start();
						t2.start();
						try {
							t1.join();
							t2.join();
						} catch (InterruptedException e) {
	       					// TODO Auto-generated catch block
	       					e.printStackTrace();
       				    }    
	            	}
	            	combinedMV.addAll(finalMVT1);
	            	combinedMV.addAll(finalMVT2);
	            	finalMVT1 = new ArrayList<Integer>();
	            	finalMVT2 = new ArrayList<Integer>();
	            }
	            enc1.updateReference(reconstructed);
	            enc2.updateReference(reconstructed);
            }
            else {
            	// I frame (intra frames)
                for (int row = 0; row < height-blockSize; row+=2* blockSize) {
                	for (int col = 0; col < width; col+=blockSize) {
               		 	
                        t1 = new MultiThreadEncoder(enc1, reconstructed, finalMVT1, qtc, prevMVT1, vbsBytes, row, col, prevModeT1, i, false);   
                        t1.start();
                        if (col != 0) {
                        	t2 = new MultiThreadEncoder(enc2, reconstructed, finalMVT2, qtc, prevMVT2, vbsBytes, row+blockSize, col-blockSize, prevModeT2, i, false);
                            t2.start();
                        }
                        else {
                        	t2 = null;
                        }
                        
                        
                        try {
        					t1.join();
        					if (t2 != null)
        						t2.join();
        									
        				} catch (InterruptedException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
                       
                    }
                	// Corner case for the very last block in the row. We do all other blocks in parallel, but this block will always be left behind
                    t2 = new MultiThreadEncoder(enc2, reconstructed, finalMVT2, qtc, prevMVT2, vbsBytes, row+blockSize, width-blockSize, prevModeT2, i, false);   
                    t2.start();
                    try {
                    	t2.join();
        			} catch (InterruptedException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}
                    
                    // Now combine things
                    combinedMV.addAll(finalMVT1);
	            	combinedMV.addAll(finalMVT2);
	            	finalMVT1 = new ArrayList<Integer>();
	            	finalMVT2 = new ArrayList<Integer>();
                }
            	enc1.setPredictor();
            	enc2.setPredictor();
            	
            	// One frame is done. We need to merge the 2 MV list into 1 for easy file writing.
            	// combinedMV = Helper.combineMVsForOneFrame(finalMVT1, finalMVT2, width/blockSize, 1);
            }
            
            // Write data we care about to file
            FileHelper.writeToReconstructedFile(reconstructed, writeCnt);
			FileHelper.writeToMVFile(combinedMV, writeCnt);
			int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtc, width, height);
			FileHelper.writeToQtcFile(Helper.intArrToShortArr(qtc), writeCnt);
			FileHelper.writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded), writeCnt);
			FileHelper.writeToVBSFile(vbsBytes, writeCnt);
			
			// writeCnt++ so we don't overwrite stuff from previous write above. 
			writeCnt += 1;
    	}
    }
    
    public void startEncodingTypeThree(EncoderBlockBased enc1, EncoderBlockBased enc2, int height, int width, int blockSize) {
    	
    	int writeCnt = 0;
    	MultiThreadEncoder t1;
		MultiThreadEncoder t2;
    	for (int i = 0; i < enc1.frameLength; i+=2) {
            System.out.println("Encoder Thread 1 Working on Frame # " + i);
            System.out.println("Encoder Thread 2 Working on Frame # " + (i+1));
            enc1.setCurr(i);
            enc1.buildBuffers();
            if (i+1 < enc1.frameLength) {
            	enc2.setCurr(i+1);
            	if (i % enc1.I_Period != 0)
            		enc2.buildBuffers();
            }
            

            // Storage for entire frame
            byte[] reconstructedFrameI = new byte[width * height]; // per pixel
            byte[] reconstructedFrameIPlusOne = new byte[width * height]; // per pixel
            int[] qtcI = new int[width*height]; // per pixel
            int[] qtcIPlusOne = new int[width*height]; // per pixel
            byte [] vbsBytesI = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
            byte [] vbsBytesIPlusOne = new byte[width*height/blockSize/blockSize]; // Need number of blocks bytes.
            ArrayList<Integer> finalMVT1 = new ArrayList<Integer>(); // 1 frame of MVs
            ArrayList<Integer> finalMVT2 = new ArrayList<Integer>(); // 1 frame of MVs
            int [] prevMVT1 = new int[3]; // always 3 elements in mv
            int [] prevMVT2 = new int[3]; // always 3 elements in mv
            int [] prevModeT1 = new int[1];
            int [] prevModeT2 = new int[1];
            
            if (i % enc1.I_Period != 0) {
            	// P frame (inter frames)
	            for (int row = 0; row < height; row+=blockSize) {
	            	for (int col = 0; col < width; col+=blockSize) {

	          			t1 = new MultiThreadEncoder(enc1, reconstructedFrameI, finalMVT1, qtcI, prevMVT1, vbsBytesI, row, col, prevModeT1, i, true);              
	          			t1.start();
	          			if ((i+1) < enc1.frameLength) { // next frame is needed
	          				if (((i + 1) % enc1.I_Period != 0)) {
		          				// Next frame is also inter, then next frame can only start after row >= 2*blockSize
		          				if (row >= 2*blockSize) {
		          					
		          					t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row-2*blockSize, col, prevModeT1, i+1, true);
		          					t2.start();
		          				}
		          				else {
		          					t2 = null;
		          				}
		          			}
		          			else {
		          				// Next frame is intra, can start in parallel
		          				t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row, col, prevModeT1, i+1, false);           
		          				t2.start();
		          			}
	          			}
	          			else {
	          				t2 = null;
	          			}
	          			
	          			
						try {
							t1.join();
							if (t2 != null)
								t2.join();
							
						} catch (InterruptedException e) {
	       					// TODO Auto-generated catch block
	       					e.printStackTrace();
       				    }    
	            	}
	            	if (i+1 < enc1.frameLength) {
	            		enc2.updateLastReferenceFrame(reconstructedFrameI);
		            	enc2.buildBuffers();
	            	}
	            }
            	if (i+1 < enc1.frameLength) {
            		enc2.updateLastReferenceFrame(reconstructedFrameI);
	            	enc2.buildBuffers();
            	}
	            // Thread 2 needs to finish, if it was inter, it started late
	            if ((i+1 < enc1.frameLength) && ((i + 1) % enc1.I_Period != 0)) {
		            for (int row = height-2*blockSize; row < height; row+=blockSize) {
		            	for (int col = 0; col < width; col+=blockSize) {
		            		t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row, col, prevModeT2, i+1, true);
		            		t2.start();
		            		try {
								t2.join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		            	}
		            }
	            }
	            // Both threads have finished their frames now
	            enc1.updateReference(reconstructedFrameI);
	            enc1.updateReference(reconstructedFrameIPlusOne);
	            enc2.updateReference(reconstructedFrameIPlusOne);
            }
            else {
            	// I frame (intra frames)
                for (int row = 0; row < height; row+=blockSize) {
                	for (int col = 0; col < width; col+=blockSize) {
               		 
                		t1 = new MultiThreadEncoder(enc1, reconstructedFrameI, finalMVT1, qtcI, prevMVT1, vbsBytesI, row, col, prevModeT1, i, false);    
                        t1.start();
                        t2 = null;
                        
                        if (i+1 < enc1.frameLength) {
                        	if (row == 0 && col == 0) {
                        		enc2.setPredictor();
                            	enc2.buildBuffers();
                        	}
                        		
                        	if (row >= 2*blockSize) {	
                        		 if ((i + 1) % enc1.I_Period != 0) {
                        			 // next frame is inter
                        			 t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row-(2*blockSize), col, prevModeT2, i+1, true);
 		          					 t2.start();
                        		 }
                        		 else {
     		          				// Next frame is intra
     		          				t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row-(2*blockSize), col, prevModeT1, i+1, false);           
     		          				t2.start();
     		          			}
                        	}
                        }
                        try {
        					t1.join();
        					if (t2 != null)
								t2.join();
							// thread 2 needs to get updated t1 (if we decide to give inter frame the previous intra as reference)
							// enc2.updateLastReferenceFrame(reconstructedFrameI);
        				
        				} catch (InterruptedException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
                       
                    }
                	
                }
                
	            // Thread 2 needs to finish, it started late
                if (i+1 < enc1.frameLength) {
		           
		            for (int row = height-2*blockSize; row < height; row+=blockSize) {
		            	for (int col = 0; col < width; col+=blockSize) {
		            		t2 = new MultiThreadEncoder(enc2, reconstructedFrameIPlusOne, finalMVT2, qtcIPlusOne, prevMVT2, vbsBytesIPlusOne, row, col, prevModeT2, i+1, ((i + 1) % enc1.I_Period) != 0);
		            		t2.start();
		            		try {
								t2.join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		            	}
		            }
		            
                }
                
                // Both threads done. Corner case being 2 Intras in a row. If 2 intras in a row, then only updateReference
                enc1.setPredictor();
                if ((i + 1) % enc1.I_Period == 0) {
                	enc2.setPredictor();
                }
                else {
                	enc1.updateReference(reconstructedFrameIPlusOne);
                	enc2.updateReference(reconstructedFrameIPlusOne);
                }
	
            	// One frame is done. We need to merge the 2 MV list into 1 for easy file writing.
            	// combinedMV = Helper.combineMVsForOneFrame(finalMVT1, finalMVT2, width/blockSize, 1);
            }
            
            // Write data we care about to file
            FileHelper.writeToReconstructedFile(reconstructedFrameI, writeCnt);
			FileHelper.writeToMVFile(finalMVT1, writeCnt);
			int[] qtcDiagReorded = EntropyEncDec.diagReorder(qtcI, width, height);
			FileHelper.writeToQtcFile(Helper.intArrToShortArr(qtcI), writeCnt);
			FileHelper.writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded), writeCnt);
			FileHelper.writeToVBSFile(vbsBytesI, writeCnt);
			
			// writeCnt++ so we don't overwrite stuff from previous write above. 
			writeCnt += 1;
			
            FileHelper.writeToReconstructedFile(reconstructedFrameIPlusOne, writeCnt);
			FileHelper.writeToMVFile(finalMVT2, writeCnt);
			qtcDiagReorded = EntropyEncDec.diagReorder(qtcIPlusOne, width, height);
			FileHelper.writeToQtcFile(Helper.intArrToShortArr(qtcIPlusOne), writeCnt);
			FileHelper.writeToQtcDiagOrderedFile(Helper.intArrToShortArr(qtcDiagReorded), writeCnt);
			FileHelper.writeToVBSFile(vbsBytesIPlusOne, writeCnt);
			
    	}
    }
    
    
    public void startEncoding() {
    	 PropertyHelper ph = new PropertyHelper();
    	 EncoderBlockBased enc1 = new EncoderBlockBased();
    	 EncoderBlockBased enc2 = new EncoderBlockBased();
    	 
    	 // These parameters are for testing only
    	 ph.setProperty("vidName", "foreman_cif_y_only.yuv");
    	 ph.setProperty("vidHeight", "288");
    	 ph.setProperty("vidWidth", "352");
    	 ph.setProperty("frameCount", "10");
    	 ph.setProperty("FMEEnable", "true");
    	 ph.setProperty("vbsEnable", "1");
    	 ph.setProperty("fastME", "true");
    	 ph.setProperty("nRefFrames", "1");
    	 ph.setProperty("rangeR", "4");
    	 ph.setProperty("I_Period", "8");
    	 ph.setProperty("quantizationPar", "4");    	 
    	 
         int blockSize = Integer.valueOf(ph.getProperty("blkSizeI"));
         int width = Helper.getWidthAfterPadding(Integer.valueOf(ph.getProperty("vidWidth")), blockSize);
         int height = Helper.getHeightAfterPadding(Integer.valueOf(ph.getProperty("vidHeight")), blockSize);
         int encodingType =  Integer.valueOf(ph.getProperty("encodingType"));
         
         if (encodingType == 0) {
        	 System.out.println("Encoding Type 0 ");
//        	 ph.setProperty("fastME", "false");
        	 this.startEncodingTypeZero();
         }
         else if (encodingType == 1) {
        	 // Disable everything
        	 System.out.println("Encoding Type 1 ");
//        	 ph.setProperty("nRefFrames", "1");
//        	 ph.setProperty("rangeR", "4");
//        	 ph.setProperty("fastME", "false");
//        	 ph.setProperty("FMEEnable", "false");
//        	 ph.setProperty("vbsEnable", "0");
        	 
        	 this.startEncodingTypeOne(enc1, enc2, height, width, blockSize);
         }
         else if (encodingType == 2) {
        	 System.out.println("Encoding Type 2 ");
        	 this.startEncodingTypeTwo(enc1, enc2, height, width, blockSize);
         }
         else if (encodingType == 3) {
        	 System.out.println("Encoding Type 3 ");
        	 // Disable FastME because we need to limit search range
//        	 ph.setProperty("fastME", "false");
        	 this.startEncodingTypeThree(enc1, enc2, height, width, blockSize);
         }
         else {
        	 System.out.println("unsupported");
        	 System.exit(1);
         }

         // Only call this when everything is done.
         EntropyEncDec.generateAllEntropyEncodedFiles();
    }
   
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        MultiThreadEncoder mulenc = new MultiThreadEncoder();
        mulenc.startEncoding();
       
    }

}

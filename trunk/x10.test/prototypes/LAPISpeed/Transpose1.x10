public class Transpose1 {

   final static int INIT_PHASE = 0;
   final static int TRANSPOSE_PHASE=1;
   final static int STARTUP_PHASE=2;
   final static int TOTAL_TIME=3;
   final static int NUM_TIMERS=4;

   final static long totalTime[];
   final static long startTimer[];

   // need to manage memory blocks specially--if
   // two blocks share the same vm they can resolve to the same
   // variable--need to ensure they live seperately, hence
   // maintain an explicit list/array of blocks
   final java.lang.Object[] _srcMatrix;
   final java.lang.Object[] _destMatrix;

   // maintain P * Q global arrays, each mapped to a single place
	// keep an array of global arrays so we can access them
     	private boolean _debug;
     	final private int _x;
	final private int _y;
	final private int _P;
	final private int _Q;
    	final int[] _transposeMap;// keep a local copy at each place
   
   static {
     totalTime = new long[NUM_TIMERS];
     startTimer = new long[NUM_TIMERS];
  }
   static void startTimer(int PhaseID){
      startTimer[PhaseID] = System.nanoTime();
   }

   static void stopTimer(int PhaseID){
      long stop = System.nanoTime();
      totalTime[PhaseID] += stop - startTimer[PhaseID];
   }

   static void reportTimes(int iterCount){
      System.out.println("Startup time:"+(totalTime[STARTUP_PHASE]/1000000000.0));
      System.out.println("Initialization time:"+(totalTime[INIT_PHASE]/1000000000.0));
      System.out.println("Total Transpose time:"+(totalTime[TRANSPOSE_PHASE]/1000000000.0));
      System.out.println("Average Transpose time:"+(totalTime[TRANSPOSE_PHASE]/(iterCount*1000000000.0)));
      System.out.println("Elapsed time:"+(totalTime[TOTAL_TIME]/1000000000.0));
   }
   // a simplified example of array transpose.  Assumes number of places is even.
   // each place represents one block of matrix elements.  Entire blocks will be exchanged, and then
   // transposed remotely.  
   // Each block is x by y elements, and there are P by Q blocks--this constitutes the matrix layout
   // that is, each block is
   //
   // (0,0), (0,1), ..., (0,x-1)
   // (1,0), (1,1), ..., (1,x-1)
   //         ...
   // (y-1,0),(y-1,1),..,(y-1,x-1)
   //
   // Note that (y-1,1) = (y-1)*x + 1
   //
   // and the blocks are ordered and places mapped sequentially:
   //
   // 0,               1,         2, ...,     Q-1
   // Q,             Q+1,       Q+2, ...,   2*Q-1
   // 2*Q,         2*Q+1,     2*Q+2, ....,  3*Q-1
   //  .              .          .             .
   //  .              .          .             .
   // (P-1)*Q, (P-1)*Q+1, (P-1)*Q+2, ..., (P*Q)-1
//
   // there is a 1-1 mapping of places to blocks
   // The inter-place communication will swap entire blocks, and then the
   // receiving place will transpose the block itself
   // the places are transposed and end up as:
   // 0 ,            1,    2,  ... , (P-1)
   // P ,          P+1,  P+2,  ... , 2*P-1,
   // 2*P,         2*P+1,  ...,  ... , 3P-1
   //     ...
   // (Q-1)*P, (Q-1)*P+1,  ...,      , (Q*P)-1

   // so a single dimension mapping array is constructed and used to
   // determine the destination of a given block e.g.
   // if there is a 8x8 (P=Q=8) grid (64 places), place 10 (places are zero based)
   // would correspond to index 10 (8+2) would map to place(2*8+1) = 17

  // Program constructs an array of target places
    public static void main(String[] args) {
       
        int dimX,dimY,blockP,blockQ;
        dimX=dimY=blockP=blockQ=0;
        boolean debug = false;

        int N=10;

        startTimer(STARTUP_PHASE);
        startTimer(TOTAL_TIME);
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-debug")) {
                    debug = true;
                }
                else if (args[i].equals("-P")) {
	            blockP = java.lang.Integer.parseInt(args[++i]);
                }
                else if (args[i].equals("-Q")) {
                    blockQ = java.lang.Integer.parseInt(args[++i]);
                }
                else if (args[i].equals("-x")) {
                    dimX = java.lang.Integer.parseInt(args[++i]);
                }
                else if (args[i].equals("-y")) {
                    dimY = java.lang.Integer.parseInt(args[++i]);
                }
                else if (args[i].equals("-N")) {
                    N = java.lang.Integer.parseInt(args[++i]);
                }
                
            } else {
                System.out.println("Usage: -x <numElements> -y <numElements> -P <blockSize> -Q <blockSize>");
                x10.lang.Runtime.setExitCode(99);
                return;
            }
          }	
          if(!checkVal(dimX,"-x <size>")) return;
          if(!checkVal(dimY,"-y <size>")) return;
          if(!checkVal(blockQ,"-Q <size>")) return;
          if(!checkVal(blockP,"-P <size>")) return;

          if(place.MAX_PLACES != blockP * blockQ){
            System.out.println("Configuration error: num places "+ place.MAX_PLACES+" != "+blockP+" * "+blockQ);
		x10.lang.Runtime.setExitCode(99);
            return;
          }
          System.out.println("block sizes:"+dimX+" x "+dimY+" block distribution:"+
                              blockP+"x"+blockQ+" iteration count:"+N);
          Transpose1 trans = new Transpose1(dimX,dimY,blockP,blockQ,debug);
          stopTimer(STARTUP_PHASE);
	  trans.initialize();

	
	  for(int i=0;i < 2;++i){
             
            trans.transpose();
            
          }
          stopTimer(TOTAL_TIME);
          if (debug){
            trans.dumpSrc();
            trans.dumpDest();
          }
	  reportTimes(N);
	    
        }




         
     public Transpose1(int x, int y, int P, int Q,boolean debug){
       _debug = debug;
     	_x = x;
	_y = y;
	_P = P;
	_Q = Q;
        place cur_place = place.FIRST_PLACE;
        //verify we're zero based
        if(cur_place.id != 0){
           throw new RuntimeException("placeid not zero based");
        }
        _transposeMap = new int[place.MAX_PLACES];
        // these should be arrays of double arrays, but polyglot doesn't like it
        _srcMatrix = new java.lang.Object [_P*_Q];
        _destMatrix = new java.lang.Object [_P*_Q];
        
        for(int i = 0;i <P;++i)
          for(int j = 0; j<Q;++j){
            _transposeMap[i*Q+j] = j*P + i;
            //System.out.println("["+i+","+j+"] "+(i*Q+j)+"=>"+(j*P+i));
	  }
     }

    public Transpose1(){ //default required for multivm
       // these stmts will be tossed
         _x=_y=_P=_Q = 0;
         _transposeMap = new int [1];
         _srcMatrix = new java.lang.Object[1];
         _destMatrix = new java.lang.Object[1];
    }
    public void initialize(){
         startTimer(INIT_PHASE);
         place cur_place = place.FIRST_PLACE;
          finish do {
            // if(_debug) System.out.println("Launching init at "+cur_place.id);
             async(cur_place){
                int placeId = here.id;
                _srcMatrix[here.id] = (java.lang.Object)createLocalBlock(placeId);
                _destMatrix[here.id] = (java.lang.Object)(new double[_x*_y]);// leave empty
             }

             cur_place = cur_place.next();
          }while(cur_place != place.FIRST_PLACE);
          stopTimer(INIT_PHASE);
     }

    public double[] createLocalBlock(int blockNumber){
      double[] array = new double[_x * _y];
      for(int i = 0;i < _x;++i)
        for(int j = 0; j < _y;++j)
            array[i*_y + j] = 1000*blockNumber + i*100 + j;
       return array;
    }

    // these blocks are x * y elements big
    //
    void transposeBlock(double[] destBlock,final double[] srcBlock){
     for(int i = 0; i < _x;++i)
       for(int j = 0; j < _y;++j){
          destBlock[j*_x + i] = srcBlock[i*_y + j];
        }
     }

     public void transpose(){
         startTimer(TRANSPOSE_PHASE);

          place cur_place = place.FIRST_PLACE;
          finish do {
             async(cur_place){
                final int destinationPlaceId = _transposeMap[here.id];
                x10.runtime.Place[] placeArray = x10.lang.Runtime.places();
                place destPlace = placeArray[destinationPlaceId];

                if(_debug) System.out.println("Moving Block "+here.id+" to "+destinationPlaceId);
                if(here.id == destinationPlaceId){
                  double[] srcBlock = (double[])_srcMatrix[here.id];
                  double[] destBlock = (double[])_destMatrix[here.id];
                  transposeBlock(destBlock,srcBlock);
                }
                else {
	          final double[] srcBlock = (double[])_srcMatrix[here.id];
                  async(destPlace){
                       double[] destBlock = (double[])_destMatrix[here.id];
		       transposeBlock(destBlock,srcBlock);
                  }
                }  
             }
           
           cur_place = cur_place.next();
          }while(cur_place != place.FIRST_PLACE);

        stopTimer(TRANSPOSE_PHASE);
      
	}

    public void dumpSrc(){
       place cur_place = place.FIRST_PLACE;
       System.out.println("Dumping source array");
       do {
          finish async(cur_place){
            System.out.println("Dumping block "+here.id);
            dumpBlock((double[])_srcMatrix[here.id],_x,_y);
          }
          cur_place = cur_place.next();
       }while(cur_place != place.FIRST_PLACE);
    }

    public void dumpDest(){
       System.out.println("Dumping dest array");
       place cur_place = place.FIRST_PLACE;
       do {
          finish async(cur_place){
            System.out.println("Dumping block "+here.id);
            dumpBlock((double[])_destMatrix[here.id],_y,_x);
          }
          cur_place = cur_place.next();
       }while(cur_place != place.FIRST_PLACE);
    }
    void dumpBlock(double[] block,int colSize,int rowSize){
        String blockRow;
                for(int i = 0;i < colSize;++i){
          blockRow = i+": ";
         
          for(int j = 0;j < rowSize;++j){
            blockRow += block[i*rowSize+j] + "   ";
          }
          System.out.println(blockRow);
        }
        System.out.println("");
    }

     public static boolean checkVal(int val,String msg){
        if(val <= 0){
          System.out.println("Invalid value "+val+" for "+msg);
          x10.lang.Runtime.setExitCode(99);
	  return false;
        }
        return true;
      }
  }

/**
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
import harness.x10Test;

/**
 * Building arrays distributed across places using the encapsulation approach 
 * (1D array of 2D arrays).
 * @author Tong 11/29/2006
 */

public class EncapsulatedArray1D_Dep extends x10Test {
    
    static value Wrapper{
        val m_array: Array[double](2);
        def this(var a_array: Array[double](2)): Wrapper = {
            m_array=a_array;
        }
    }
    
    public def run(): boolean = {

        val size: int = 5;
        val R  = [0..size-1, 0..size-1] to Region;
        val D  = Dist.makeUnique(); 
        val numOfPlaces = Place.MAX_PLACES;
        
        val A  = Array.make[Wrapper](D);
        finish ateach (val (i): Point in D) 
        A(i)=new Wrapper(Array.make[double](R->here, (Point)=>0.0D));
        
        //for (int i=0;i<numOfPlaces;i++){    
        finish ateach (val (i): Point in D) { 
            val temp  =   A(i).m_array;
            for (val p: Point(2) in temp.region) temp(p)=(i to Double);
        }
        
        return true;
    }
    
    public static def main(var args: Rail[String]): void = {
        new EncapsulatedArray1D_Dep().execute();
    }

}

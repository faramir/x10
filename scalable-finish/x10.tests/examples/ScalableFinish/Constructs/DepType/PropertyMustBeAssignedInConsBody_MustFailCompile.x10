/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

/**
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
//LIMITATION:
//The current release does not implement the check that for every constructor
// with a defined return type, every path to the exit node contains 
// an invocation of property that is strong enough to entail the return type.


/** Test that the compiler detects a situation in which one branch of a conditional has
    a property clause but not another.
 *@author vj
 *
 */

 

public class PropertyMustBeAssignedInConsBody_MustFailCompile   { 

    class Tester(i: int(2) ) {
      public def this(arg:int):Tester { 
        
      } 
    }
	
 
    public def run()=true;
  
	
    public static def main(var args: Rail[String]): void = {
        new PropertyMustBeAssignedInConsBody_MustFailCompile().run ();
    }
   

		
}
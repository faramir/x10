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

import harness.x10Test;
import x10.compiler.NoInline;

/**
 * Check that closure literals can be upcast
 * to superclass function types and properly evaluated
 * at the supertype.  
 * Also checks to see if the same can be done for a class
 * like Array that implements a function type.
 *
 * Functionality implemented under XTENLANG-920.
 */
public class FunctionUpcast1 extends x10Test {
    public static @NoInline def eval1(cls:(Int,Int)=>Any) = cls(1000, 1);
    public static @NoInline def eval2(cls:()=>Any) = cls();
    public static @NoInline def eval3(i:Int, cls:(Int)=>Any) = cls(i);

    public def run(): boolean = {
        val e1 = eval1((a:Any, b:Any)=>((a as Int)+(b as Int)+10));
        val e2 = eval2(()=>1011);
        chk(e1 == e2);
        chk(e1.equals(1011));
    
        val data = [1,1,2,3,5,8,13,21];
        for (pt in data) {
            val e3 = data(pt);
            val e4 = eval3(pt, data);
            chk(e3.equals(e4));
        }
        
	return true;
    }

    public static def main(args:Rail[String]) {
        new FunctionUpcast1().execute();
    }
}

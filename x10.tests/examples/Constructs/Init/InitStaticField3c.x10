/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */

import harness.x10Test;


/**
 * Check lazy, per-field, per-place initialization semantics of static fields.
 * Valid for X10 2.2.3 and later.
 * 
 * @author mtake 7/2012
 */
public class InitStaticField3c extends x10Test {

	static val a = "abc" + "def";
	static val b = "abc".length();
	static val c = "abc".typeName();
	static val d = "abc".equals("def");
	static val e = "abc"(1);
	static val f = "abc".substring(1,2);

	static val xa = 1.0 / 0.0;
	static val xb = -1.0 / 0.0;
	static val xc = 0.0 / 0.0;

	static val za = 1.typeName();
	static val zb = 1.equals(2);

    public def run():Boolean {
        chk(a.equals("abcdef"));
        chk(b == 3);
        chk(c.equals("x10.lang.String"));
        chk(d == false);
        chk(e == 'b');
        chk(f.equals("b"));
        
        chk(xa == Double.POSITIVE_INFINITY);
        chk(xb == Double.NEGATIVE_INFINITY);
        chk(xc != Double.NaN); // "NaN == NaN" is false.
        
        chk(za.equals("x10.lang.Int"));
        chk(zb == false);

        return true;
    }

    public static def main(Array[String](1)) {
        new InitStaticField3c().execute();
    }

}

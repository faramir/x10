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



/**
 * @author bdlucas 8/2008
 */

public class GenericInheritance03 extends GenericTest {

    interface I[T] {
        def m(T):int;
    }

    
    class A implements I[int] {
        public def m(int) = 0;
        public def m(String) = 1;
    }

    public def run() = {

        val a = new A();
        check("a.m(0)", a.m(0), 0);
        check("a.m(\"1\")", a.m("1"), 1);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new GenericInheritance03().execute();
    }
}

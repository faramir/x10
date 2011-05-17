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
 * Java and X10 permit a call to a method which returns a value to occur as a statement.
 * The returned value is discarded. However, Java does not permit a variable standing alone
 * as a statement. Thus the x10 compiler must check that as a result of flattening it does
 * not produce a variable standing alone. 
 * In an earlier implementation this would give a t0 not reachable error.
 */

public class FlattenVarInit   {

    val a: Array[int](2)!;

    public def this(): FlattenVarInit = {
        a = new Array[int]([1..10, 1..10], ((i,j): Point): int => { return i+j;});
    }

    def m(var x: int): int = {
        return x;
    }
    
    public def run(): boolean = {
    var t0: int;
        t0 = m(a(1, 1));
        return t0==2;
    }

    public static def main(var args: Rail[String]): void = {
        new FlattenVarInit().run ();
    }
}
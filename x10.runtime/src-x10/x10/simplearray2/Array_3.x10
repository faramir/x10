/*
 *  This file is part of the X10 project (http://x10-lang.org).
 * 
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 *  (C) Copyright IBM Corporation 2006-2013.
 */

package x10.simplearray2;

import x10.compiler.CompilerFlags;
import x10.compiler.Inline;

/**
 * Implementation of 3-dimensional Array.
 */
public final class Array_3[T] (
        /**
         * The number of elements in rank 1 (indexed 0..(numElems_1-1))
         */
        numElems_1:Long,

        /**
         * The number of elements in rank 2 (indexed 0..(numElems_2-1)).
         */
        numElems_2:Long,

        /**
         * The number of elements in rank 3 (indexed 0..(numElems_3-1)).
         */
        numElems_3:Long
) extends Array[T] {
    
    /**
     * Construct a 3-dimensional array with indices [0..m-1][0..n-1][0..p-1] 
     * whose elements are zero-initialized.
     */
    public def this(m:Long, n:Long, p:Long) {
	super(m*n*p, true);
	property(m, n, p);
    }

    /**
     * Construct a 3-dimensional array with indices [0..m-1][0..n-1][0..p-1] 
     * whose elements are initialized to init.
     */
    public def this(m:Long, n:Long, p:Long, init:T) {
	super(m*n*p, false);
        property(m, n, p);
	for (i in raw.range()) {
            raw(i) = init;
        }
    }

    /**
     * Construct a 3-dimensional array with indices [0..m-1][0..n-1][0..p-1] 
     * whose elements are initialized to the value returned by the init closure for each index.
     */
    public @Inline def this(m:Long, n:Long, p:Long, init:(long,long,long)=>T) {
	super(m*n*p, false);
        property(m, n, p);
        for (i in 0..(m-1)) {
            for (j in 0..(n-1)) {
                for (k in 0..(p-1)) {
                    raw(offset(i,j,k)) = init(i,j,j);
                }
            }
        }
    }


    /**
     * Return the string representation of this array.
     * 
     * @return the string representation of this array.
     */
    public def toString():String {
        return "Array: TODO implement pretty print for rank = 3";
    }

    
    /**
     * Return the element of this array corresponding to the given triple of indices.
     * 
     * @param i the given index in the first dimension
     * @param j the given index in the second dimension
     * @param k the given index in the third dimension
     * @return the element of this array corresponding to the given triple of indices.
     * @see #set(T, Int, Int)
     */
    public @Inline operator this(i:long, j:long, k:long):T {
        if (CompilerFlags.checkBounds() && (i < 0 || i >= numElems_1 || 
                                            j < 0 || j >= numElems_2 || 
                                            k < 0 || k >= numElems_3)) {
            raiseBoundsError(i, j, k);
        }
        return Unsafe.uncheckedRailApply(raw, offset(i, j, k));
    }

    
    /**
     * Set the element of this array corresponding to the given triple of indices to the given value.
     * Return the new value of the element.
     * 
     * @param v the given value
     * @param i the given index in the first dimension
     * @param j the given index in the second dimension
     * @param k the given index in the third dimension
     * @return the new value of the element of this array corresponding to the given triple of indices.
     * @see #operator(Int, Int)
     */
    public @Inline operator this(i:long,j:long,k:long)=(v:T):T{self==v} {
        if (CompilerFlags.checkBounds() && (i < 0 || i >= numElems_1 || 
                                            j < 0 || j >= numElems_2 || 
                                            k < 0 || k >= numElems_3)) {
            raiseBoundsError(i, j, k);
        }
        Unsafe.uncheckedRailSet(raw, offset(i, j, k), v);
        return v;
    }
    
    private @Inline def offset(i:long, j:long, k:long) {
        return k + numElems_3 * (j + (i * numElems_2));
    }
}

// vim:tabstop=4:shiftwidth=4:expandtab
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

package x10.array;

public abstract class Xform {

    public static def transpose(rank:Int, i:Int, j:Int) {

        // reverse transform
        val t = new MatBuilder(rank+1, rank+1);
        for (var k:int=0; k<=rank; k++)
            if (k!=i && k!=j)
                t(k, k) = 1;
        t(i, j) = 1;
        t(j, i) = 1;
        val T = t.toXformMat();

        // no extra constraints
        val e = new PolyMatBuilder(2);
        val E = e.toSortedPolyMat(true);

        return new PolyXform(E, T);
    }

    public static def tile(sizes:ValRail[int]) {

        // input rank is sizes.length; 
        // output rank is 2*rank
        val rank = sizes.length;

        // reverse transform
        val t = new MatBuilder(rank+1, 2*rank+1);
        t.setDiagonal(0, 0, sizes.length, sizes);
        t.setDiagonal(0, rank, rank, (Int)=>1);
        t(rank, 2*rank) = 1;
        val T = t.toXformMat();

        // extra constraints
        val e = new PolyMatBuilder(2*rank);
        e.setDiagonal(0, rank, rank, (Int)=>-1);
        e.setDiagonal(rank, rank, rank, (Int)=>1);
        e.setColumn(rank, 2*rank, rank, (i:Int)=>1-sizes(i));
        val E = e.toSortedPolyMat(true);

        return new PolyXform(E, T);
    }

    public static def skew(axis:int, with:ValRail[int]) {

        val rank = with.length - 1;
        
        // reverse transform
        val t = new MatBuilder(rank+1, rank+1);
        t.setDiagonal(0, 0, rank+1, (Int)=>1);
        t.setColumn(0, axis, rank, (i:Int)=>with(i));
        val T = t.toXformMat();

        // no extra constraints
        val e = new PolyMatBuilder(2);
        val E = e.toSortedPolyMat(true);

        return new PolyXform(E, T);
    }

    // compose transforms
    abstract public operator this * (that:Xform!): Xform;
}
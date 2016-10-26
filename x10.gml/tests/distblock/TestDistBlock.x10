/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2011-2016.
 */

import harness.x10Test;

import x10.matrix.util.Debug;
import x10.matrix.DenseMatrix;
import x10.matrix.ElemType;

import x10.matrix.block.Grid;
import x10.matrix.block.BlockMatrix;
import x10.matrix.distblock.DistMap;
import x10.matrix.distblock.DistGrid;
import x10.matrix.distblock.DistBlockMatrix;

import x10.util.Team;
import x10.util.resilient.iterative.PlaceGroupBuilder;

public class TestDistBlock extends x10Test {
    static def ET(a:Double)= a as ElemType;
    static def ET(a:Float)= a as ElemType;
    public val nzp:Float;
    public val M:Long;
    public val N:Long;
    public val K:Long;
    public val bM:Long;
    public val bN:Long;
    
    public val grid:Grid;
    public val dmap:DistMap;
    public val skipPlaces:Long;
    
    public def this(args:Rail[String]) {
        M = args.size > 0 ? Long.parse(args(0)):30;
        nzp = args.size > 1 ?Float.parse(args(1)):0.9f;
        N = args.size > 2 ? Long.parse(args(2)):(M as Int)+1;
        K = args.size > 3 ? Long.parse(args(3)):(M as Int)+2;
        bM= args.size > 4 ? Long.parse(args(4)):4;
        bN= args.size > 5 ? Long.parse(args(5)):5;
	
        grid = new Grid(M, N, bM, bN);
        if (x10.xrx.Runtime.RESILIENT_MODE > 0 && Place.numPlaces() > 2) {
            skipPlaces = 2;
        } else {
            skipPlaces = 0;
        }
        val numPlaces = Place.numPlaces()-skipPlaces;
        dmap = DistGrid.make(grid, numPlaces).dmap;
        //Console.OUT.printf("Matrix M:%d K:%d N:%d, blocks(%d, %d) on %d places\n", M, N, K, bM, bN, Place.numPlaces());
    }
    
    public def run():Boolean {
        Console.OUT.println("DistBlockMatrix clone/add/sub/scaling tests");
	
        var ret:Boolean = true;
        val places = PlaceGroupBuilder.excludeSparePlaces(skipPlaces);	
        val team = new Team(places);
        ret &= (testClone(places, team));
        ret &= (testCopyTo(places, team));
        ret &= (testScale(places, team));
        ret &= (testAdd(places, team));
        ret &= (testAddSub(places, team));
        ret &= (testAddAssociative(places, team));
        ret &= (testScaleAdd(places, team));
        ret &= (testCellMult(places, team));
        ret &= (testCellDiv(places, team));
        return ret;
    }
    
    public def testClone(places:PlaceGroup, team:Team):Boolean {
        var ret:Boolean = true;
        Console.OUT.println("DistBlockMatrix clone test on dense blocks");
        val ddm = DistBlockMatrix.makeDense(grid, dmap, places, team).init((r:Long, c:Long)=>ET(1.0+r+c));
	
        val ddm1 = ddm.clone();
        ret = ddm.equals(ddm1);
	
        val den = DenseMatrix.make(grid.M, grid.N).init((r:Long,c:Long)=>ET(1.0+r+c));
        ret &= den.equals(ddm);
	
        if (!ret)
	    Console.OUT.println("--------DistBlockMatrix Clone test failed!--------");
        return ret;
    }
    
    public def testCopyTo(places:PlaceGroup, team:Team):Boolean {
        var ret:Boolean = true;
        Console.OUT.println("DistBlockMatrix copyTo test");
        val dstblk = DistBlockMatrix.makeDense(grid, dmap, places, team);
        val blkden = BlockMatrix.makeDense(grid);
        val den    = DenseMatrix.make(M,N);
	
        dstblk.initRandom();
        dstblk.copyTo(blkden);
        ret &= dstblk.equals(blkden as Matrix(dstblk.M, dstblk.N));
        if (! ret)  return ret;
	
        dstblk.reset();
        dstblk.copyFrom(blkden);
        ret &= blkden.equals(dstblk as Matrix(blkden.M,blkden.N));
        if (! ret) return ret;
	
        val dmat = DistBlockMatrix.make(M, 1, bM, 1, places.size(), 1, places, team).allocDenseBlocks().initRandom();
        val denm = DenseMatrix.make(M, 1);
	
        dmat.copyTo(denm);
        ret &= dmat.equals(denm as Matrix(dmat.M, dmat.N));
        if (! ret) return ret;
	
	
        if (!ret)
            Console.OUT.println("--------Dist dense matrix copyTo test failed!--------");    
        return ret;
    }
    
    public def testScale(places:PlaceGroup, team:Team):Boolean{
	Console.OUT.println("DistBlockMatrix scaling test");
	val dm = DistBlockMatrix.make(M, N, bM, bN, places, team).allocDenseBlocks().initRandom();
	
	val dm1  = dm * ET(2.5);
	val m = dm.toDense();
	val m1 = m * ET(2.5);
	val ret = dm1.equals(m1);
	if (!ret)
	    Console.OUT.println("--------Dist block matrix Scaling test failed!--------");    
	return ret;
    }
    
    public def testAdd(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix add test");
        val dm = DistBlockMatrix.make(M, N, bM, bN, places, team).allocDenseBlocks().initRandom();
	
        val dm1 = dm  * ET(-1.0);
        val dm0 = dm + dm1;
        val ret = dm0.equals(ET(0.0));
        if (!ret)
            Console.OUT.println("--------DistBlockMatrix Add: dm + dm*-1 test failed--------");
        return ret;
    }
    
    public def testAddSub(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix add-sub test");
        val dm = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
        val dm1= DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
	
        val dm2   = dm  + dm1;
        val dm_c  = dm2 - dm1;
        val ret   = dm.equals(dm_c as Matrix(dm.M, dm.N));
        if (!ret)
            Console.OUT.println("--------DistBlockMatrix Add-sub test failed!--------");
        return ret;
    }
    
    
    public def testAddAssociative(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix associative test");
	
        val a = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
        val b = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();;
        val c = DistBlockMatrix.makeSparse(grid, dmap, nzp, places, team).initRandom();
	
        val c1 = a + b + c;
        val c2 = a + (b + c);
        val ret = c1.equals(c2);
        if (!ret)
            Console.OUT.println("--------DistBlockMatrix Add associative test failed!--------");
        return ret;
    }
    
    public def testScaleAdd(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix scaling-add test");
	
        val a = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
	
        val m = a.toDense();
        val a1= a * ET(0.2);
        val a2= ET(0.8) * a;
        var ret:Boolean = a.equals(a1+a2);
        ret &= a.equals(m);
	
        if (!ret)
            Console.OUT.println("--------DistBlockeMatrix scaling-add test failed!--------");
        return ret;
    }
    
    public def testCellMult(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix cellwise mult test");
	
        val a = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
        val b = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
	
        val c = (a + b) * a;
        val d = a * a + b * a;
        var ret:Boolean = c.equals(d);
	
        val da= a.toDense();
        val db= b.toDense();
        val dc= (da + db) * da;
        ret &= dc.equals(c);
	
        if (!ret)
            Console.OUT.println("--------Dist block matrix cellwise mult test failed!--------");
        return ret;
    }
    
    public def testCellDiv(places:PlaceGroup, team:Team):Boolean {
        Console.OUT.println("DistBlockMatrix cellwise mult-div test");
	
        val a = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
        val b = DistBlockMatrix.makeDense(grid, dmap, places, team).initRandom();
	
        val c = (a + b) * a;
        val d =  c / (a + b);
        var ret:Boolean = d.equals(a);
	
        if (!ret)
            Console.OUT.println("--------Dist block matrix cellwise mult-div test failed!--------");
        return ret;
    }
    
    public static def main(args:Rail[String]) {
        new TestDistBlock(args).execute();
    }
}

/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
import harness.x10Test;;

/**
 * Array bounds test - 3D.
 *
 * randomly generate 3D arrays and indices,
 *
 * see if the array index out of bounds exception occurs
 * in the right  conditions
 */

public class DistBounds3D extends x10Test {

    public def run(): boolean = {
        val COUNT: int = 200;
        val L: int = 3;
        val K: int = 1;
        for (var n: int = 0; n < COUNT; n++) {
            var i: int = ranInt(-L-K, L+K);
            var j: int = ranInt(-L-K, L+K);
            var k: int = ranInt(-L-K, L+K);
            var lb1: int = ranInt(-L, L);
            var lb2: int = ranInt(-L, L);
            var lb3: int = ranInt(-L, L);
            var ub1: int = ranInt(lb1, L);
            var ub2: int = ranInt(lb2, L);
            var ub3: int = ranInt(lb3, L);
            var d: int = ranInt(0, dist2.N_DIST_TYPES-1);
            var withinBounds: boolean = arrayAccess(lb1, ub1, lb2, ub2, lb3, ub3, i, j, k, d);
            chk(iff(withinBounds,
                    i >= lb1 && i <= ub1 &&
                    j >= lb2 && j <= ub2 &&
                    k >= lb3 && k <= ub3));
        }
        return true;
    }

    /**
     * create a[lb1..ub1,lb2..ub2,lb3..ub3] then access a[i,j,k],
     * return true iff
     * no array bounds exception occurred
     */
    private static def arrayAccess(var lb1: int, var ub1: int, var lb2: int, var ub2: int, var lb3: int, var ub3: int, val i: int, val j: int, val k: int, var distType: int): boolean = {

        //pr(lb1+" "+ub1+" "+lb2+" "+ub2+" "+lb3+" "+ub3+" "+i+" "+j+" "+k+" "+ distType);

        val a: Array[int] = Array.make[int](dist2.getDist(distType, [lb1..ub1, lb2..ub2, lb3..ub3]));

        var withinBounds: boolean = true;
        try {
            chk(a.dist(i, j, k).id< Place.MAX_PLACES && a.dist(i, j, k).id >= 0);
            finish async(a.dist(i, j, k)) {
                a(i, j, k) = ( 0xabcdef07L to Int);
                chk(a(i, j, k) == (0xabcdef07L to Int));
            }
        } catch (var e: ArrayIndexOutOfBoundsException) {
            withinBounds = false;
        }

        //pr(lb1+" "+ub1+" "+lb2+" "+ub2+" "+lb3+" "+ub3+" "+i+" "+j+" "+k+" "+distType+" "+ withinBounds);

        return withinBounds;
    }

    // utility methods after this point

    /**
     * print a string
     */
    private static def pr(var s: String): void = {
        System.out.println(s);
    }

    /**
     * true iff (x if and only if y)
     */
    private static def iff(var x: boolean, var y: boolean): boolean = {
        return x == y;
    }

    public static def main(var args: Rail[String]): void = {
        new DistBounds3D().execute();
    }

    /**
     * utility for creating a dist from a
     * a dist type int value
     */
    static class dist2 {

        // Java has poor support for enum
        const BLOCK: int = 0;
        const CYCLIC: int = 1;
        const CONSTANT: int = 2;
        const RANDOM: int = 3;
        const ARBITRARY: int = 4;
        const N_DIST_TYPES: int = 5;

        /**
         * Return a dist with region r, of type disttype
         */
        public static def getDist(var distType: int, var r: Region): Dist = {
            switch(distType) {
                case BLOCK: return Dist.makeBlock(r);
                case CYCLIC: return Dist.makeCyclic(r);
                case CONSTANT: return r->here;
                case RANDOM: return Dist.makeRandom(r);
                case ARBITRARY: return Dist.makeArbitrary(r);
                default:throw new Error("TODO");
            }
        }
    }
}

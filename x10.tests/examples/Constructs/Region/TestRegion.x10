import x10.io.Printer;
import x10.io.StringWriter;

import x10.array.UnboundedRegionException;

import harness.x10Test;


abstract public class TestRegion extends x10Test {
    
    var os: StringWriter;
    var out: Printer;

    def testName() {
        var cn:String = className();
        val init = cn.substring(0,6); // XTENLANG-???
        if (init.equals("class "))
            cn = cn.substring(6, cn.length());
        return cn;
    }

    def this() {
        System.setProperty("line.separator", "\n");
        try {
            os = new StringWriter();
            out = new Printer(os);
        } catch (e:Exception) {
            //e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    abstract def expected():String;

    def status() {
        val got = os.toString();
        if (got.equals(expected())) {
            return true;
        } else {
            System.out.println("=== got:\n" + got);
            System.out.println("=== expected:\n" + expected());
            System.out.println("=== ");
            return false;
        }
    }

    //
    //
    //

    class Grid {

        var os: Rail[Object] = Rail.makeVar[Object](10);

        def set(i0: int, vue: double): void = {
            os(i0) = vue as Object; // XTENLANG-210
        }

        def set(i0: int, i1: int, vue: double): void = {
            if (os(i0)==null) os(i0) = new Grid();
            val grid = os(i0) as Grid;
            grid.set(i1, vue);
        }

        def set(i0: int, i1: int, i2: int, vue: double): void = {
            if (os(i0)==null) os(i0) = new Grid();
            val grid = os(i0) as Grid;
            grid.set(i1, i2, vue);
        }

        def pr(rank: int): void = {
            var min: int = os.length;
            var max: int = 0;
            for (var i: int = 0; i<os.length; i++) {
                if (os(i)!=null) {
                    if (i<min) min = i;
                    else if (i>max) max = i;
                }
            }
            for (var i: int = 0; i<os.length; i++) {
                var o: Object = os(i);
                if (o==null) {
                    if (rank==1)
                        out.print(".");
                    else if (rank==2) {
                        if (min<=i && i<=max)
                            out.print("    " + i + "\n");
                    }
                } else if (o instanceof Grid) {
                    if (rank==2)
                        out.print("    " + i + "  ");
                    else if (rank>=3) {
                        out.print("    ");
                        for (var j: int = 0; j<rank; j++)
                            out.print("-");
                        out.print(" " + i + "\n");
                    }
                    (o as Grid).pr(rank-1);
                } else {
                    // XTENLANG-34, XTENLANG-211
                    val d = (o as Box[double]) as double;
                    out.print((d as int)+"");
                }

                if (rank==1)
                    out.print(" ");
            }
            if (rank==1)
                out.print("\n");
        }
    }

    def prArray(test: String, r: Region): Array[double]{rank==r.rank} = {
        return prArray(test, r, false);
    }

    def prArray(test: String, r: Region, bump: boolean): Array[double]{rank==r.rank} = {

        val init1 = (pt: Point) => {
            var v: int = 1;
            for (var i: int = 0; i<pt.rank; i++)
                v *= pt(i);
            return v%10 as double;
        };

        val init0 = (Point) => 0.0D;

        val a = Array.make[double](r, bump? init0 : init1);
        prArray(test, a, bump);

        return a as Array[double]{rank==r.rank};
    }

    def prUnbounded(test: String, r: Region): void = {
        try {
            prRegion(test, r);
            var s: Region.Scanner = r.scanners().next() as Region.Scanner; // XTENLANG-55
            var i: Iterator[Point] = r.iterator();
        } catch (e: UnboundedRegionException) {
            pr(e.toString());
        }
    }

    def pr(test: String, run: ()=>String) {
        var r: String;
        try {
            r = run();
        } catch (e: Throwable) {
            r = e.getMessage();
        }
        pr(test + " " + r);
    }
            
    def prRegion(test: String, r: Region): void = {

        pr("--- " + testName() + ": " + test);

        pr("rank",       () => r.rank.toString());
        pr("rect",       () => r.rect.toString());
        pr("zeroBased",  () => r.zeroBased.toString());
        pr("rail",       () => r.rail.toString());

        pr("isConvex()", () => r.isConvex().toString());
        pr("size()",     () => r.size().toString());

        pr("region: " + r);
    }

    def prArray(test: String, a: Array[double]): void = {
        prArray(test, a, false);
    }

    def prArray(test: String, a: Array[double], bump: boolean): void = {

        val r: Region = a.region;

        prRegion(test, r);

        // scanner api
        var grid: Grid = new Grid();
        var it: Iterator[Region.Scanner] = r.scanners();
        while (it.hasNext()) {
            var s: Region.Scanner = it.next() as Region.Scanner; // XTENLANG-55
            pr("  poly");
            if (r.rank==0) {
                pr("ERROR rank==0");
            } else if (r.rank==1) {
                var min0: int = s.min(0);
                var max0: int = s.max(0);
                for (var i0: int = min0; i0<=max0; i0++) {
                    if (bump) a(i0) = a(i0)+1;
                    grid.set(i0, a(i0));
                }
            } else if (r.rank==2) {
                var min0: int = s.min(0);
                var max0: int = s.max(0);
                for (var i0: int = min0; i0<=max0; i0++) {
                    s.set(0, i0);
                    var min1: int = s.min(1);
                    var max1: int = s.max(1);
                    for (var i1: int = min1; i1<=max1; i1++) {
                        if (bump) a(i0, i1) = a(i0, i1) + 1;
                        grid.set(i0, i1, a(i0,i1));
                    }
                }
            } else if (r.rank==3) {
                var min0: int = s.min(0);
                var max0: int = s.max(0);
                for (var i0: int = min0; i0<=max0; i0++) {
                    s.set(0, i0);
                    var min1: int = s.min(1);
                    var max1: int = s.max(1);
                    for (var i1: int = min1; i1<=max1; i1++) {
                        s.set(1, i1);
                        var min2: int = s.min(2);
                        var max2: int = s.max(2);
                        for (var i2: int = min2; i2<=max2; i2++) {
                            if (bump) a(i0, i1, i2) = a(i0, i1, i2) + 1;
                            grid.set(i0, i1, i2, a(i0,i1,i2));
                        }
                    }
                }
            }
        }
        grid.pr(r.rank);

        pr("  iterator");
        prArray1(a, /*bump*/ false); // XXX use bump, update tests
    }

    def prArray1(a: Array[double], bump: boolean): void = {
        // iterator api
        var grid: Grid = new Grid();
        for (p:Point in a.region) {
            //var v: double = a(p as Point(a.rank));
            if (p.rank==1) {
                if (bump) a(p(0)) = a(p(0)) + 1;
                grid.set(p(0), a(p(0)));
            } else if (p.rank==2) {
                if (bump) a(p(0), p(1)) = a(p(0), p(1)) + 1;
                grid.set(p(0), p(1), a(p(0),p(1)));
            } else if (p.rank==3) {
                if (bump) a(p(0), p(1), p(2)) = a(p(0), p(1), p(2)) + 1;
                grid.set(p(0), p(1), p(2), a(p(0),p(1),p(2)));
            }
        }
        grid.pr(a.rank);
    }


    def pr(s: String): void = {
        out.println(s);
    }

    def r(a: int, b: int, c: int, d: int): Region(2) {
        //return Region.makeRectangular([a,c], [b,d]);
        return [a..b, c..d] as Region(2);
    }

    // a simple mechanism of somewhat dubious utility to allow
    // semi-symbolic specification of halfspaces. For example
    // X0-Y1 >= n is specified as addHalfspace(X(0)-Y(1), GE, n)
    //
    // XXX coefficients must be -1,0,+1; can allow larger coefficients
    // by increasing # bits per coeff

    const ZERO = 0xAAAAAAA;
    const GE = 0;
    const LE = 1;
    def X(axis: int) = 0x1<<2*axis;

    public def reg(rank: int, var coeff: int, op: int, k: int): Region(rank) {
        coeff += ZERO;
        val as = Rail.makeVar[int](rank);
        for (var i: int = 0; i<rank; i++) {
            var a: int = (coeff&3) - 2;
            as(i) = op==LE? a : - a;
            coeff = coeff >> 2;
        }
        return Region.makeHalfspace(as, op==LE? -k : k);
    }



}

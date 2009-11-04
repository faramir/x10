/**
 * Distribution algebra
 *
 * (Was DistAlgebra, but NOTE: the semantics of cyclic distributions
 * have changed)
 */

class PolyDistAlgebra1 extends TestDist {

    // XXX move to Place.toString
    def s(p:Place) = "place(id=" + p.id + ")";

    def check(name: String, d: Dist(2)): void = {

        prDist(name, d);

        //Check range restriction to a place
        for (var k: int = 0; k<Place.MAX_PLACES; k++) {
            var p: Place = Place.place(k);
            var dp: Dist(2) = d | p;
            prDist(name + "|" + s(p), dp);
        }
    }

    public def run() {

        val r1 = r(0,1,0,7);
        prArray("r1", r1);

        val r2 = r(4,5,0,7);
        prArray("r2", r2);

        val r3 = r(0,7,4,5);
        prArray("r3", r3);

        val r12 = r1 || (r2);
        pr("r12" + r12);

        val r12a3 = r12 && (r3);
        pr("r12a3" + r12a3);

        val r123 = r1 || (r2) || (r3);
        pr("r123" + r123);

        val r12m3 = r12 - (r3);
        pr("r12m3" + r12m3);

        val d123x0 = Dist.makeCyclic(r123, 0);
        check("d123x0", d123x0);

        val d123x1 = Dist.makeCyclic(r123, 1);
        check("d123x1", d123x1);

        //
        // dist op region
        //

        val d123x0r12a3 = d123x0 - (r12a3);
        prDist("d123x0r12a3", d123x0r12a3);

        val d123x1r12a3 = d123x1 - (r12a3);
        prDist("d123x1r12a3", d123x1r12a3);

        val d123x0r12m3 = d123x0 | (r12m3);
        prDist("d123x0r12m3", d123x0r12m3);

        val d123x1r12m3 = d123x1 | (r12m3);
        prDist("d123x1r12m3", d123x1r12m3);


        //
        // dist - dist
        //

        val d1 = d123x0 - (d123x0r12m3);
        prDist("d1 = d123x0 - d123x0r12m3", d1);
        val d3 = d123x0 | (r3);
        prDist("d3 = d123x0 | r3", d3);
        pr("d1.equals(d3) checks " + d1.equals(d3));
        pr("d1.isSubdistribution(d123x0) checks " + d1.isSubdistribution(d123x0));
        pr("!d123x0.isSubdistribution(d1) checks " + !d123x0.isSubdistribution(d1));

        val d1x = d123x0 - (d123x1r12m3);
        prDist("d1x = d123x0 - d123x1r12m3", d1x);
        pr("d1x.isSubdistribution(d123x0) checks " + d1x.isSubdistribution(d123x0));
        pr("!d123x0.isSubdistribution(d1x) checks " + !d123x0.isSubdistribution(d1x));


        //
        // dist && dist
        //

        val d2 = d123x0r12m3 && (d123x0);
        prDist("d2 = d123x0r12m3 && d123x0", d2);
        pr("d2.equals(d123x0r12m3) checks " + d2.equals(d123x0r12m3));

        val d2x = d123x0r12m3 && (d123x1);
        prDist("d2x = d123x0r12m3 && d123x1", d2x);


        //
        // dist overlay dist
        //

        val d5 = d123x0 | (r12);
        prDist("d5 = d123x0 | r12", d5);
        val d4 = d5.overlay(d3);
        prDist("d4 = d5.overlay(d3)", d4);
        pr("d4.equals(d123x0) checks " + d4.equals(d123x0));


        val d5x = d123x1 | (r12);
        prDist("d5x = d123x1 | r12", d5);
        val d4x = d5x.overlay(d3);
        prDist("d4x = d5x.overlay(d3)", d4x);


        //
        // dist union dist
        //

        val d6 = d123x0r12a3 || (d123x0r12m3);
        prDist("d6 = d123x0r12a3 || d123x0r12m3", d6);
        pr("d6.equals(d5) checks " + d6.equals(d5));


       /* new E("d6x = d123x0 || d123x1") {
            def run(): void = {
                val d6x = d123x0.$or(d123x1);
                prDist("d6x = d123x0 || d123x1", d6x);
            }
        };*/

        return status();
    }


    def expected() =
        "--- PolyDistAlgebra1: r1\n"+
        "rank 2\n"+
        "rect true\n"+
        "zeroBased true\n"+
        "rail false\n"+
        "isConvex() true\n"+
        "size() 16\n"+
        "region: [0..1,0..7]\n"+
        "  poly\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  0 1 2 3 4 5 6 7 . . \n"+
        "  iterator\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  0 1 2 3 4 5 6 7 . . \n"+
        "--- PolyDistAlgebra1: r2\n"+
        "rank 2\n"+
        "rect true\n"+
        "zeroBased false\n"+
        "rail false\n"+
        "isConvex() true\n"+
        "size() 16\n"+
        "region: [4..5,0..7]\n"+
        "  poly\n"+
        "    4  0 4 8 2 6 0 4 8 . . \n"+
        "    5  0 5 0 5 0 5 0 5 . . \n"+
        "  iterator\n"+
        "    4  0 4 8 2 6 0 4 8 . . \n"+
        "    5  0 5 0 5 0 5 0 5 . . \n"+
        "--- PolyDistAlgebra1: r3\n"+
        "rank 2\n"+
        "rect true\n"+
        "zeroBased false\n"+
        "rail false\n"+
        "isConvex() true\n"+
        "size() 16\n"+
        "region: [0..7,4..5]\n"+
        "  poly\n"+
        "    0  . . . . 0 0 . . . . \n"+
        "    1  . . . . 4 5 . . . . \n"+
        "    2  . . . . 8 0 . . . . \n"+
        "    3  . . . . 2 5 . . . . \n"+
        "    4  . . . . 6 0 . . . . \n"+
        "    5  . . . . 0 5 . . . . \n"+
        "    6  . . . . 4 0 . . . . \n"+
        "    7  . . . . 8 5 . . . . \n"+
        "  iterator\n"+
        "    0  . . . . 0 0 . . . . \n"+
        "    1  . . . . 4 5 . . . . \n"+
        "    2  . . . . 8 0 . . . . \n"+
        "    3  . . . . 2 5 . . . . \n"+
        "    4  . . . . 6 0 . . . . \n"+
        "    5  . . . . 0 5 . . . . \n"+
        "    6  . . . . 4 0 . . . . \n"+
        "    7  . . . . 8 5 . . . . \n"+
        "r12([0..1,0..7] || [4..5,0..7])\n"+
        "r12a3([0..1,4..5] || [4..5,4..5])\n"+
        "r123([0..1,0..7] || [4..5,0..7] || [2..3,4..5] || [6..7,4..5])\n"+
        "r12m3([0..1,0..3] || [0..1,6..7] || [4..5,0..3] || [4..5,6..7])\n"+
        "--- d123x0: Dist(0->([0..0,0..7] || [4..4,0..7]),1->([1..1,0..7] || [5..5,0..7]),2->([2..2,4..5] || [6..6,4..5]),3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "--- d123x0|place(id=0): Dist(0->([0..0,0..7] || [4..4,0..7]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1\n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "--- d123x0|place(id=1): Dist(1->([1..1,0..7] || [5..5,0..7]))\n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4\n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "--- d123x0|place(id=2): Dist(2->([2..2,4..5] || [6..6,4..5]))\n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3\n"+
        "    4\n"+
        "    5\n"+
        "    6  . . . . 2 2 . . . . \n"+
        "--- d123x0|place(id=3): Dist(3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4\n"+
        "    5\n"+
        "    6\n"+
        "    7  . . . . 3 3 . . . . \n"+
        "--- d123x1: Dist(0->([0..1,0..0] || [4..5,0..0] || [0..1,4..4] || [4..5,4..4] || [2..3,4..4] || [6..7,4..4]),1->([0..1,1..1] || [4..5,1..1] || [0..1,5..5] || [4..5,5..5] || [2..3,5..5] || [6..7,5..5]),2->([0..1,2..2] || [4..5,2..2] || [0..1,6..6] || [4..5,6..6]),3->([0..1,3..3] || [4..5,3..3] || [0..1,7..7] || [4..5,7..7]))\n"+
        "    0  0 1 2 3 0 1 2 3 . . \n"+
        "    1  0 1 2 3 0 1 2 3 . . \n"+
        "    2  . . . . 0 1 . . . . \n"+
        "    3  . . . . 0 1 . . . . \n"+
        "    4  0 1 2 3 0 1 2 3 . . \n"+
        "    5  0 1 2 3 0 1 2 3 . . \n"+
        "    6  . . . . 0 1 . . . . \n"+
        "    7  . . . . 0 1 . . . . \n"+
        "--- d123x1|place(id=0): Dist(0->([0..1,0..0] || [4..5,0..0] || [0..1,4..4] || [4..5,4..4] || [2..3,4..4] || [6..7,4..4]))\n"+
        "    0  0 . . . 0 . . . . . \n"+
        "    1  0 . . . 0 . . . . . \n"+
        "    2  . . . . 0 . . . . . \n"+
        "    3  . . . . 0 . . . . . \n"+
        "    4  0 . . . 0 . . . . . \n"+
        "    5  0 . . . 0 . . . . . \n"+
        "    6  . . . . 0 . . . . . \n"+
        "    7  . . . . 0 . . . . . \n"+
        "--- d123x1|place(id=1): Dist(1->([0..1,1..1] || [4..5,1..1] || [0..1,5..5] || [4..5,5..5] || [2..3,5..5] || [6..7,5..5]))\n"+
        "    0  . 1 . . . 1 . . . . \n"+
        "    1  . 1 . . . 1 . . . . \n"+
        "    2  . . . . . 1 . . . . \n"+
        "    3  . . . . . 1 . . . . \n"+
        "    4  . 1 . . . 1 . . . . \n"+
        "    5  . 1 . . . 1 . . . . \n"+
        "    6  . . . . . 1 . . . . \n"+
        "    7  . . . . . 1 . . . . \n"+
        "--- d123x1|place(id=2): Dist(2->([0..1,2..2] || [4..5,2..2] || [0..1,6..6] || [4..5,6..6]))\n"+
        "    0  . . 2 . . . 2 . . . \n"+
        "    1  . . 2 . . . 2 . . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  . . 2 . . . 2 . . . \n"+
        "    5  . . 2 . . . 2 . . . \n"+
        "--- d123x1|place(id=3): Dist(3->([0..1,3..3] || [4..5,3..3] || [0..1,7..7] || [4..5,7..7]))\n"+
        "    0  . . . 3 . . . 3 . . \n"+
        "    1  . . . 3 . . . 3 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  . . . 3 . . . 3 . . \n"+
        "    5  . . . 3 . . . 3 . . \n"+
        "--- d123x0r12a3: Dist(0->([0..0,4..5] || [4..4,4..5]),1->([1..1,4..5] || [5..5,4..5]))\n"+
        "    0  . . . . 0 0 . . . . \n"+
        "    1  . . . . 1 1 . . . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  . . . . 0 0 . . . . \n"+
        "    5  . . . . 1 1 . . . . \n"+
        "--- d123x1r12a3: Dist(0->([0..1,4..4] || [4..5,4..4]),1->([0..1,5..5] || [4..5,5..5]))\n"+
        "    0  . . . . 0 1 . . . . \n"+
        "    1  . . . . 0 1 . . . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  . . . . 0 1 . . . . \n"+
        "    5  . . . . 0 1 . . . . \n"+
        "--- d123x0r12m3: Dist(0->([0..0,0..3] || [0..0,6..7] || [4..4,0..3] || [4..4,6..7]),1->([1..1,0..3] || [1..1,6..7] || [5..5,0..3] || [5..5,6..7]))\n"+
        "    0  0 0 0 0 . . 0 0 . . \n"+
        "    1  1 1 1 1 . . 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 . . 0 0 . . \n"+
        "    5  1 1 1 1 . . 1 1 . . \n"+
        "--- d123x1r12m3: Dist(0->([0..1,0..0] || [4..5,0..0]),1->([0..1,1..1] || [4..5,1..1]),2->([0..1,2..2] || [4..5,2..2] || [0..1,6..6] || [4..5,6..6]),3->([0..1,3..3] || [4..5,3..3] || [0..1,7..7] || [4..5,7..7]))\n"+
        "    0  0 1 2 3 . . 2 3 . . \n"+
        "    1  0 1 2 3 . . 2 3 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 1 2 3 . . 2 3 . . \n"+
        "    5  0 1 2 3 . . 2 3 . . \n"+
        "--- d1 = d123x0 - d123x0r12m3: Dist(0->([0..0,4..5] || [4..4,4..5]),1->([1..1,4..5] || [5..5,4..5]),2->([2..2,4..5] || [6..6,4..5]),3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    0  . . . . 0 0 . . . . \n"+
        "    1  . . . . 1 1 . . . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  . . . . 0 0 . . . . \n"+
        "    5  . . . . 1 1 . . . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "--- d3 = d123x0 | r3: Dist(0->([0..0,4..5] || [4..4,4..5]),1->([1..1,4..5] || [5..5,4..5]),2->([2..2,4..5] || [6..6,4..5]),3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    0  . . . . 0 0 . . . . \n"+
        "    1  . . . . 1 1 . . . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  . . . . 0 0 . . . . \n"+
        "    5  . . . . 1 1 . . . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "d1.equals(d3) checks true\n"+
        "d1.isSubdistribution(d123x0) checks true\n"+
        "!d123x0.isSubdistribution(d1) checks true\n"+
        "--- d1x = d123x0 - d123x1r12m3: Dist(0->([0..0,1..7] || [4..4,1..7]),1->([1..1,0..0] || [1..1,2..7] || [5..5,0..0] || [5..5,2..7]),2->([2..2,4..5] || [6..6,4..5]),3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    0  . 0 0 0 0 0 0 0 . . \n"+
        "    1  1 . 1 1 1 1 1 1 . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  . 0 0 0 0 0 0 0 . . \n"+
        "    5  1 . 1 1 1 1 1 1 . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "d1x.isSubdistribution(d123x0) checks true\n"+
        "!d123x0.isSubdistribution(d1x) checks true\n"+
        "--- d2 = d123x0r12m3 && d123x0: Dist(0->([0..0,0..3] || [0..0,6..7] || [4..4,0..3] || [4..4,6..7]),1->([1..1,0..3] || [1..1,6..7] || [5..5,0..3] || [5..5,6..7]))\n"+
        "    0  0 0 0 0 . . 0 0 . . \n"+
        "    1  1 1 1 1 . . 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 . . 0 0 . . \n"+
        "    5  1 1 1 1 . . 1 1 . . \n"+
        "d2.equals(d123x0r12m3) checks true\n"+
        "--- d2x = d123x0r12m3 && d123x1: Dist(0->([0..0,0..0] || [4..4,0..0]),1->([1..1,1..1] || [5..5,1..1]))\n"+
        "    0  0 . . . . . . . . . \n"+
        "    1  . 1 . . . . . . . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 . . . . . . . . . \n"+
        "    5  . 1 . . . . . . . . \n"+
        "--- d5 = d123x0 | r12: Dist(0->([0..0,0..7] || [4..4,0..7]),1->([1..1,0..7] || [5..5,0..7]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "--- d4 = d5.overlay(d3): Dist(0->([0..0,0..3] || [0..0,6..7] || [4..4,0..3] || [4..4,6..7] || [0..0,4..5] || [4..4,4..5]),1->([1..1,0..3] || [1..1,6..7] || [5..5,0..3] || [5..5,6..7] || [1..1,4..5] || [5..5,4..5]),2->([2..2,4..5] || [6..6,4..5]),3->([3..3,4..5] || [7..7,4..5]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "d4.equals(d123x0) checks true\n"+
        "--- d5x = d123x1 | r12: Dist(0->([0..0,0..7] || [4..4,0..7]),1->([1..1,0..7] || [5..5,0..7]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "--- d4x = d5x.overlay(d3): Dist(0->([0..1,0..0] || [4..5,0..0] || [0..0,4..5] || [4..4,4..5]),1->([0..1,1..1] || [4..5,1..1] || [1..1,4..5] || [5..5,4..5]),2->([0..1,2..2] || [4..5,2..2] || [0..1,6..6] || [4..5,6..6] || [2..2,4..5] || [6..6,4..5]),3->([0..1,3..3] || [4..5,3..3] || [0..1,7..7] || [4..5,7..7] || [3..3,4..5] || [7..7,4..5]))\n"+
        "    0  0 1 2 3 0 0 2 3 . . \n"+
        "    1  0 1 2 3 1 1 2 3 . . \n"+
        "    2  . . . . 2 2 . . . . \n"+
        "    3  . . . . 3 3 . . . . \n"+
        "    4  0 1 2 3 0 0 2 3 . . \n"+
        "    5  0 1 2 3 1 1 2 3 . . \n"+
        "    6  . . . . 2 2 . . . . \n"+
        "    7  . . . . 3 3 . . . . \n"+
        "--- d6 = d123x0r12a3 || d123x0r12m3: Dist(0->([0..0,4..5] || [4..4,4..5] || [0..0,0..3] || [0..0,6..7] || [4..4,0..3] || [4..4,6..7]),1->([1..1,4..5] || [5..5,4..5] || [1..1,0..3] || [1..1,6..7] || [5..5,0..3] || [5..5,6..7]))\n"+
        "    0  0 0 0 0 0 0 0 0 . . \n"+
        "    1  1 1 1 1 1 1 1 1 . . \n"+
        "    2\n"+
        "    3\n"+
        "    4  0 0 0 0 0 0 0 0 . . \n"+
        "    5  1 1 1 1 1 1 1 1 . . \n"+
        "d6.equals(d5) checks true\n"+
        "d6x = d123x0 || d123x1: regions are not disjoint\n";
    

    public static def main(Rail[String]) {
        new PolyDistAlgebra1().execute();
    }
}
    

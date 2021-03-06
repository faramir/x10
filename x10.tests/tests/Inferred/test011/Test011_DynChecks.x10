/**
 * Variation on the example Test09.
 *
 * The constraint on y depends on x which is defined earlier.
 *
 */
//OPTIONS: -STATIC_CHECKS=false -CONSTRAINT_INFERENCE=false -VERBOSE_INFERENCE=true



import harness.x10Test;
import x10.compiler.InferGuard;

public class Test011_DynChecks extends x10Test {
    static def assert_eq(x: Long, y: Long{ self == x}){}

    @InferGuard
    static def f(x: Long, y: Long) {
        assert_eq(x, y);   // <=   x == y
        assert_eq(42, x);  // <=  42 == y
    }

    public def run(): boolean {
	Test011_DynChecks.f(42, 42);
        return true;
    }

    public static def main(Rail[String]) {
    	new Test011_DynChecks().execute();
    }

}

//OPTIONS: -STATIC_CHECKS=false -CONSTRAINT_INFERENCE=false -VERBOSE_INFERENCE=true



import harness.x10Test;

public class Test022_DynCheckException extends x10Test {

    public def mustFailRun(): boolean {
	Test022_DynChecks.f(true, 1, 1, 1);
        return true;
    }

    public def run() {
        try { mustFailRun(); return false; } catch (FailedDynamicCheckException) {}
        return true;
    }

    public static def main(Rail[String]) {
    	new Test022_DynCheckException().execute();
    }

}

/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package x10.lang;

import x10.compiler.Native;
import x10.compiler.NativeRep;
import x10.io.PrintStream;

@NativeRep("java", "java.lang.Throwable", null, null)
public value Throwable {
	val cause: Box[Throwable];
    val message: String;
    public def this() = this("");
    public def this(message: String) {
    	super();
    	this.cause = null;
    	this.message = message;
    }
    public def this(cause: Throwable) = this("", cause);
    public def this(message: String, cause: Throwable): Throwable {
    	super();
    	this.cause = cause to Box[Throwable]; // BUG: should autobox
    	this.message = message;
    }
    
    @Native("java", "#0.getMessage()")
    public def getMessage() = message;
    
    @Native("java", "#0.getLocalizedMessage()")
    public incomplete def getLocalizedMessage(): String;
    
    @Native("java", "(x10.core.Box<java.lang.Throwable>) x10.core.Box.<java.lang.Throwable>make(new x10.core.Box.RTT(new x10.types.RuntimeType<java.lang.Throwable>(java.lang.Throwable.class)), #0.getCause())")
    public final def getCause(): Box[Throwable] = cause;
    
    @Native("java", "#0.toString()")
    public def toString() = className() + ": " + getMessage();
    
    @Native("java", "#0.printStackTrace()")
    public  def printStackTrace() {
    	printStackTrace(System.err);
    }
    
    @Native("java", "#0.printStackTrace((java.io.PrintStream) (#1).nativeWriter())")
    public def printStackTrace(p: PrintStream ) {
    	// We do not bother to walk the stack and insert stack elements
    	// for the C/C++ implementation (for now).
    	p.println(this);
    	p.println("Stack trace unavailable. So cry your heart out.");
    }
    /*
    public void printStackTrace(java.io.PrintWriter);
    public synchronized native java.lang.Throwable fillInStackTrace();
    public java.lang.StackTraceElement[] getStackTrace();
    public void setStackTrace(java.lang.StackTraceElement[]);
*/
}

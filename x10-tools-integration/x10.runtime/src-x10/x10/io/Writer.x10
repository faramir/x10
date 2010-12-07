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

package x10.io;

import x10.compiler.Native;

/**
 * Usage:
 *
 * try {
 *   val in = new File(inputFileName);
 *   val out = new File(outputFileName);
 *   val p = out.printer();
 *   for (line in in.lines()) {
 *      line = line.chop();
 *      p.println(line);
 *   }
 * }
 * catch (IOException e) { }
 */    
public abstract class Writer {
    public abstract def close(): void ; //throws IOException
    public abstract def flush(): void ; //throws IOException
    public abstract def write(x: Byte): void ; //throws IOException

    public def writeByte(x: Byte): void //throws IOException 
    = Marshal.BYTE.write(this, x);
    public def writeChar(x: Char): void //throws IOException 
    =  Marshal.CHAR.write(this, x);
    public def writeShort(x: Short): void //throws IOException 
    = Marshal.SHORT.write(this, x);
    public def writeInt(x: Int): void //throws IOException 
    = Marshal.INT.write(this, x);
    public def writeLong(x: Long): void //throws IOException 
    = Marshal.LONG.write(this, x);
    public def writeFloat(x: Float): void //throws IOException 
    = Marshal.FLOAT.write(this, x);
    public def writeDouble(x: Double): void //throws IOException 
    = Marshal.DOUBLE.write(this, x);
    public def writeBoolean(x: Boolean): void //throws IOException 
    = Marshal.BOOLEAN.write(this, x);
    
    // made final to satisfy the restrictions on template functions in c++
    public final def write[T](m: Marshal[T], x: T): void //throws IOException 
    = m.write(this, x);

    public def write(buf: Rail[Byte]): void //throws IOException 
    {
        write(buf, 0, buf.length);
    }

    public def write(buf: Rail[Byte], off: Int, len: Int): void //throws IOException 
    {
        for (var i: Int = off; i < off+len; i++) {
            write(buf(i));
        }
    }
    
    @Native("java", "new java.lang.Object() { x10.core.io.OutputStream eval(final x10.io.Writer w) { return new x10.core.io.OutputStream() { public void write(int x) throws x10.io.IOException { w.write((byte) x); } }; } }.eval(#0)")
    private def oos(): OutputStreamWriter.OutputStream {
        return oos();
    }
    
    // DO NOT CALL from X10 code -- only used in @Native annotations
    public def getNativeOutputStream(): OutputStreamWriter.OutputStream = oos();
}

/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

package x10.core;

import x10.rtt.Type;
import x10.rtt.Types;

/**
 * Represents a boxed Long value. Boxed representation is used when casting
 * an Long value to type Any, parameter type T or superinterfaces such
 * as Comparable<Long>.
 */
final public class Long extends Numeric implements java.lang.Comparable<Long>,
    x10.lang.Arithmetic<Long>, x10.lang.Bitwise<Long>, x10.util.Ordered<Long>
{
    private static final long serialVersionUID = 1L;
    
    public static final x10.rtt.RuntimeType<?> $RTT = Types.LONG;
    public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
    public x10.rtt.Type<?> $getParam(int i) {return null;}

    final long $value;

    private Long(long value) {
        this.$value = value;
    }

    private abstract static class Cache {
        static final int low = -128;
        static final int high = 127;
        static final Long cache[] = new Long[high - low + 1];
        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new Long(low + i);
            }
        }
    }

    public static Long $box(long value) {
        if (Cache.low <= value && value <= Cache.high) {
            return Cache.cache[(int)value - Cache.low];
        }
        return new Long(value);
    }

    public static long $unbox(Long obj) {
        return obj.$value;
    }
    
    public static long $unbox(Object obj) {
        if (obj instanceof Long) return ((Long)obj).$value;
        else return ((java.lang.Long)obj).longValue();
    }
    
    // make $box/$unbox idempotent
    public static Long $box(Long obj) {
        return obj;
    }

    public static long $unbox(long value) {
        return value;
    }
    
    public boolean _struct_equals$O(Object obj) {
        if (obj instanceof Long && ((Long) obj).$value == $value)
            return true;
        return false;
    }
    
    public boolean equals(Object value) {
        if (value instanceof Long) {
            return ((Long) value).$value == $value;
        } else if (value instanceof java.lang.Long) { // integer literals come here as Long autoboxed values
            return ((java.lang.Long) value).longValue() == $value;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (int)$value;
    }

    @Override
    public java.lang.String toString() {
        return java.lang.Long.toString($value);
    }
    
    // implements Comparable<Long>
    public int compareTo(Long o) {
        if ($value > o.$value) return 1;
        else if ($value < o.$value) return -1;
        return 0;
    }
    
    // implements Arithmetic<Long>
    public Long $plus$G() { return this; }
    public Long $minus$G() { return Long.$box(-$value); }
    public Long $plus(Long b, Type t) { return Long.$box($value + b.$value); }
    public Long $minus(Long b, Type t) { return Long.$box($value - b.$value); }
    public Long $times(Long b, Type t) { return Long.$box($value * b.$value); }
    public Long $over(Long b, Type t) { return Long.$box($value / b.$value); }
    
    // implements Bitwise<Long>
    public Long $tilde$G() { return Long.$box(~$value); }
    public Long $ampersand(Long b, Type t) { return Long.$box($value & b.$value); }
    public Long $bar(Long b, Type t) { return Long.$box($value | b.$value); }
    public Long $caret(Long b, Type t) { return Long.$box($value ^ b.$value); }
    public Long $left$G(final int count) { return Long.$box($value << count); }
    public Long $right$G(final int count) { return Long.$box($value >> count); }
    public Long $unsigned_right$G(final int count) { return Long.$box($value >>> count); }        
    
    // implements Ordered<Long>. Rely on autoboxing of booleans
    public Object $lt(Long b, Type t) { return ($value < b.$value); }
    public Object $gt(Long b, Type t) { return ($value > b.$value); }
    public Object $le(Long b, Type t) { return ($value <= b.$value); }
    public Object $ge(Long b, Type t) { return ($value >= b.$value); }
    
    // extends abstract class java.lang.Number
    @Override
    public int intValue() {
        return (int)$value;
    }
    @Override
    public long longValue() {
        return (long)$value;
    }
    @Override
    public float floatValue() {
        return (float)$value;
    }
    @Override
    public double doubleValue() {
        return (double)$value;
    }
}
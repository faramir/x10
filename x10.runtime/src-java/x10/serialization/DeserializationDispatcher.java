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

package x10.serialization;


import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeserializationDispatcher implements SerializationConstants {

    // Should start issuing serializationID's from 1 cause the serializationID 0 is used to indicate a null value.
    // We first increment nextSerializationID before issuing the serializationID hence initialize to NULL_ID
    private static short nextSerializationID = NULL_ID;

    private static List<DeserializationInfo> idToDeserializationInfo = new ArrayList<DeserializationInfo>();
    private static List<Method> idToDeserializermethod = new ArrayList<Method>();
    private static Map<String, Short> classNameToId = new HashMap<String, Short>();
    
    private static Map<String,Method> deserializerMethods = new HashMap<String,Method>();

    public static short addDispatcher(Class<?> clazz) {
        if (nextSerializationID == NULL_ID) {
            classNameToId.put(null, nextSerializationID);
            idToDeserializationInfo.add(nextSerializationID, null);
            idToDeserializermethod.add(nextSerializationID, null);
            nextSerializationID++;
            try {
                add(Class.forName("java.lang.String"), false);
                add(Class.forName("java.lang.Float"), false);
                add(Class.forName("java.lang.Double"), false);
                add(Class.forName("java.lang.Integer"), false);
                add(Class.forName("java.lang.Boolean"), false);
                add(Class.forName("java.lang.Byte"), false);
                add(Class.forName("java.lang.Short"), false);
                add(Class.forName("java.lang.Long"), false);
                add(Class.forName("java.lang.Character"), false);
                
                add(Class.forName("x10.rtt.AnyType"), false);
                add(Class.forName("x10.rtt.BooleanType"), false);
                add(Class.forName("x10.rtt.ByteType"), false);
                add(Class.forName("x10.rtt.CharType"), false);
                add(Class.forName("x10.rtt.DoubleType"), false);
                add(Class.forName("x10.rtt.FloatType"), false);
                add(Class.forName("x10.rtt.IntType"), false);
                add(Class.forName("x10.rtt.LongType"), false);
                add(Class.forName("x10.rtt.ShortType"), false);
                add(Class.forName("x10.rtt.StringType"), false);
                add(Class.forName("x10.rtt.UByteType"), false);
                add(Class.forName("x10.rtt.UIntType"), false);
                add(Class.forName("x10.rtt.ULongType"), false);
                add(Class.forName("x10.rtt.UShortType"), false);
            } catch (ClassNotFoundException e) {
                // This will never happen
            }
        }
        add(clazz, true);
        return (short) (nextSerializationID-1);
    }

    public static short addDispatcher(Class<?> clazz, String alternate) {
        short serializationID = addDispatcher(clazz);
        classNameToId.put(alternate, serializationID);
        return serializationID;
    }

    private static void add(Class<?> clazz, boolean addDeserializerMethod) {
        classNameToId.put(clazz.getName(), nextSerializationID);
        DeserializationInfo deserializationInfo = new DeserializationInfo(clazz, nextSerializationID);
        idToDeserializationInfo.add(nextSerializationID, deserializationInfo);
        if (addDeserializerMethod && !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))) {
            idToDeserializermethod.add(nextSerializationID, getDeserializerMethod(clazz));
        } else {
            idToDeserializermethod.add(nextSerializationID, null);
        }
        nextSerializationID++;
    }

    public static synchronized Method getDeserializerMethod(String className) {
        Method m = deserializerMethods.get(className);
        if (null == m) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.err.println("DeserializationDispatcher: failed to load class "+className);
                throw new RuntimeException(e);
            }
            try {
                m = clazz.getMethod("$_deserializer", X10JavaDeserializer.class);
            } catch (NoSuchMethodException e) {
                System.err.println("DeserializationDispatcher: class "+className+" does not have a $_deserializer method");
                throw new RuntimeException(e);
            }
            m.setAccessible(true);            
            
            deserializerMethods.put(className,m);
        }
        return m;
    }
    
    public static Method getDeserializerMethod(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("$_deserializer", X10JavaDeserializer.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    public static String getClassNameForID(short serializationID, X10JavaDeserializer deserializer) {
         if (serializationID == JAVA_CLASS_ID) {
            try {
                return deserializer.readStringValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return getClassForID(serializationID, deserializer).getName();
    }

    public static Class<?> getClassForID(short serializationID, X10JavaDeserializer deserializer) {
        if (serializationID == JAVA_CLASS_ID) {
            try {
                String className = deserializer.readStringValue();
                return Class.forName(className);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return idToDeserializationInfo.get(serializationID).clazz;
    }

    public static short getSerializationIDForClassName(String str) {
        Short serializationID = classNameToId.get(str);

        // When there are inner classes the type name is of the form A.B.C where
        // C is an inner class of B. But classNameToId uses class.getName() to store
        // the class names thus the following code is a workaround for this issue. We
        // need to have the class name cause we do class.forName using this name
        // but the typenames are stored at A.B.C so this disconnect is inevitable
        int i;
        String s = str;
        while (serializationID == null && ((i = s.lastIndexOf(".")) > 0)) {
            s = s.substring(0, i) + "$" + s.substring(i + 1);
            serializationID = classNameToId.get(s);
        }
        if (serializationID == null) {
            return -1;
        }
        return serializationID;
    }

    public static void registerHandlers() {
        x10.x10rt.MessageHandlers.registerHandlers();
    }

    private static class DeserializationInfo {
        public Class<?> clazz;
        public short serializationID;

        private DeserializationInfo(Class<?> clazz, short serializationID) {
            this.clazz = clazz;
            this.serializationID = serializationID;
        }
    }
}
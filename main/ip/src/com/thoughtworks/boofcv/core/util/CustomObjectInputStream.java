/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.boofcv.core.util;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.core.util.FastStack;

import java.io.*;
import java.util.Map;

public class CustomObjectInputStream extends ObjectInputStream {

    private FastStack callbacks = new FastStack(1);
    private final XStreamClassLoader classLoader;

    private static final String DATA_HOLDER_KEY = CustomObjectInputStream.class.getName();

    public static interface StreamCallback {
        Object readFromStream() throws IOException;
        Map readFieldsFromStream() throws IOException;
        void defaultReadObject() throws IOException;
        void registerValidation(ObjectInputValidation validation, int priority) throws NotActiveException, InvalidObjectException;
        void close() throws IOException;
    }

    /**
     * @deprecated As of 1.4 use {@link #getInstance(com.thoughtworks.xstream.converters.DataHolder, StreamCallback, XStreamClassLoader)}
     */
    public static CustomObjectInputStream getInstance(DataHolder whereFrom, CustomObjectInputStream.StreamCallback callback) {
        return getInstance(whereFrom, callback, null);
    }

    public static synchronized CustomObjectInputStream getInstance(DataHolder whereFrom, CustomObjectInputStream.StreamCallback callback, XStreamClassLoader classLoaderReference) {
        try {
            CustomObjectInputStream result = (CustomObjectInputStream) whereFrom.get(DATA_HOLDER_KEY);
            if (result == null) {
                result = new CustomObjectInputStream(callback, classLoaderReference);
                whereFrom.put(DATA_HOLDER_KEY, result);
            } else {
                result.pushCallback(callback);
            }
            return result;
        } catch (IOException e) {
            throw new ConversionException("Cannot create CustomObjectStream", e);
        }
    }

    /**
     * Warning, this object is expensive to create (due to functionality inherited from superclass).
     * Use the static fetch() method instead, wherever possible.
     *
     * @see #getInstance(com.thoughtworks.xstream.converters.DataHolder, StreamCallback, XStreamClassLoader)
     */
    public CustomObjectInputStream(StreamCallback callback, XStreamClassLoader classLoader) throws IOException, SecurityException {
        super();
        this.callbacks.push(callback);
        this.classLoader = classLoader;
    }

    /**
     * Allows the CustomObjectInputStream (which is expensive to create) to be reused.
     */
    public void pushCallback(StreamCallback callback) {
        this.callbacks.push(callback);
    }
    
    public StreamCallback popCallback(){
        return (StreamCallback) this.callbacks.pop();
    }
    
    public StreamCallback peekCallback(){
        return (StreamCallback) this.callbacks.peek();
    }
    
    protected Class resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
        if (classLoader == null) {
            return super.resolveClass(desc);
        } else {
            return classLoader.loadClass(desc.getName());
        }
    }

    public void defaultReadObject() throws IOException {
        peekCallback().defaultReadObject();
    }

    protected Object readObjectOverride() throws IOException {
        return peekCallback().readFromStream();
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return readObject();
    }

    public boolean readBoolean() throws IOException {
        return ((Boolean)peekCallback().readFromStream()).booleanValue();
    }

    public byte readByte() throws IOException {
        return ((Byte)peekCallback().readFromStream()).byteValue();
    }

    public int readUnsignedByte() throws IOException {
        int b = ((Byte)peekCallback().readFromStream()).byteValue();
        if (b < 0) {
            b += Byte.MAX_VALUE;
        }
        return b;
    }

    public int readInt() throws IOException {
        return ((Integer)peekCallback().readFromStream()).intValue();
    }

    public char readChar() throws IOException {
        return ((Character)peekCallback().readFromStream()).charValue();
    }

    public float readFloat() throws IOException {
        return ((Float)peekCallback().readFromStream()).floatValue();
    }

    public double readDouble() throws IOException {
        return ((Double)peekCallback().readFromStream()).doubleValue();
    }

    public long readLong() throws IOException {
        return ((Long)peekCallback().readFromStream()).longValue();
    }

    public short readShort() throws IOException {
        return ((Short)peekCallback().readFromStream()).shortValue();
    }

    public int readUnsignedShort() throws IOException {
        int b = ((Short)peekCallback().readFromStream()).shortValue();
        if (b < 0) {
            b += Short.MAX_VALUE;
        }
        return b;
    }

    public String readUTF() throws IOException {
        return (String)peekCallback().readFromStream();
    }

    public void readFully(byte[] buf) throws IOException {
        readFully(buf, 0, buf.length);
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        byte[] b = (byte[])peekCallback().readFromStream();
        System.arraycopy(b, 0, buf, off, len);
    }

    public int read() throws IOException {
        return readUnsignedByte();
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        byte[] b = (byte[])peekCallback().readFromStream();
        if (b.length != len) {
            throw new StreamCorruptedException("Expected " + len + " bytes from stream, got " + b.length);
        }
        System.arraycopy(b, 0, buf, off, len);
        return len;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public GetField readFields() throws IOException {
        return new CustomGetField(peekCallback().readFieldsFromStream());
    }

    private class CustomGetField extends GetField {

        private Map fields;

        public CustomGetField(Map fields) {
            this.fields = fields;
        }

        public ObjectStreamClass getObjectStreamClass() {
            throw new UnsupportedOperationException();
        }

        private Object get(String name) {
            return fields.get(name);
        }

        public boolean defaulted(String name) {
            return !fields.containsKey(name);
        }

        public byte get(String name, byte val) {
            return defaulted(name) ? val : ((Byte)get(name)).byteValue();
        }

        public char get(String name, char val) {
            return defaulted(name) ? val : ((Character)get(name)).charValue();
        }

        public double get(String name, double val) {
            return defaulted(name) ? val : ((Double)get(name)).doubleValue();
        }

        public float get(String name, float val) {
            return defaulted(name) ? val : ((Float)get(name)).floatValue();
        }

        public int get(String name, int val) {
            return defaulted(name) ? val : ((Integer)get(name)).intValue();
        }

        public long get(String name, long val) {
            return defaulted(name) ? val : ((Long)get(name)).longValue();
        }

        public short get(String name, short val) {
            return defaulted(name) ? val : ((Short)get(name)).shortValue();
        }

        public boolean get(String name, boolean val) {
            return defaulted(name) ? val : ((Boolean)get(name)).booleanValue();
        }

        public Object get(String name, Object val) {
            return defaulted(name) ? val : get(name);
        }

    }

    public void registerValidation(ObjectInputValidation validation, int priority) throws NotActiveException, InvalidObjectException {
        peekCallback().registerValidation(validation, priority);
    }

    public void close() throws IOException {
        peekCallback().close();
    }

    /****** Unsupported methods ******/

    public int available() {
        throw new UnsupportedOperationException();
    }

    public String readLine() {
        throw new UnsupportedOperationException();
    }

    public int skipBytes(int len) {
        throw new UnsupportedOperationException();
    }

    public long skip(long n) {
        throw new UnsupportedOperationException();
    }

    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public boolean markSupported() {
        return false;
    }

}

/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.serialization.coders;

import org.nustaq.serialization.*;
import org.nustaq.serialization.util.FSTOutputStream;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Default Coder used for serialization. Serializes into a binary stream
 */
public class FSTStreamEncoder implements FSTEncoder {

    private FSTConfiguration conf;
    
    private FSTClazzNameRegistry clnames;
    private FSTOutputStream buffout;
    private byte[] ascStringCache;

    public FSTStreamEncoder(FSTConfiguration conf) {
        this.conf = conf;
        clnames = (FSTClazzNameRegistry) conf.getCachedObject(FSTClazzNameRegistry.class);
        if ( clnames == null ) {
            clnames = new FSTClazzNameRegistry(conf.getClassRegistry());
        } else {
            clnames.clear();
        }
    }

    @Override
    public void setConf(FSTConfiguration conf) {
        this.conf = conf;
        clnames = (FSTClazzNameRegistry) conf.getCachedObject(FSTClazzNameRegistry.class);
        if ( clnames == null ) {
            clnames = new FSTClazzNameRegistry(conf.getClassRegistry());
        } else {
            clnames.clear();
        }
    }

    void writeFBooleanArr(boolean[] arr, int off, int len) throws IOException {
        buffout.ensureFree(len);
        for (int i = off; i < off+len; i++) {
            buffout.write((arr[i] ? 1 : 0));
        }
    }

    public void writeFFloatArr(float[] arr, int off, int len) throws IOException {
        int byteLen = arr.length * 4;
        buffout.ensureFree(byteLen);
        int max = off + len;
        for (int i = off; i < max; i++) {
            long anInt = Float.floatToIntBits(arr[i]);
            buffout.write((int)(anInt >>> 0));
            buffout.write((int)(anInt >>> 8));
            buffout.write((int)(anInt >>> 16));
            buffout.write((int)(anInt >>> 24));
        }
    }

    public void writeFDoubleArr(double[] arr, int off, int len) throws IOException {
        final int byteLen = arr.length * 8;
        buffout.ensureFree(byteLen);
        final int max = off + len;
        for (int i = off; i < max; i++) {
            long aLong = Double.doubleToLongBits(arr[i]);
            buffout.write((int) (aLong >>> 0));
            buffout.write((int) (aLong >>> 8));
            buffout.write((int) (aLong >>> 16));
            buffout.write((int) (aLong >>> 24));
            buffout.write((int) (aLong >>> 32));
            buffout.write((int) (aLong >>> 40));
            buffout.write((int) (aLong >>> 48));
            buffout.write((int) (aLong >>> 56));
        }
    }


//    Using ByteBuffers is faster but only with 1.8_u40. seems terrible with prior versions.
//    also requires some hacking in order to reuse a bytebuffer

//    static Field bbHB, bbCap;
//    static {
//        Field[] fields = ByteBuffer.class.getDeclaredFields();
//        for (int i = 0; i < fields.length; i++) {
//            Field fi = fields[i];
//            if ( fi.getName() == "hb" ) {
//                bbHB = fi;
//                bbHB.setAccessible(true);
//            }
//        }
//        fields = Buffer.class.getDeclaredFields();
//        for (int i = 0; i < fields.length; i++) {
//            Field fi = fields[i];
//            if ( fi.getName() == "capacity" ) {
//                bbCap = fi;
//                bbCap.setAccessible(true);
//            }
//        }
//    }
//
//    ThreadLocal<ByteBuffer> buf = new ThreadLocal<ByteBuffer>() {
//        @Override
//        protected ByteBuffer initialValue() {
//            return ByteBuffer.wrap(new byte[0]);
//        }
//    };
//
//    public void writeFDoubleArr(double[] arr, int off, int len) throws IOException {
//        int byteLen = arr.length * 8;
//        buffout.ensureFree(byteLen);
//        int max = off + len;
//        ByteBuffer wrap = buf.get();
//        try {
//            bbHB.set(wrap, buffout.buf);
//            bbCap.set(wrap,buffout.buf.length);
//            wrap.limit(buffout.pos+byteLen);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//            wrap = ByteBuffer.wrap(buffout.buf, buffout.pos, byteLen).order(ByteOrder.LITTLE_ENDIAN);
//        }

//        int count = buffout.pos;
//        for (int i = off; i < max; i++) {
//            long aLong = Double.doubleToLongBits(arr[i]);
//            wrap.putLong(count,aLong);
//            count += 8;
//        }
//        buffout.pos+= byteLen;
//    }

    public void writeFShortArr(short[] arr, int off, int len) throws IOException {
        buffout.ensureFree(len*3);
        for (int i = off; i < off+len; i++) {
            short c = arr[i];
            if (c < 255 && c >= 0) {
                buffout.write(c);
            } else {
                buffout.write(255);
                buffout.write(c >>> 0);
                buffout.write(c >>> 8);
            }
        }
    }

    public void writeFCharArr(char[] arr, int off, int len) throws IOException {
        buffout.ensureFree(len*3);
        for (int i = off; i < off+len; i++) {
            char c = arr[i];
            if (c < 255 && c >= 0) {
                buffout.write(c);
            } else {
                buffout.write(255);
                buffout.write(c >>> 0);
                buffout.write(c >>> 8);
            }
        }
    }

    // uncompressed version
    public void writeFIntArr(int[] arr, int off, int len) throws IOException {
        int byteLen = arr.length * 4;
        buffout.ensureFree(byteLen);
        int max = off + len;
        for (int i = off; i < max; i++) {
            long anInt = arr[i];
            buffout.write((byte) (anInt >>> 0));
            buffout.write((byte) (anInt >>> 8));
            buffout.write((byte) (anInt >>> 16));
            buffout.write((byte) (anInt >>> 24));
        }
    }

    // compressed version
    public void _writeFIntArr(int v[], int off, int len) throws IOException {
        final int free = 5 * len;
        buffout.ensureFree(free);
        for (int i = off; i < off+len; i++) {
            final int anInt = v[i];
            if ( anInt > -127 && anInt <=127 ) {
                buffout.write(anInt);
            } else
            if ( anInt >= Short.MIN_VALUE && anInt <= Short.MAX_VALUE ) {
                buffout.write(-128);
                buffout.write((byte) ((anInt >>>  0) & 0xFF));
                buffout.write((byte) ((anInt >>> 8) & 0xFF));
            } else {
                buffout.write(-127);
                buffout.write((byte) ((anInt >>>  0) & 0xFF));
                buffout.write((byte) ((anInt >>>  8) & 0xFF));
                buffout.write((byte) ((anInt >>> 16) & 0xFF));
                buffout.write((byte) ((anInt >>> 24) & 0xFF));
            }
        }
    }

    void writeFLongArr(long[] arr, int off, int len) throws IOException {
        int byteLen = arr.length * 8;
        buffout.ensureFree(byteLen);
        for (int i = off; i < off+len; i++) {
            long anInt = arr[i];
            buffout.write((byte) (anInt >>> 0) );
            buffout.write((byte) (anInt >>> 8) );
            buffout.write((byte) (anInt >>> 16));
            buffout.write((byte) (anInt >>> 24));
            buffout.write((byte) (anInt >>> 32));
            buffout.write((byte) (anInt >>> 40));
            buffout.write((byte) (anInt >>> 48));
            buffout.write((byte) (anInt >>> 56));
        }
    }

    /**
     * write prim array no len no tag
     *
     *
     * @param array
     * @throws IOException
     */
    public void writePrimitiveArray(Object array, int off, int len) throws IOException {
        Class<?> componentType = array.getClass().getComponentType();
        if ( componentType == byte.class ) {
            writeRawBytes((byte[]) array, off, len);
        } else
        if ( componentType == char.class ) {
            writeFCharArr((char[]) array, off, len);
        } else
        if ( componentType == short.class ) {
            writeFShortArr((short[]) array, off, len);
        } else
        if ( componentType == int.class ) {
            writeFIntArr((int[]) array, off, len);
        } else
        if ( componentType == double.class ) {
            writeFDoubleArr((double[]) array, off, len);
        } else
        if ( componentType == float.class ) {
            writeFFloatArr((float[]) array, off, len);
        } else
        if ( componentType == long.class ) {
            writeFLongArr((long[]) array, off, len);
        } else
        if ( componentType == boolean.class ) {
            writeFBooleanArr((boolean[]) array, off, len);
        } else {
            throw new RuntimeException("expected primitive array");
        }
    }
    
    
    /**
     * does not write length, just plain bytes
     *
     * @param array
     * @param length
     * @throws java.io.IOException
     */
    public void writeRawBytes(byte[] array, int start, int length) throws IOException {
        buffout.write(array, start, length);
    }

    @Override
    public void writeStringUTF(String str) throws IOException {
        final int strlen = str.length();

        writeFInt(strlen);
        buffout.ensureFree(strlen*3);
        for (int i=0; i<strlen; i++) {
            final char c = str.charAt(i);
            if (c < 255) {
                buffout.write(c);
            } else {
                buffout.write(255);
                buffout.write((byte) ((c >>> 0) & 0xFF));
                buffout.write((byte) ((c >>> 8) & 0xFF));
            }
        }
    }
    
    /**
     * length < 127 !!!!!
     *
     * @param name
     * @throws java.io.IOException
     */
    void writeStringAsc(String name) throws IOException {
        int len = name.length();
        if ( len >= 127 ) {
            throw new RuntimeException("Ascii String too long");
        }
        writeFByte((byte) len);
        buffout.ensureFree(len);
        if (ascStringCache == null || ascStringCache.length < len)
            ascStringCache = new byte[len];
        name.getBytes(0, len, ascStringCache, 0);
        writeRawBytes(ascStringCache, 0, len);
    }

    @Override
    public void writeFShort(short c) throws IOException {
        if (c < 255 && c >= 0) {
            writeFByte(c);
        } else {
            writeFByte(255);
            writePlainShort(c);
        }
    }

    public boolean writeAttributeName(FSTClazzInfo.FSTFieldInfo subInfo, Object value) {
        return false;
    }
    
    public boolean writeTag(byte tag, Object info, long somValue, Object toWrite, FSTObjectOutput oout) throws IOException {
        writeFByte(tag);
        return false;
    }

    @Override
    public void writeFChar(char c) throws IOException {
        // -128 = short byte, -127 == 4 byte
        if (c < 255 && c >= 0) {
            buffout.write(c);
        } else {
            buffout.ensureFree(3);
            buffout.write(255);
            buffout.write((byte) (c >>> 0));
            buffout.write((byte) (c >>> 8));
        }
    }

    @Override
    public final void writeFByte(int v) throws IOException {
        buffout.write(v);
    }

    @Override
    public void writeFInt(int anInt) throws IOException {
        // -128 = short byte, -127 == 4 byte
        if (anInt > -127 && anInt <= 127) {
            buffout.write(anInt);
        } else if (anInt >= Short.MIN_VALUE && anInt <= Short.MAX_VALUE) {
            ensureFree(3);
            buffout.write(-128);
            buffout.write((byte) (anInt >>> 0));
            buffout.write((byte) (anInt >>> 8));
        } else {
            buffout.ensureFree(5);
            buffout.write((byte) -127);
            buffout.write((byte) ((anInt >>> 0) & 0xFF));
            buffout.write((byte) ((anInt >>>  8) & 0xFF));
            buffout.write((byte) ((anInt >>> 16) & 0xFF));
            buffout.write((byte) ((anInt >>> 24) & 0xFF));
        }
    }

    @Override
    public void writeFLong(long anInt) throws IOException {
// -128 = short byte, -127 == 4 byte
        if (anInt > -126 && anInt <= 127) {
            writeFByte((int) anInt);
        } else if (anInt >= Short.MIN_VALUE && anInt <= Short.MAX_VALUE) {
            ensureFree(3);
            buffout.write((byte) -128);
            buffout.write((byte) (anInt >>> 0));
            buffout.write((byte) (anInt >>> 8));
        } else if (anInt >= Integer.MIN_VALUE && anInt <= Integer.MAX_VALUE) {
            buffout.ensureFree(5);
            buffout.write((byte) -127);
            buffout.write((byte) ((anInt >>> 0) & 0xFF));
            buffout.write((byte) ((anInt >>>  8) & 0xFF));
            buffout.write((byte) ((anInt >>> 16) & 0xFF));
            buffout.write((byte) ((anInt >>> 24) & 0xFF));
        } else {
            buffout.ensureFree(9);
            buffout.write((byte) -126);
            buffout.write((byte) (anInt >>> 0));
            buffout.write((byte) (anInt >>> 8));
            buffout.write((byte) (anInt >>> 16));
            buffout.write((byte) (anInt >>> 24));
            buffout.write((byte) (anInt >>> 32));
            buffout.write((byte) (anInt >>> 40));
            buffout.write((byte) (anInt >>> 48));
            buffout.write((byte) (anInt >>> 56));
        }
    }

    /**
     * Writes a 4 byte float.
     */
    @Override
    public void writeFFloat(float value) throws IOException {
        writePlainInt(Float.floatToIntBits(value));
    }

    @Override
    public void writeFDouble(double value) throws IOException {
        writePlainLong(Double.doubleToLongBits(value));
    }

    @Override
    public int getWritten() {
        return buffout.pos-buffout.getOff();
    }

    /**
     * close and flush to underlying stream if present. The stream is also closed
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        buffout.close();
        conf.returnObject(clnames);
    }

    @Override
    public void reset(byte[] out) {
        if (out==null) {
            buffout.reset();
        } else {
            buffout.reset(out);
        }
        clnames.clear();
    }

    @Override
    public void skip(int i) {
        buffout.pos+=i;
    }

    /**
     * used to write uncompressed int (guaranteed length = 4) at a (eventually recent) position
     * @param position
     * @param v
     */
    @Override
    public void writeInt32At(int position, int v) {
        buffout.buf[position] = (byte)  (v >>> 0);
        buffout.buf[position+1] = (byte) (v >>> 8);
        buffout.buf[position+2] = (byte) (v >>> 16);
        buffout.buf[position+3] = (byte) (v >>> 24);
    }

    /**
     * if output stream is null, just encode into a byte array 
     * @param outstream
     */
    @Override
    public void setOutstream(OutputStream outstream) {
        if ( buffout == null ) 
        {
            // try reuse
            buffout = (FSTOutputStream) conf.getCachedObject(FSTOutputStream.class);
            if ( buffout == null ) // if fail, alloc
                buffout = new FSTOutputStream(1000, outstream);
            else
                buffout.reset(); // reset resued fstoutput
        }
        if ( outstream == null )
            buffout.setOutstream(buffout);
        else
            buffout.setOutstream(outstream);
    }
    
    /**
     * writes current buffer to underlying output and resets buffer. 
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        buffout.flush();
    }

    @Override
    public void ensureFree(int bytes) throws IOException {
        buffout.ensureFree(bytes);
    }

    @Override
    public byte[] getBuffer() {
        return buffout.buf;
    }

    public void registerClass(Class possible) {
        clnames.registerClass(possible,conf);
    }

    @Override
    public final void writeClass(Class cl) {
        try {
            clnames.encodeClass(this,cl);
        } catch ( IOException e) {
            FSTUtil.<RuntimeException>rethrow(e);
        }
    }

    @Override
    public final void writeClass(FSTClazzInfo clInf) {
        try {
            clnames.encodeClass(this, clInf);
        } catch ( IOException e) {
            FSTUtil.<RuntimeException>rethrow(e);
        }
    }
    

    private void writePlainLong(long v) throws IOException {
        buffout.ensureFree(8);
        byte[] buf = buffout.buf;
        int count = buffout.pos;
        buffout.write((byte) (v >>> 0));
        buffout.write((byte) (v >>> 8));
        buffout.write((byte) (v >>> 16));
        buffout.write((byte) (v >>> 24));
        buffout.write((byte) (v >>> 32));
        buffout.write((byte) (v >>> 40));
        buffout.write((byte) (v >>> 48));
        buffout.write((byte) (v >>> 56));
        buffout.pos += 8;
    }

    private void writePlainShort(int v) throws IOException {
        buffout.ensureFree(2);
        byte[] buf = buffout.buf;
        int count = buffout.pos;
        buffout.write((byte) (v >>> 0));
        buffout.write((byte) (v >>> 8));
        buffout.pos += 2;
    }

    private void writePlainChar(int v) throws IOException {
        buffout.ensureFree(2);
        byte[] buf = buffout.buf;
        int count = buffout.pos;
        buffout.write((byte) (v >>> 0));
        buffout.write((byte) (v >>> 8));
        buffout.pos += 2;
    }

    private void writePlainInt(int v) throws IOException {
        buffout.ensureFree(4);
        byte[] buf = buffout.buf;
        int count = buffout.pos;
        buffout.write((byte) ((v >>> 0) & 0xFF));
        buffout.write((byte) ((v >>>  8) & 0xFF));
        buffout.write((byte) ((v >>> 16) & 0xFF));
        buffout.write((byte) ((v >>> 24) & 0xFF));
        buffout.pos = count;
    }

    public void externalEnd(FSTClazzInfo clz) {
    }

    @Override
    public boolean isWritingAttributes() {
        return false;
    }

    public boolean isPrimitiveArray(Object array, Class<?> componentType) {
        return componentType.isPrimitive();
    }

    public boolean isTagMultiDimSubArrays() {
        return false;
    }

    @Override
    public void writeVersionTag(int version) throws IOException {
        writeFByte(version);
    }

    @Override
    public boolean isByteArrayBased() {
        return true;
    }

    @Override
    public void writeArrayEnd() {

    }

    @Override
    public void writeFieldsEnd(FSTClazzInfo serializationInfo) {
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
    }

}


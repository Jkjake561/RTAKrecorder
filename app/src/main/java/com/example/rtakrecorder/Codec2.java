package com.example.rtakrecorder;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;



public class Codec2 implements AutoCloseable {
    static {
        System.loadLibrary("Codec2Binding");
    }

    public static final int REQUIRED_SAMPLE_RATE = 8000;

    public static enum Mode {
        _3200,
        _2400,
        _1600,
        _1400,
        _1300,
        _1200,
        _700C
    }
    public static byte[] makeHeader(int mode) {
        byte[] header = new byte[7];
        header[0] = (byte) 0xc0; // Codec2 magic number
        header[1] = (byte) 0xde; // Codec2 magic number
        header[2] = (byte) 0xc2; // Codec2 identifier
        header[3] = 1; // version_major
        header[4] = 0; // version_minor
        header[5] = (byte) mode; // codec mode
        header[6] = 0; // flags
        return header;
    }
    public int getEncodedFrameSize() {
        switch (mode) {
            case _3200:
                return 8; // Adjust based on actual frame sizes for each mode
            case _2400:
                return 8;
            case _1600:
                return 8;
            case _1400:
                return 8;
            case _1300:
                return 6;
            case _1200:
                return 6;
            case _700C:
                return 6;
            default:
                throw new IllegalArgumentException("Unsupported mode: " + mode);
        }
    }
    public int getPCMFrameSize() {
        // All modes use 160 PCM samples per frame at 8000 Hz sample rate
        return 160;
    }
    private boolean closed = false;

    private final long codec2StatePtr;
    public final Mode mode;

    private Codec2(long codec2StatePtr, Mode mode) throws RuntimeException {
        this.codec2StatePtr = codec2StatePtr;
        this.mode = mode;
    }

    private static native long nativeCreateCodec2State(int mode, Class<RuntimeException> runtimeExceptionClass) throws RuntimeException;

    @NonNull
    public static Codec2 createInstance(@NotNull Mode mode) throws RuntimeException {
        long codec2StatePtr = nativeCreateCodec2State(mode.ordinal(), RuntimeException.class);
        return new Codec2(codec2StatePtr, mode);
    }

    // Both "directByteBuffer" and "byteArray" cannot be non-null, only one or the other can be passed.
    private static native ByteBuffer nativeEncodeCodec2(long codec2StatePtr, ByteBuffer directByteBuffer, byte[] byteArray, Class<RuntimeException> runtimeExceptionClass) throws RuntimeException;

    public ByteBuffer encode(@NotNull ByteBuffer pcmBuffer) throws RuntimeException {
        if (pcmBuffer.isDirect())
            return nativeEncodeCodec2(codec2StatePtr, pcmBuffer, null, RuntimeException.class);
        else {
            if (pcmBuffer.hasArray())
                return nativeEncodeCodec2(codec2StatePtr, null, pcmBuffer.array(), RuntimeException.class);
            else
                throw new RuntimeException("Unable to retrieve backing array of non-direct PCM buffer");
        }
    }

    // Both "directByteBuffer" and "byteArray" cannot be non-null, only one or the other can be passed.
    private static native ByteBuffer nativeDecodeCodec2(long codec2StatePtr, ByteBuffer directByteBuffer, byte[] byteArray, Class<RuntimeException> runtimeExceptionClass) throws  RuntimeException;

    public ByteBuffer decode(@NotNull byte[] codec2ByteArray) throws RuntimeException {
        return nativeDecodeCodec2(codec2StatePtr, null, codec2ByteArray, RuntimeException.class);
    }

    public ByteBuffer decode(@NotNull ByteBuffer codec2Buffer) throws RuntimeException {
        if (codec2Buffer.isDirect())
            return nativeDecodeCodec2(codec2StatePtr, codec2Buffer, null, RuntimeException.class);
        else {
            if (codec2Buffer.hasArray())
                return nativeDecodeCodec2(codec2StatePtr, null, codec2Buffer.array(), RuntimeException.class);
            else
                throw new RuntimeException("Unable to retrieve backing array of non-direct Codec2 buffer");
        }
    }


    public ByteBuffer encode(@NotNull byte[] pcmByteArray) throws RuntimeException {
        return nativeEncodeCodec2(codec2StatePtr, null, pcmByteArray, RuntimeException.class);
    }

    private static native void nativeDestroyCodec2State(long codec2StatePtr);

    @Override
    public void close() throws Exception {
        if (!closed) {
            nativeDestroyCodec2State(codec2StatePtr);
            closed = true;
        }
        else
            throw new Exception("Codec2 instance has already been closed");
    }

    @Override
    protected void finalize() {
        if (!closed)
            nativeDestroyCodec2State(codec2StatePtr);
    }
}
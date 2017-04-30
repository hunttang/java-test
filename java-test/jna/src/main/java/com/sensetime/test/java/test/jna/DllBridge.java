package com.sensetime.test.java.test.jna;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hunt on 8/13/15.
 */
public interface DllBridge extends Library {
    class TestStructA extends Structure {
        public static class ByValue extends TestStructA implements Structure.ByValue {
        }

        public int a;

        public TestStructA() {
            super();
        }

        public TestStructA(Pointer p) {
            super(p);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"a"});
        }
    }

    class TestStructB extends Structure {
        public TestStructA a;
        public double b;

        public TestStructB() {
            super();
        }

        public TestStructB(Pointer p) {
            super(p);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"a", "b"});
        }
    }

    DllBridge INSTANCE = (DllBridge) Native.loadLibrary("DllTest", DllBridge.class);

    int fnDllTest(TestStructB struct);

    int fnPointerTest(PointerByReference pArray, IntByReference pSize);

    int fnPointerToStructTest(PointerByReference pArray, IntByReference pSize);

    DllBridge C_INSTANCE = (DllBridge) Native.loadLibrary("msvcrt", DllBridge.class);

    void printf(String format, Object... args);
}

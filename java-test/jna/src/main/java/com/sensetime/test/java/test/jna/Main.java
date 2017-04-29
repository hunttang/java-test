package com.sensetime.test.java.test.jna;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;

/**
 * Created by Hunt on 8/13/15.
 */
public class Main {
    public static void main(String[] args) {
        // Test fnDllTest()
        DllBridge.TestStructB struct = new DllBridge.TestStructB();
        struct.a.a = 59;
        DllBridge.C_INSTANCE.printf("%d\n", DllBridge.INSTANCE.fnDllTest(struct));

        /*
        // Test fnPointerTest()
        PointerByReference pArray = new PointerByReference();
        IntByReference pSize = new IntByReference();

        int rst = DllBridge.INSTANCE.fnPointerTest(pArray, pSize);
        System.out.printf("Return = %d\nSize = %d\n", rst, pSize.getValue());

        int[] array = pArray.getValue().getIntArray(0, pSize.getValue());

        for (int element : array) {
            System.out.println(element);
        }

        // Test fnPointerToStructTest()
        PointerByReference pArray = new PointerByReference();
        IntByReference pSize = new IntByReference();

        int rst = DllBridge.INSTANCE.fnPointerToStructTest(pArray, pSize);
        System.out.printf("Return = %d\nSize = %d\n", rst, pSize.getValue());

        DllBridge.TestStruct arrayBlock = new DllBridge.TestStruct(pArray.getValue());
        arrayBlock.read();
        DllBridge.TestStruct[] array = (DllBridge.TestStruct[]) arrayBlock.toArray(pSize.getValue());

        for (DllBridge.TestStruct element : array) {
            System.out.println(element.a);
            System.out.println(element.b);
        }
        */
    }
}

package com.sam.blejavaadvertise;

public class NativeDestroyNotCalledException extends Exception {

    @Override
    public String getMessage() {
        return "Native destroy need to performed before using another instance";
    }
}

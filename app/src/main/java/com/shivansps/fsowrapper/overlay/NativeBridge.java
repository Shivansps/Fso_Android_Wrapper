package com.shivansps.fsowrapper.overlay;

import android.util.Log;

public class NativeBridge {
    static { System.loadLibrary("fsowrapper"); }
    //COMMON
    public static final int CODE_ESC = 1;
    public static final int CODE_F3 = 2;
    public static final int CODE_ALT_M = 3;
    public static final int CODE_ALT_H = 4;
    public static final int CODE_ALT_J = 5;
    //DPAD
    public static final int CODE_KEY_W = 10;
    public static final int CODE_KEY_A = 11;
    public static final int CODE_KEY_S = 12;
    public static final int CODE_KEY_D = 13;

    //Weapons Area
    public static final int CODE_KEY_SPACE = 20;
    public static final int CODE_KEY_CTRL = 21;
    public static final int CODE_KEY_CYCLE_P = 22;
    public static final int CODE_KEY_CYCLE_S = 23;
    public static final int CODE_KEY_TAB = 24;
    public static final int CODE_KEY_PLUS = 25;
    public static final int CODE_KEY_MINUS = 26;
    public static final int CODE_KEY_Q = 27;
    public static final int CODE_KEY_X = 28;

    //Targeting
    public static final int CODE_KEY_Y = 30;
    public static final int CODE_KEY_H = 31;
    public static final int CODE_KEY_B = 32;
    public static final int CODE_KEY_E = 33;
    public static final int CODE_KEY_F = 34;
    public static final int CODE_KEY_T = 35;

    public static native void onButton(int code, boolean down);
}
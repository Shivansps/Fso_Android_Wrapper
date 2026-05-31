package com.shivansps.fsowrapper;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import com.shivansps.fsowrapper.overlay.RadialDpadView;
import com.shivansps.fsowrapper.tts.TTSManager;
import java.util.ArrayList;
import android.view.*;
import android.widget.*;
import com.shivansps.fsowrapper.overlay.NativeBridge;

public class GameActivity extends org.libsdl.app.SDLActivity {

    private static String _workingFolder = "";

    /* FSO API */

    public static String getWorkingFolder() { return _workingFolder; }

    /* ******* */

    @Override
    protected String[] getArguments() {
        android.content.Intent i = getIntent();
        java.util.ArrayList<String> args = (i != null)
                ? i.getStringArrayListExtra("fsoArgs")
                : null;

        if (args == null || args.isEmpty()) {
            return new String[0];
        }
        return args.toArray(new String[0]);
    }

    @Override
    protected String[] getLibraries() {
        ArrayList<String> libs = new ArrayList<>();
        libs.add("SDL2");

        Intent i = getIntent();
        String engineBase = (i != null) ? i.getStringExtra("engineLibName") : null;
        if (engineBase == null || engineBase.trim().isEmpty()) {
            engineBase = "fs2_open_24_3_0_arm64"; // fallback
        }
        libs.add(engineBase);
        return libs.toArray(new String[0]);
    }

    @Override
    protected String getMainFunction() {
        return "android_main";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            getWindow().setSustainedPerformanceMode(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            hideSystemUI();
        } catch (Throwable ignored) {
        }
        TTSManager.init(this);
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if(i != null)
        {
            boolean overlayOn = i.getBooleanExtra("forceTouchOverlay", true);
            _workingFolder = i.getStringExtra("workingFolder");
            if(overlayOn){
                getWindow().getDecorView().post(this::setupOverlayFromXml);
            }
        }

    }

    @Override protected void onPause() {
        TTSManager.stop();
        super.onPause();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override protected void onDestroy()
    {
        _workingFolder = "";
        TTSManager.shutdown();
        super.onDestroy();
        try {
            if (isChangingConfigurations()) return;
            // kill process if it is still running
            // Note: may break pilot files
            String proc = android.app.Application.getProcessName();
            if (proc != null && proc.endsWith(":game")) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        } catch (Throwable ignored) {}
    }

    private void toggleSdlKeyboard(View overlayRoot) {
        boolean imeVisible = false;

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsets insets = overlayRoot.getRootWindowInsets();
            if (insets != null) {
                imeVisible = insets.isVisible(WindowInsets.Type.ime());
            }
        }

        if (!imeVisible) {
            NativeBridge.setTextInputEnabled(true);
        } else {
            NativeBridge.setTextInputEnabled(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener makeTouchHandler(int code) {
        return (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    NativeBridge.onButton(code, true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    NativeBridge.onButton(code, false);
                    return true;
            }
            return false;
        };
    }

    @SuppressLint({"ClickableViewAccessibility", "DiscouragedApi"})
    private void setupOverlayFromXml()
    {
        int layoutId = getResources().getIdentifier("overlay_controls", "layout", getPackageName());
        View overlay = getLayoutInflater().inflate(layoutId, null);

        Button btnToggle = overlay.findViewById(getResources().getIdentifier("btnToggle", "id", getPackageName()));
        RadialDpadView dpad = overlay.findViewById(getResources().getIdentifier("dpad", "id", getPackageName()));


        // Button listeners
        Button btnKyb = overlay.findViewById(getResources().getIdentifier("btnKyb", "id", getPackageName()));
        btnKyb.setOnClickListener(v -> toggleSdlKeyboard(overlay));
        // ESC
        Button btnEsc = overlay.findViewById(getResources().getIdentifier("btnEsc", "id", getPackageName()));
        btnEsc.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ESC));

        // F3
        Button btnF3 = overlay.findViewById(getResources().getIdentifier("btnF3", "id", getPackageName()));
        btnF3.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_F3));

        // ALT+J
        Button btnALTJ = overlay.findViewById(getResources().getIdentifier("btnAltJ", "id", getPackageName()));
        btnALTJ.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_J));

        // ALT+M
        Button btnALTM = overlay.findViewById(getResources().getIdentifier("btnAltM", "id", getPackageName()));
        btnALTM.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_M));

        // ALT+H
        Button btnALTH = overlay.findViewById(getResources().getIdentifier("btnAltH", "id", getPackageName()));
        btnALTH.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_H));

        // ALT+A
        Button btnAltA = overlay.findViewById(getResources().getIdentifier("btnAltA", "id", getPackageName()));
        btnAltA.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_A));

        // Space
        Button btnSpace = overlay.findViewById(getResources().getIdentifier("btnFireS", "id", getPackageName()));
        btnSpace.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_SPACE));

        // LCtrl
        Button btnLCtrl = overlay.findViewById(getResources().getIdentifier("btnFireP", "id", getPackageName()));
        btnLCtrl.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CTRL));

        // CycleP
        Button btnCycleP = overlay.findViewById(getResources().getIdentifier("btnCycleP", "id", getPackageName()));
        btnCycleP.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CYCLE_P));

        // CycleS
        Button btnCycleS = overlay.findViewById(getResources().getIdentifier("btnCycleS", "id", getPackageName()));
        btnCycleS.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CYCLE_S));

        // Tab
        Button btnTab = overlay.findViewById(getResources().getIdentifier("btnTab", "id", getPackageName()));
        btnTab.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_TAB));

        // +
        Button btnPlus = overlay.findViewById(getResources().getIdentifier("btnPlus", "id", getPackageName()));
        btnPlus.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_PLUS));

        // -
        Button btnMinus = overlay.findViewById(getResources().getIdentifier("btnMinus", "id", getPackageName()));
        btnMinus.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_MINUS));

        // Q
        Button btnQ = overlay.findViewById(getResources().getIdentifier("btnQ", "id", getPackageName()));
        btnQ.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_Q));

        // -
        Button btnX = overlay.findViewById(getResources().getIdentifier("btnX", "id", getPackageName()));
        btnX.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_X));

        // Y
        Button btnY = overlay.findViewById(getResources().getIdentifier("btnY", "id", getPackageName()));
        btnY.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_Y));

        // H
        Button btnH = overlay.findViewById(getResources().getIdentifier("btnH", "id", getPackageName()));
        btnH.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_H));

        // B
        Button btnB = overlay.findViewById(getResources().getIdentifier("btnB", "id", getPackageName()));
        btnB.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_B));

        // E
        Button btnE = overlay.findViewById(getResources().getIdentifier("btnE", "id", getPackageName()));
        btnE.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_E));

        // F
        Button btnF = overlay.findViewById(getResources().getIdentifier("btnF", "id", getPackageName()));
        btnF.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_F));

        // T
        Button btnT = overlay.findViewById(getResources().getIdentifier("btnT", "id", getPackageName()));
        btnT.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_T));

        // M
        Button btnM = overlay.findViewById(getResources().getIdentifier("btnM", "id", getPackageName()));
        btnM.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_M));

        // S
        Button btnS = overlay.findViewById(getResources().getIdentifier("btnS", "id", getPackageName()));
        btnS.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_S));

        // A
        Button btnA = overlay.findViewById(getResources().getIdentifier("btnA", "id", getPackageName()));
        btnA.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_A));

        // Z
        Button btnZ = overlay.findViewById(getResources().getIdentifier("btnZ", "id", getPackageName()));
        btnZ.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_Z));

        // Return
        Button btnRet = overlay.findViewById(getResources().getIdentifier("btnRet", "id", getPackageName()));
        btnRet.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_BACKSPACE));

        // Backslash
        Button btnBackSlash = overlay.findViewById(getResources().getIdentifier("btnBackSlash", "id", getPackageName()));
        btnBackSlash.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_BACKSLASH));

        // C+3+1
        Button btnC31 = overlay.findViewById(getResources().getIdentifier("btnC31", "id", getPackageName()));
        btnC31.setOnClickListener(v -> NativeBridge.runMacro(NativeBridge.C_3_1));

        // C+3+5
        Button btnC35 = overlay.findViewById(getResources().getIdentifier("btnC35", "id", getPackageName()));
        btnC35.setOnClickListener(v -> NativeBridge.runMacro(NativeBridge.C_3_5));

        // C+3+9
        Button btnC39 = overlay.findViewById(getResources().getIdentifier("btnC39", "id", getPackageName()));
        btnC39.setOnClickListener(v -> NativeBridge.runMacro(NativeBridge.C_3_9));

        // C+5
        Button btnC5 = overlay.findViewById(getResources().getIdentifier("btnC5", "id", getPackageName()));
        btnC5.setOnClickListener(v -> NativeBridge.runMacro(NativeBridge.C_5));

        // Buttons that visibility are controlled by the toggle
        View[] topBar = new View[] {
                btnEsc, btnF3, btnALTJ, btnALTM, btnALTH, btnAltA, btnC31, btnC35, btnC39, btnC5, btnKyb };

        View[] joystick = new View[] {
                dpad, btnSpace, btnLCtrl, btnCycleP, btnCycleS, btnTab, btnS, btnA, btnZ, btnRet,
                btnPlus, btnMinus, btnX, btnQ, btnY, btnH, btnB, btnE, btnF, btnT, btnM, btnBackSlash };

        btnToggle.setOnClickListener(v -> {
            boolean topBarVisible = topBar[0].getVisibility() == View.VISIBLE;
            boolean joystickVisible = joystick[0].getVisibility() == View.VISIBLE;

            int newTop = (topBarVisible && joystickVisible) ? View.GONE : View.VISIBLE;
            int newJoy = (topBarVisible && !joystickVisible) ? View.VISIBLE : View.GONE;

            for (int i = 0; i < topBar.length; i++) topBar[i].setVisibility(newTop);
            for (int i = 0; i < joystick.length; i++) joystick[i].setVisibility(newJoy);
        });

        for (View w : topBar) w.setVisibility(
                View.GONE
        );
        for (View w : joystick) w.setVisibility(
                View.GONE
        );

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        addContentView(overlay, lp);
        overlay.bringToFront();
        overlay.setElevation(10000f);


        if (Build.VERSION.SDK_INT >= 30) {
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }
}
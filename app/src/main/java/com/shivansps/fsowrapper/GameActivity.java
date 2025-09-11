package com.shivansps.fsowrapper;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.shivansps.fsowrapper.overlay.RadialDpadView;
import com.shivansps.fsowrapper.tts.TTSManager;
import java.util.ArrayList;
import android.view.*;
import android.widget.*;
import com.shivansps.fsowrapper.overlay.NativeBridge;

public class GameActivity extends org.libsdl.app.SDLActivity {
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
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            getWindow().setSustainedPerformanceMode(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Throwable ignored) {
        }
        TTSManager.init(this);
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        boolean touchOverlay = i == null || i.getBooleanExtra("touchOverlay", true);
        if (touchOverlay) {
            getWindow().getDecorView().post(this::setupOverlayFromXml);
        }
    }

    @Override protected void onPause() {
        TTSManager.stop();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
    }

    @Override protected void onDestroy()
    {
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupOverlayFromXml()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        View overlay = inflater.inflate(R.layout.overlay_controls, null);
        Button btnToggle = overlay.findViewById(R.id.btnToggle);
        RadialDpadView dpad = overlay.findViewById(R.id.dpad);
        dpad.setMode(RadialDpadView.Mode.DIR8);

        // Button listeners
        // ESC
        Button btnEsc = overlay.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ESC));

        // F3
        Button btnF3 = overlay.findViewById(R.id.btnF3);
        btnF3.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_F3));

        // ALT+J
        Button btnALTJ = overlay.findViewById(R.id.btnAltJ);
        btnALTJ.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_J));

        // ALT+M
        Button btnALTM = overlay.findViewById(R.id.btnAltM);
        btnALTM.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_M));

        // ALT+H
        Button btnALTH = overlay.findViewById(R.id.btnAltH);
        btnALTH.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_ALT_H));

        // Space
        Button btnSpace = overlay.findViewById(R.id.btnFireS);
        btnSpace.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_SPACE));

        // LCtrl
        Button btnLCtrl = overlay.findViewById(R.id.btnFireP);
        btnLCtrl.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CTRL));

        // CycleP
        Button btnCycleP = overlay.findViewById(R.id.btnCycleP);
        btnCycleP.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CYCLE_P));

        // CycleS
        Button btnCycleS = overlay.findViewById(R.id.btnCycleS);
        btnCycleS.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_CYCLE_S));

        // Tab
        Button btnTab = overlay.findViewById(R.id.btnTab);
        btnTab.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_TAB));

        // +
        Button btnPlus = overlay.findViewById(R.id.btnPlus);
        btnPlus.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_PLUS));

        // -
        Button btnMinus = overlay.findViewById(R.id.btnMinus);
        btnMinus.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_MINUS));

        // Q
        Button btnQ = overlay.findViewById(R.id.btnQ);
        btnQ.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_Q));

        // -
        Button btnX = overlay.findViewById(R.id.btnX);
        btnX.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_X));

        // Y
        Button btnY = overlay.findViewById(R.id.btnY);
        btnY.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_Y));

        // H
        Button btnH = overlay.findViewById(R.id.btnH);
        btnH.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_H));

        // B
        Button btnB = overlay.findViewById(R.id.btnB);
        btnB.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_B));

        // E
        Button btnE = overlay.findViewById(R.id.btnE);
        btnE.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_E));

        // F
        Button btnF = overlay.findViewById(R.id.btnF);
        btnF.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_F));

        // T
        Button btnT = overlay.findViewById(R.id.btnT);
        btnT.setOnTouchListener(makeTouchHandler(NativeBridge.CODE_KEY_T));

        // Buttons that visibility are controlled by the toggle
        View[] controlled = new View[] {
                btnEsc, btnF3, dpad, btnALTJ, btnALTM, btnALTH, btnSpace, btnLCtrl,
                btnCycleP, btnCycleS, btnTab, btnPlus, btnMinus, btnX, btnQ, btnY,
                btnH, btnB, btnE, btnF, btnT };


        btnToggle.setOnClickListener(v -> {
            boolean hide = controlled[0].getVisibility() == View.VISIBLE;
            for (View w : controlled) w.setVisibility(hide ? View.GONE : View.VISIBLE);
        });

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
}
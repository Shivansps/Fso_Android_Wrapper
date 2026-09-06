package com.shivansps.fsowrapper;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import com.shivansps.fsowrapper.overlay.RadialDpadView;
import com.shivansps.fsowrapper.overlay.RadialActionView;
import com.shivansps.fsowrapper.overlay.HudStyle;
import com.shivansps.fsowrapper.tts.TTSManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.view.*;
import android.widget.*;
import com.shivansps.fsowrapper.overlay.NativeBridge;
import java.lang.ref.WeakReference;
import android.util.SparseIntArray;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class GameActivity extends org.libsdl.app.SDLActivity {

    private static String _workingFolder = "";
    private static WeakReference<View> _overlayRef = null;
    private static WeakReference<GameActivity> _activityRef = null;
    private static Boolean _pendingVisibility = null;
    private static Boolean _forceOverlayOn = false;

    private final SparseIntArray heldKeyCounts = new SparseIntArray();
    private final ArrayList<RadialActionView> radialControls = new ArrayList<>();
    private RadialDpadView dpadControl;
    private Button[] hudButtons = new Button[0];
    private View[] topControls = new View[0];
    private View[] levelOneControls = new View[0];
    private View[] levelTwoControls = new View[0];
    private View[] levelThreeControls = new View[0];
    private int hudMode = 0;

    /* FSO API */

    public static String getWorkingFolder() { return _workingFolder; }

    public static void setOverlayOpacity(float opacity) {
        HudStyle.setBackgroundOpacity(opacity);
        GameActivity activity = _activityRef != null ? _activityRef.get() : null;
        View overlay = _overlayRef != null ? _overlayRef.get() : null;
        if (activity != null && overlay != null) {
            overlay.post(activity::refreshHudAppearance);
        }
    }

    public static void enableOverlay() {
        if (_forceOverlayOn) return;
        View overlay = _overlayRef != null ? _overlayRef.get() : null;
        if (overlay != null) {
            overlay.post(() -> overlay.setVisibility(View.VISIBLE));
        } else {
            _pendingVisibility = true;
        }
    }

    public static void disableOverlay() {
        if (_forceOverlayOn) return;
        View overlay = _overlayRef != null ? _overlayRef.get() : null;
        if (overlay != null) {
            GameActivity activity = _activityRef != null ? _activityRef.get() : null;
            overlay.post(() -> {
                if (activity != null) activity.releaseOverlayInputs();
                overlay.setVisibility(View.GONE);
            });
        } else {
            _pendingVisibility = false;
        }
    }

    // TTS wrappers ----------------------------------------------------------
    public static boolean tts_speak(String text)     { return TTSManager.speak(text); }
    public static boolean tts_stop()                 { return TTSManager.stop(); }
    public static boolean tts_pause()                { return TTSManager.pause(); }
    public static boolean tts_resume()               { return TTSManager.resume(); }
    public static boolean tts_isSpeaking()           { return TTSManager.isSpeaking(); }
    public static void    tts_shutdown()             { TTSManager.shutdown(); }
    public static void    tts_setRate(float rate)    { TTSManager.setRate(rate); }
    public static void    tts_setLanguageTag(String tag) { TTSManager.setLanguageTag(tag); }
    public static String[] tts_getAvailableLanguageTags() { return TTSManager.getAvailableLanguageTags(); }
    // -------------------------------------------------------------------------

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
        libs.add("SDL3");

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
            applyImmersive();
        } catch (Throwable ignored) {
        }
        TTSManager.init(this);
        super.onCreate(savedInstanceState);
        _activityRef = new WeakReference<>(this);
        Intent i = getIntent();
        if(i != null)
        {
            _forceOverlayOn = i.getBooleanExtra("forceTouchOverlay", false);
            if(_forceOverlayOn) HudStyle.ensureVisible();
            _workingFolder = i.getStringExtra("workingFolder");
            getWindow().getDecorView().post(this::setupOverlayFromXml);
        }

    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_ESCAPE || k == KeyEvent.KEYCODE_BACK) {
            switch (e.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if (e.getRepeatCount() == 0) {
                        setKeyPressed(NativeBridge.CODE_ESC, true);
                    }
                    return true;
                case KeyEvent.ACTION_UP:
                    setKeyPressed(NativeBridge.CODE_ESC, false);
                    return true;
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override protected void onPause() {
        releaseOverlayInputs();
        TTSManager.stop();
        super.onPause();
    }

    private void applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersive();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        applyImmersive();
    }

    @Override protected void onDestroy()
    {
        _workingFolder = "";
        _overlayRef  = null;
        _activityRef = null;
        _pendingVisibility = null;
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
    private View.OnTouchListener makeTouchHandler(int... codes) {
        final int[] actionCodes = Arrays.copyOf(codes, codes.length);
        return (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    setKeysPressed(actionCodes, true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    setKeysPressed(actionCodes, false);
                    return true;
            }
            return false;
        };
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindHoldButton(Button button, int... codes) {
        final int[] actionCodes = Arrays.copyOf(codes, codes.length);
        button.setOnTouchListener(makeTouchHandler(actionCodes));
        button.setOnClickListener(view -> {
            setKeysPressed(actionCodes, true);
            view.postDelayed(() -> setKeysPressed(actionCodes, false), 45L);
        });
    }

    private void setKeysPressed(int[] codes, boolean pressed) {
        if (pressed) {
            for (int code : codes) setKeyPressed(code, true);
        } else {
            for (int index = codes.length - 1; index >= 0; index--) {
                setKeyPressed(codes[index], false);
            }
        }
    }

    private void setKeyPressed(int code, boolean pressed) {
        int count = heldKeyCounts.get(code, 0);
        if (pressed) {
            heldKeyCounts.put(code, count + 1);
            if (count == 0) NativeBridge.onButton(code, true);
        } else if (count > 0) {
            if (count == 1) {
                heldKeyCounts.delete(code);
                NativeBridge.onButton(code, false);
            } else {
                heldKeyCounts.put(code, count - 1);
            }
        }
    }

    private void dispatchRadialTransition(List<RadialActionView.Action> releasedActions,
                                          List<RadialActionView.Action> pressedActions) {
        SparseIntArray deltas = new SparseIntArray();
        for (RadialActionView.Action releasedAction : releasedActions) {
            for (int code : releasedAction.getKeyCodes()) {
                deltas.put(code, deltas.get(code, 0) - 1);
            }
        }
        for (RadialActionView.Action pressedAction : pressedActions) {
            for (int code : pressedAction.getKeyCodes()) {
                deltas.put(code, deltas.get(code, 0) + 1);
            }
        }

        // Release removed keys before pressing new ones, but leave shared keys
        // untouched so sliding Primary <-> Both <-> Secondary has no pulse.
        for (int index = 0; index < deltas.size(); index++) {
            int delta = deltas.valueAt(index);
            for (int count = 0; count < -delta; count++) {
                setKeyPressed(deltas.keyAt(index), false);
            }
        }
        for (int index = 0; index < deltas.size(); index++) {
            int delta = deltas.valueAt(index);
            for (int count = 0; count < delta; count++) {
                setKeyPressed(deltas.keyAt(index), true);
            }
        }
    }

    private void releaseOverlayInputs() {
        for (RadialActionView control : radialControls) control.releaseAllActions();
        if (dpadControl != null) dpadControl.releaseAllControls();
        for (View control : topControls) control.setPressed(false);
        while (heldKeyCounts.size() > 0) {
            int code = heldKeyCounts.keyAt(heldKeyCounts.size() - 1);
            heldKeyCounts.removeAt(heldKeyCounts.size() - 1);
            NativeBridge.onButton(code, false);
        }
    }

    private void refreshHudAppearance() {
        for (Button button : hudButtons) HudStyle.applyTo(button);
        for (RadialActionView control : radialControls) control.invalidate();
        if (dpadControl != null) dpadControl.invalidate();
    }

    private RadialActionView.Action action(String label, int... codes) {
        return new RadialActionView.Action(label, codes);
    }

    private RadialActionView.Action action(String label, String description, int... codes) {
        return new RadialActionView.Action(label, description, codes);
    }

    private void configureWheel(RadialActionView wheel,
                                RadialActionView.Action[] center,
                                RadialActionView.Action[] outer) {
        wheel.setOnActionListener(this::dispatchRadialTransition);
        wheel.setActions(Arrays.asList(center), Arrays.asList(outer));
        radialControls.add(wheel);
    }

    private void applyHudMode() {
        setControlGroupVisible(levelOneControls, hudMode >= 1);
        setControlGroupVisible(levelTwoControls, hudMode >= 2);
        setControlGroupVisible(levelThreeControls, hudMode >= 3);
    }

    private static void setControlGroupVisible(View[] controls, boolean visible) {
        for (View control : controls) {
            control.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "DiscouragedApi"})
    private void setupOverlayFromXml()
    {
        ViewGroup contentRoot = findViewById(android.R.id.content);
        View overlay = getLayoutInflater().inflate(R.layout.overlay_controls, contentRoot, false);
        ViewCompat.setOnApplyWindowInsetsListener(overlay, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                    WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.systemGestures());
            view.setPadding(safe.left, 0, safe.right, safe.bottom);
            return windowInsets;
        });

        Button btnToggle = overlay.findViewById(R.id.btnToggle);
        Button btnKyb = overlay.findViewById(R.id.btnKyb);
        btnKyb.setOnClickListener(v -> toggleSdlKeyboard(overlay));

        Button btn0 = overlay.findViewById(R.id.btn0);
        Button btnF1 = overlay.findViewById(R.id.btnF1);
        Button btnF2 = overlay.findViewById(R.id.btnF2);
        Button btnF3 = overlay.findViewById(R.id.btnF3);
        Button btnF4 = overlay.findViewById(R.id.btnF4);
        Button btnEsc = overlay.findViewById(R.id.btnEsc);
        Button btnAltJ = overlay.findViewById(R.id.btnAltJ);
        Button btnAltM = overlay.findViewById(R.id.btnAltM);
        Button btnAltH = overlay.findViewById(R.id.btnAltH);
        Button btnAltA = overlay.findViewById(R.id.btnAltA);

        hudButtons = new Button[] {
                btn0, btnF1, btnF2, btnF3, btnF4, btnEsc, btnToggle, btnKyb,
                btnAltJ, btnAltM, btnAltH, btnAltA
        };
        refreshHudAppearance();

        bindHoldButton(btn0, NativeBridge.CODE_KEY_0);
        bindHoldButton(btnF1, NativeBridge.CODE_F1);
        bindHoldButton(btnF2, NativeBridge.CODE_F2);
        bindHoldButton(btnF3, NativeBridge.CODE_F3);
        bindHoldButton(btnF4, NativeBridge.CODE_F4);
        bindHoldButton(btnEsc, NativeBridge.CODE_ESC);
        bindHoldButton(btnAltJ,
                NativeBridge.CODE_KEY_ALT, NativeBridge.CODE_KEY_J);
        bindHoldButton(btnAltM,
                NativeBridge.CODE_KEY_ALT, NativeBridge.CODE_KEY_M);
        bindHoldButton(btnAltH,
                NativeBridge.CODE_KEY_ALT, NativeBridge.CODE_KEY_H);
        bindHoldButton(btnAltA,
                NativeBridge.CODE_KEY_ALT, NativeBridge.CODE_KEY_A);

        RadialActionView communicationWheel = overlay.findViewById(R.id.communicationWheel);
        configureWheel(communicationWheel,
                new RadialActionView.Action[] {
                        action("C", NativeBridge.CODE_KEY_C)
                },
                new RadialActionView.Action[] {
                        action("1", NativeBridge.CODE_KEY_1), action("2", NativeBridge.CODE_KEY_2),
                        action("3", NativeBridge.CODE_KEY_3), action("4", NativeBridge.CODE_KEY_4),
                        action("5", NativeBridge.CODE_KEY_5), action("6", NativeBridge.CODE_KEY_6),
                        action("7", NativeBridge.CODE_KEY_7), action("8", NativeBridge.CODE_KEY_8),
                        action("9", NativeBridge.CODE_KEY_9)
                });
        communicationWheel.setInnerRadiusRatio(0.48f);
        communicationWheel.setFirstOuterActionAngle(-70f);
        communicationWheel.setLabelScale(0.92f);

        RadialActionView targetWheel = overlay.findViewById(R.id.targetWheel);
        configureWheel(targetWheel,
                new RadialActionView.Action[] {
                        action("B", NativeBridge.CODE_KEY_B),
                        action("H", NativeBridge.CODE_KEY_H)
                },
                new RadialActionView.Action[] {
                        action("Y", NativeBridge.CODE_KEY_Y), action("E", NativeBridge.CODE_KEY_E),
                        action("S", NativeBridge.CODE_KEY_S), action("T", NativeBridge.CODE_KEY_T),
                        action("F", NativeBridge.CODE_KEY_F)
                });
        targetWheel.setInnerRadiusRatio(0.55f);

        RadialActionView weaponWheel = overlay.findViewById(R.id.weaponWheel);
        configureWheel(weaponWheel,
                new RadialActionView.Action[] {
                        action("PRIMARY", "Fire primary", NativeBridge.CODE_KEY_CTRL),
                        action("BOTH", "Fire primary and secondary",
                                NativeBridge.CODE_KEY_CTRL, NativeBridge.CODE_KEY_SPACE),
                        action("SECONDARY", "Fire secondary", NativeBridge.CODE_KEY_SPACE)
                },
                new RadialActionView.Action[] {
                        action("+", NativeBridge.CODE_KEY_PLUS), action("-", NativeBridge.CODE_KEY_MINUS),
                        action("Z", NativeBridge.CODE_KEY_Z), action("X", NativeBridge.CODE_KEY_X),
                        action("Q", NativeBridge.CODE_KEY_Q),
                        action("SW\nS", "Switch secondary weapon", NativeBridge.CODE_KEY_CYCLE_S),
                        action("SW\nP", "Switch primary weapon", NativeBridge.CODE_KEY_CYCLE_P),
                        action("\\", "Backslash", NativeBridge.CODE_KEY_BACKSLASH),
                        action("\u2190", "Backspace", NativeBridge.CODE_KEY_BACKSPACE),
                        action("M", NativeBridge.CODE_KEY_M), action("A", NativeBridge.CODE_KEY_A)
                });
        weaponWheel.setInnerRadiusRatio(0.59f);
        weaponWheel.setCenterWeights(0.42f, 0.16f, 0.42f);
        weaponWheel.setFirstOuterActionAngle(-106.36f);

        dpadControl = overlay.findViewById(R.id.dpad);
        dpadControl.setOnRingActionListener(
                pressed -> setKeyPressed(NativeBridge.CODE_KEY_TAB, pressed));

        topControls = new View[] {
                btn0, btnF1, btnF2, btnF3, btnF4, btnEsc, btnKyb,
                btnAltJ, btnAltM, btnAltH, btnAltA
        };
        levelOneControls = new View[] {
                btnF1, btnF2, btnF3, btnF4, btnEsc, btnKyb,
                btnAltJ, btnAltM, btnAltH, btnAltA
        };
        levelTwoControls = new View[] {
                btn0, communicationWheel
        };
        levelThreeControls = new View[] {
                targetWheel, weaponWheel, dpadControl
        };

        btnToggle.setOnClickListener(v -> {
            releaseOverlayInputs();
            hudMode = (hudMode + 1) % 4;
            applyHudMode();
        });
        applyHudMode();

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        addContentView(overlay, lp);
        ViewCompat.requestApplyInsets(overlay);
        overlay.bringToFront();
        overlay.setElevation(10000f);


        if (Build.VERSION.SDK_INT >= 30) {
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        _overlayRef = new WeakReference<>(overlay);

        if(!_forceOverlayOn)
            overlay.setVisibility(View.GONE);

        if (_pendingVisibility != null) {
            overlay.setVisibility(_pendingVisibility ? View.VISIBLE : View.GONE);
            _pendingVisibility = null;
        }
    }
}

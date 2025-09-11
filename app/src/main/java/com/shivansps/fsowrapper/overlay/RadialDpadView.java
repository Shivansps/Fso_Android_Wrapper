package com.shivansps.fsowrapper.overlay;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashSet;
import java.util.Set;

public class RadialDpadView extends View {
    public enum Mode { DIR4, DIR8 }

    private final Paint pBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pKnob = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean visible = true;
    private float cx, cy;
    private float rBase;
    private float rKnob;
    private int activePid = -1;

    // Config
    private Mode mode = Mode.DIR8;
    private float deadzone = 0.12f;
    private float cardinalBiasDeg = 12f;
    private boolean floating = false;

    private final Set<Integer> pressed = new HashSet<>();

    public RadialDpadView(Context c) { this(c, null); }
    public RadialDpadView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setFocusable(false);
        setClickable(false);
        setHapticFeedbackEnabled(true);
        pBase.setStyle(Paint.Style.FILL); pBase.setAlpha(60);
        pKnob.setStyle(Paint.Style.FILL); pKnob.setAlpha(110);
    }

    public void setMode(Mode m) { this.mode = m; }
    public void setFloating(boolean f) { this.floating = f; if(f) visible = false; }
    public void setDeadzone(float dz) { this.deadzone = Math.max(0f, Math.min(0.5f, dz)); }
    public void setCardinalBias(float degrees) { this.cardinalBiasDeg = Math.max(0f, Math.min(30f, degrees)); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rBase = 0.5f * Math.min(w, h) * 0.95f;
        rKnob = rBase * 0.40f;
        cx = w * 0.5f;
        cy = h * 0.5f;
        knobX = cx;
        knobY = cy;
    }

    @Override protected void onDraw(Canvas c) {
        c.drawColor(0, PorterDuff.Mode.CLEAR);
        if (!visible) return;
        c.drawCircle(cx, cy, rBase, pBase);
        c.drawCircle(knobX, knobY, rKnob, pKnob);
    }

    private float knobX, knobY;

    @Override public boolean onTouchEvent(MotionEvent ev) {
        final int act = ev.getActionMasked();
        switch (act) {
            case MotionEvent.ACTION_DOWN: {
                int idx = ev.getActionIndex();
                float x = ev.getX(idx), y = ev.getY(idx);
                float dx = x - cx, dy = y - cy;
                if ((dx*dx + dy*dy) > (rBase*rBase)) return false;

                int pid = ev.getPointerId(idx);
                if (activePid == -1) {
                    activePid = pid;
                    performHapticFeedback(1);
                    if (floating) { cx = x; cy = y; }
                    updateFromTouch(x, y);
                    if(floating) visible = true;
                    invalidate();
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (activePid != -1) {
                    int idx = ev.findPointerIndex(activePid);
                    if (idx >= 0) updateFromTouch(ev.getX(idx), ev.getY(idx));
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int idx = ev.getActionIndex();
                int pid = ev.getPointerId(idx);
                if (pid == activePid) {
                    releaseAllKeys();
                    activePid = -1;
                    if(floating) visible = false;
                    invalidate();
                }
                break;
            }
        }
        return true;
    }

    private void updateFromTouch(float x, float y) {
        float dx = x - cx, dy = y - cy;
        float len = (float) Math.hypot(dx, dy);
        if (len > rBase) { dx *= (rBase/len); dy *= (rBase/len); len = rBase; }
        knobX = cx + dx; knobY = cy + dy; invalidate();

        float norm = (rBase > 0f) ? (len / rBase) : 0f; // 0..1
        if (norm < deadzone) { // dentro de deadzone no hay teclas
            pressKeys(false, false, false, false);
            return;
        }

        float nx = dx / rBase;
        float ny = -dy / rBase;
        double ang = Math.toDegrees(Math.atan2(ny, nx)); // [-180..180], 0=este(D)

        if (Math.abs(ang -   0) <= cardinalBiasDeg) ang =   0;   // E (D)
        if (Math.abs(ang -  90) <= cardinalBiasDeg) ang =  90;   // N (W)
        if (Math.abs(ang +  90) <= cardinalBiasDeg) ang = -90;   // S (S)
        if (Math.abs(Math.abs(ang) - 180) <= cardinalBiasDeg) ang = 180; // O (A)

        boolean W=false,A=false,S=false,D=false;
        if (mode == Mode.DIR4) {
            if (ang >= -45 && ang < 45) D = true;           // E
            else if (ang >= 45 && ang < 135) W = true;      // N
            else if (ang >= -135 && ang < -45) S = true;    // S
            else A = true;                                  // O
        } else {
            if (ang >= -22.5 && ang < 22.5) D = true;                    // E
            else if (ang >= 22.5 && ang < 67.5) { D = true; W=true; }    // NE
            else if (ang >= 67.5 && ang < 112.5) W = true;               // N
            else if (ang >= 112.5 && ang < 157.5){ W=true; A=true; }     // NO
            else if (ang >= 157.5 || ang < -157.5) A = true;             // O
            else if (ang >= -157.5 && ang < -112.5){ A=true; S=true; }   // SO
            else if (ang >= -112.5 && ang < -67.5) S = true;             // S
            else /* -67.5..-22.5 */ { S=true; D=true; }                  // SE
        }
        pressKeys(W,A,S,D);
    }

    private void pressKeys(boolean W, boolean A, boolean S, boolean D) {
        setKey(NativeBridge.CODE_KEY_W, W);
        setKey(NativeBridge.CODE_KEY_A, A);
        setKey(NativeBridge.CODE_KEY_S, S);
        setKey(NativeBridge.CODE_KEY_D, D);
    }

    private void setKey(int code, boolean on) {
        if (on) {
            if (!pressed.contains(code)) {
                pressed.add(code);
                NativeBridge.onButton(code, true);
            }
        } else {
            if (pressed.remove(code)) {
                NativeBridge.onButton(code, false);
            }
        }
    }

    private void releaseAllKeys() {
        for (Integer code : pressed.toArray(new Integer[0])) {
            NativeBridge.onButton(code, false);
        }
        pressed.clear();
    }
}
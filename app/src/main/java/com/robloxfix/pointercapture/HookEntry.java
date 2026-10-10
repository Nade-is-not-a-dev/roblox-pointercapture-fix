package com.robloxfix.pointercapture;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v2.5 - Gate-forcing MANUAL ONLY (AUTO dihapus per request).
 *
 * Pipeline Roblox mk/e 2.742.1059:
 *  - e$e.onGenericMotion: jika nativeGetMainWindowIsMouseLockedCenter()==true -> requestPointerCapture()
 *  - e$d.onCapturedPointer: jika true -> z() AXIS_RELATIVE -> 360°, jika false -> releasePointerCapture()
 *  - y(): delta getX/getY absolut -> mentok = stuck
 *
 * v2.5: Tidak ada auto saat tahan klik kanan/Shift. Hanya:
 *  - Ctrl+Alt toggle FORCE_OFF <-> FORCE_ON
 *  - ESC / BACK / sentuh jari -> FORCE_OFF + release
 *  Kursor hilang saat ON (TYPE_NULL), muncul saat OFF.
 */
public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "RobloxPointerFix";
    private static final String ROBLOX_PKG = "com.roblox.client";
    private static final String NATIVE_CLS = "com.roblox.engine.jni.NativeInputInterface";

    // 0=FORCE_OFF (kursor bebas), 1=FORCE_ON (kursor lock 360°)
    private static volatile int forceMode = 0;
    private static volatile boolean lastGate = false;

    private static volatile View capturedView = null;
    private static volatile Activity lastActivity = null;

    private void log(String s) {
        XposedBridge.log("[" + TAG + "] " + s);
    }

    private static final String[] MODE_NAMES = {"FORCE_OFF", "FORCE_ON"};

    private void toast(final String msg) {
        try {
            final Activity act = lastActivity;
            if (act == null) return;
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        android.widget.Toast.makeText(act.getApplicationContext(),
                                "[PointerFix] " + msg,
                                android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    private static boolean gateWanted() {
        return forceMode == 1;
    }

    private void logGate(String why) {
        boolean g = gateWanted();
        if (g != lastGate) {
            lastGate = g;
            log("gate -> " + g + " (" + why + " mode=" + MODE_NAMES[forceMode] + ")");
        }
    }

    private void releaseCapture(String why) {
        try {
            View v = capturedView;
            if (v != null) {
                try { v.releasePointerCapture(); } catch (Throwable ignored) {}
            }
            Activity act = lastActivity;
            if (act != null && act.getWindow() != null) {
                try { act.getWindow().getDecorView().releasePointerCapture(); }
                catch (Throwable ignored) {}
                try {
                    View f = act.getWindow().getCurrentFocus();
                    if (f != null && f != v) f.releasePointerCapture();
                } catch (Throwable ignored) {}
            }
            capturedView = null;
            log("releaseCapture (" + why + ")");
        } catch (Throwable t) {
            log("releaseCapture fail: " + t);
        }
    }

    private void markActive() {
        try {
            java.io.File f = new java.io.File("/data/local/tmp/rbxlsp_active");
            f.createNewFile();
            try { Runtime.getRuntime().exec(new String[]{"chmod", "644", "/data/local/tmp/rbxlsp_active"}); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!ROBLOX_PKG.equals(lpparam.packageName)) return;
        markActive();
        log("v2.6 loaded in " + lpparam.packageName + " process=" + lpparam.processName);

        final ClassLoader cl = lpparam.classLoader;

        // 1. GATE
        try {
            XposedHelpers.findAndHookMethod(NATIVE_CLS, cl,
                    "nativeGetMainWindowIsMouseLockedCenter", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (gateWanted()) param.setResult(true);
                }
            });
            log("hooked nativeGetMainWindowIsMouseLockedCenter");
        } catch (Throwable t) {
            log("HOOK FAIL gate: " + t);
        }

        // 2. Ctrl+Alt toggle FORCE_OFF <-> FORCE_ON, ESC/BACK -> OFF
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent",
                    KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity act = (Activity) param.thisObject;
                    lastActivity = act;
                    KeyEvent e = (KeyEvent) param.args[0];
                    if (e == null) return;
                    int kc = e.getKeyCode();
                    boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
                    if ((kc == KeyEvent.KEYCODE_ESCAPE
                            || kc == KeyEvent.KEYCODE_BACK) && down) {
                        if (forceMode != 0) {
                            forceMode = 0;
                            log("forceMode -> FORCE_OFF (menu)");
                            toast("Mode: FORCE_OFF");
                        }
                        releaseCapture("menu key");
                        logGate("menu");
                    } else if (down && e.isCtrlPressed() && e.isAltPressed()) {
                        forceMode = 1 - forceMode;
                        log("forceMode -> " + MODE_NAMES[forceMode] + " (Ctrl+Alt)");
                        toast("Mode: " + MODE_NAMES[forceMode]);
                        logGate("toggle");
                        if (forceMode == 0) releaseCapture("toggle OFF");
                    }
                }
            });
            log("hooked Activity.dispatchKeyEvent");
        } catch (Throwable t) {
            log("HOOK FAIL keys: " + t);
        }

        // 3. Sentuh jari -> FORCE_OFF + release
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "dispatchTouchEvent",
                    MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MotionEvent e = (MotionEvent) param.args[0];
                    if (e == null) return;
                    if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                            && e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                        if (forceMode == 1 || capturedView != null) {
                            forceMode = 0;
                            releaseCapture("finger touch");
                            logGate("finger");
                            // no toast spam on finger, only log
                        }
                    }
                }
            });
            log("hooked Activity.dispatchTouchEvent");
        } catch (Throwable t) {
            log("HOOK FAIL touch: " + t);
        }

        // 4. Catat view yang pegang capture
        try {
            XposedHelpers.findAndHookMethod(View.class, "onPointerCaptureChanged",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View v = (View) param.thisObject;
                    boolean has = (boolean) param.args[0];
                    if (has) capturedView = v;
                    else if (capturedView == v) capturedView = null;
                    log("onPointerCaptureChanged hasCapture=" + has
                            + " view=" + v.getClass().getName());
                }
            });
            log("hooked View.onPointerCaptureChanged");
        } catch (Throwable t) {
            log("HOOK FAIL capture-cb: " + t);
        }

        // 5. Kursor hilang saat ON, muncul saat OFF
        try {
            XposedHelpers.findAndHookMethod(View.class, "onResolvePointerIcon",
                    MotionEvent.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!gateWanted()) return;
                    View v = (View) param.thisObject;
                    try {
                        android.view.PointerIcon nullIcon =
                                android.view.PointerIcon.getSystemIcon(v.getContext(),
                                        android.view.PointerIcon.TYPE_NULL);
                        param.setResult(nullIcon);
                    } catch (Throwable ignored) {}
                }
            });
            log("hooked View.onResolvePointerIcon");
        } catch (Throwable t) {
            log("HOOK FAIL icon: " + t);
        }

        log("v2.5 hooks installed. Ctrl+Alt toggle FORCE_ON/OFF, ESC atau sentuh untuk OFF.");
    }
}

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * v2.6 — MANUAL ONLY, custom combos stored in /data/local/tmp/rbxlsp_keys.json
 * Default if no file: Ctrl+Alt (left/right any)
 */
public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "RobloxPointerFix";
    private static final String ROBLOX_PKG_PREFIX = "com.roblox.";
    private static final String NATIVE_CLS = "com.roblox.engine.jni.NativeInputInterface";

    private static boolean isRobloxPackage(String pkg) {
        return pkg != null && pkg.startsWith(ROBLOX_PKG_PREFIX);
    }
    private static final String KEYS_FILE = "/data/local/tmp/rbxlsp_keys.json";

    private static volatile int forceMode = 0; // 0=OFF 1=ON
    private static volatile boolean lastGate = false;
    private static volatile View capturedView = null;
    private static volatile Activity lastActivity = null;

    // combo state
    private static final Set<Integer> downKeys = new HashSet<>();
    private static String lastTriggeredCombo = null;

    private void log(String s) { XposedBridge.log("[" + TAG + "] " + s); }

    private static final String[] MODE_NAMES = {"FORCE_OFF", "FORCE_ON"};

    private void toast(final String msg) {
        try {
            final Activity act = lastActivity;
            if (act == null) return;
            act.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        android.widget.Toast.makeText(act.getApplicationContext(),
                                "[PointerFix] " + msg, android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    private static boolean gateWanted() { return forceMode == 1; }

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
            if (v != null) try { v.releasePointerCapture(); } catch (Throwable ignored) {}
            Activity act = lastActivity;
            if (act != null && act.getWindow() != null) {
                try { act.getWindow().getDecorView().releasePointerCapture(); } catch (Throwable ignored) {}
                try { View f = act.getWindow().getCurrentFocus(); if (f != null && f != v) f.releasePointerCapture(); } catch (Throwable ignored) {}
            }
            capturedView = null;
            log("releaseCapture (" + why + ")");
        } catch (Throwable t) { log("releaseCapture fail: " + t); }
    }

    private void markActive() {
        try {
            File f = new File("/data/local/tmp/rbxlsp_active");
            f.createNewFile();
            try { Runtime.getRuntime().exec(new String[]{"chmod", "644", "/data/local/tmp/rbxlsp_active"}); } catch (Throwable ignored) {}
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "touch /data/local/tmp/rbxlsp_active && chmod 644 /data/local/tmp/rbxlsp_active"});
            } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}
    }

    // ---- combo loading ----
    private List<Set<Integer>> loadCombos() {
        List<Set<Integer>> out = new ArrayList<>();
        // 1. try file
        try {
            File f = new File(KEYS_FILE);
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String txt = sb.toString().trim();
                if (!txt.isEmpty()) {
                    org.json.JSONArray arr = new org.json.JSONArray(txt);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONArray combo = arr.getJSONArray(i);
                        Set<Integer> s = new HashSet<>();
                        for (int j = 0; j < combo.length(); j++) s.add(combo.getInt(j));
                        if (!s.isEmpty()) out.add(s);
                    }
                    if (!out.isEmpty()) return out;
                }
            }
        } catch (Throwable t) { log("loadCombos file fail: " + t); }
        return out;
    }

    private boolean isComboPressed(Set<Integer> combo) {
        // combo pressed if all keys in combo are down, and no extra non-modifier?
        // We do exact match: downKeys equals combo (allow extra if combo is subset? use containsAll)
        // For single-key combo, allow exactly that key down.
        if (combo.size() == 1) return downKeys.equals(combo);
        // For multi-key, require all combo keys present (allow extra? we require exact to avoid false trigger)
        return downKeys.equals(combo);
    }

    private String comboToString(Set<Integer> c) {
        StringBuilder sb = new StringBuilder();
        for (Integer k : c) { if (sb.length() > 0) sb.append("+"); sb.append(k); }
        return sb.toString();
    }

    // default combo: Ctrl+Alt (any left/right)
    private boolean isDefaultCtrlAlt() {
        boolean ctrl = downKeys.contains(KeyEvent.KEYCODE_CTRL_LEFT) || downKeys.contains(KeyEvent.KEYCODE_CTRL_RIGHT);
        boolean alt = downKeys.contains(KeyEvent.KEYCODE_ALT_LEFT) || downKeys.contains(KeyEvent.KEYCODE_ALT_RIGHT);
        // also support generic ctrl/alt via meta? fallback: check exact 2 keys Ctrl+Alt
        if (ctrl && alt && downKeys.size() == 2) return true;
        // Also handle case where device sends KEYCODE_CTRL_LEFT + KEYCODE_ALT_LEFT as 2 keys
        if (downKeys.size() == 2 && ctrl && alt) return true;
        return false;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!isRobloxPackage(lpparam.packageName)) return;
        markActive();
        log("v2.6 loaded in " + lpparam.packageName + " process=" + lpparam.processName);

        final ClassLoader cl = lpparam.classLoader;

        try {
            XposedHelpers.findAndHookMethod(NATIVE_CLS, cl, "nativeGetMainWindowIsMouseLockedCenter", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (gateWanted()) param.setResult(true);
                }
            });
            log("hooked nativeGetMainWindowIsMouseLockedCenter");
        } catch (Throwable t) { log("HOOK FAIL gate: " + t); }

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "dispatchKeyEvent", KeyEvent.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity act = (Activity) param.thisObject;
                    lastActivity = act;
                    KeyEvent e = (KeyEvent) param.args[0];
                    if (e == null) return;
                    int kc = e.getKeyCode();
                    boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
                    // track down keys
                    if (down) {
                        downKeys.add(kc);
                    } else if (e.getAction() == KeyEvent.ACTION_UP) {
                        downKeys.remove(kc);
                        // reset trigger when any key up
                        lastTriggeredCombo = null;
                    }

                    if ((kc == KeyEvent.KEYCODE_ESCAPE || kc == KeyEvent.KEYCODE_BACK) && down) {
                        if (forceMode != 0) { forceMode = 0; log("forceMode -> FORCE_OFF (menu)"); toast("Mode: FORCE_OFF"); }
                        releaseCapture("menu key");
                        logGate("menu");
                        return;
                    }

                    if (!down) return; // only trigger on down

                    // check custom combos
                    List<Set<Integer>> combos = loadCombos();
                    boolean matched = false;
                    String matchedStr = null;
                    if (combos.isEmpty()) {
                        // default
                        if (isDefaultCtrlAlt()) { matched = true; matchedStr = "CTRL+ALT"; }
                    } else {
                        for (Set<Integer> c : combos) {
                            if (isComboPressed(c)) { matched = true; matchedStr = comboToString(c); break; }
                        }
                    }
                    if (matched) {
                        if (matchedStr != null && matchedStr.equals(lastTriggeredCombo)) return; // debounce
                        lastTriggeredCombo = matchedStr;
                        forceMode = 1 - forceMode;
                        log("forceMode -> " + MODE_NAMES[forceMode] + " (combo " + matchedStr + ")");
                        toast("Mode: " + MODE_NAMES[forceMode]);
                        logGate("toggle");
                        if (forceMode == 0) releaseCapture("toggle OFF");
                    }
                }
            });
            log("hooked Activity.dispatchKeyEvent");
        } catch (Throwable t) { log("HOOK FAIL keys: " + t); }

        try {
            XposedHelpers.findAndHookMethod(Activity.class, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MotionEvent e = (MotionEvent) param.args[0];
                    if (e == null) return;
                    if (e.getActionMasked() == MotionEvent.ACTION_DOWN && e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                        if (forceMode == 1 || capturedView != null) {
                            forceMode = 0; releaseCapture("finger touch"); logGate("finger");
                        }
                        downKeys.clear(); lastTriggeredCombo = null;
                    }
                }
            });
            log("hooked Activity.dispatchTouchEvent");
        } catch (Throwable t) { log("HOOK FAIL touch: " + t); }

        try {
            XposedHelpers.findAndHookMethod(View.class, "onPointerCaptureChanged", boolean.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View v = (View) param.thisObject;
                    boolean has = (boolean) param.args[0];
                    if (has) capturedView = v; else if (capturedView == v) capturedView = null;
                    log("onPointerCaptureChanged hasCapture=" + has + " view=" + v.getClass().getName());
                }
            });
            log("hooked View.onPointerCaptureChanged");
        } catch (Throwable t) { log("HOOK FAIL capture-cb: " + t); }

        try {
            XposedHelpers.findAndHookMethod(View.class, "onResolvePointerIcon", MotionEvent.class, int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!gateWanted()) return;
                    View v = (View) param.thisObject;
                    try {
                        android.view.PointerIcon nullIcon = android.view.PointerIcon.getSystemIcon(v.getContext(), android.view.PointerIcon.TYPE_NULL);
                        param.setResult(nullIcon);
                    } catch (Throwable ignored) {}
                }
            });
            log("hooked View.onResolvePointerIcon");
        } catch (Throwable t) { log("HOOK FAIL icon: " + t); }

        log("v2.6 hooks installed. Ctrl+Alt (or custom) toggle FORCE_ON/OFF, Esc/touch for OFF.");
    }
}

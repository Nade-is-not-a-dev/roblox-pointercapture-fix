package com.robloxfix.pointercapture;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private TextView statusDot, statusText;

    // ---- helpers ----
    private boolean isModuleEnabled() {
        // Xposed adds this flag to the ApplicationInfo when the module is active.
        // LSPosed sets "xposedmodule" meta-data; we check via PackageManager isEnabled+meta.
        // Most reliable: try to load XposedBridge class - if present and hook succeeded, log exists.
        // Simpler: check if LSPosed manager is installed + module enabled is tracked via own pref.
        // Here: check if HookEntry was loaded (we write a system prop / file on hook).
        // Fallback: detect LSPosed/Xposed presence.
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            // Could also check /data/misc/lspd etc, but existence of class is not enough.
            // We check shared pref written by module via world-readable file - if hook ran, file exists.
            java.io.File f = new java.io.File("/data/local/tmp/rbxlsp_active");
            if (f.exists()) return true;
        } catch (Throwable ignored) {}
        // Also try reading module's own enabled state via PackageManager GET_META_DATA
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            if (ai.metaData != null && ai.metaData.getBoolean("xposedmodule", false)) {
                // Check LSPosed scope enabled by querying Xposed's config (best effort)
                // If LSPosed is installed, the Xposed framework itself will be present on device.
                // We treat "Xposed present + module installed" as potentially active, but warn to check LSPosed.
                // Returning false here keeps banner yellow ("verify in LSPosed") instead of fake green.
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isLSPosedInstalled() {
        for (String pkg : new String[]{
                "org.lsposed.manager", "com.android.shell", "io.github.lsposed.manager"}) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                return true;
            } catch (Throwable ignored) {}
        }
        // Also check LSPosed framework binary
        try {
            return new java.io.File("/data/adb/lspd").exists()
                    || new java.io.File("/data/adb/modules/zygisk_lsposed").exists()
                    || new java.io.File("/data/adb/modules/riru_lsposed").exists();
        } catch (Throwable ignored) {}
        return false;
    }

    private void copy(String s) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("rbxlsp", s));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    // ---- UI builders ----
    private CardView card(LinearLayout parent) {
        CardView cv = new CardView(this);
        cv.setRadius(dp(14));
        cv.setCardElevation(dp(2));
        cv.setUseCompatPadding(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        cv.setLayoutParams(lp);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        cv.addView(inner);
        cv.setTag(inner);
        parent.addView(cv);
        return cv;
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private TextView sectionTitle(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(11);
        tv.setLetterSpacing(0.06f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#6B7280"));
        tv.setPadding(0, dp(4), 0, dp(6));
        return tv;
    }

    private TextView body(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(13.5f);
        tv.setTextColor(Color.parseColor("#111827"));
        tv.setLineSpacing(dp(2), 1f);
        return tv;
    }

    private TextView mono(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(Color.parseColor("#374151"));
        tv.setTextIsSelectable(true);
        return tv;
    }

    private TextView bullet(String s) {
        TextView tv = new TextView(this);
        tv.setText("•  " + s);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#1F2937"));
        tv.setPadding(0, dp(3), 0, dp(3));
        return tv;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.parseColor("#F9FAFB"));
        sv.addView(root);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, dp(8), 0, dp(16));
        root.addView(header);

        TextView hTitle = new TextView(this);
        hTitle.setText("Roblox Pointer Fix");
        hTitle.setTextSize(22);
        hTitle.setTypeface(null, Typeface.BOLD);
        hTitle.setTextColor(Color.parseColor("#111827"));
        header.addView(hTitle);

        TextView hSub = new TextView(this);
        hSub.setText("OTG mouse & keyboard — 360° camera");
        hSub.setTextSize(13);
        hSub.setTextColor(Color.parseColor("#6B7280"));
        hSub.setPadding(0, dp(4), 0, 0);
        header.addView(hSub);

        TextView hVer = new TextView(this);
        hVer.setText("v2.5  •  Android 8+  •  KernelSU / Magisk + LSPosed");
        hVer.setTextSize(11);
        hVer.setTextColor(Color.parseColor("#9CA3AF"));
        hVer.setPadding(0, dp(6), 0, 0);
        header.addView(hVer);

        // Status card
        CardView statusCard = card(root);
        LinearLayout sc = (LinearLayout) statusCard.getTag();
        sc.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        sc.addView(statusRow);

        statusDot = new TextView(this);
        statusDot.setText("●");
        statusDot.setTextSize(14);
        statusRow.addView(statusDot);

        statusText = new TextView(this);
        statusText.setTextSize(13);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setPadding(dp(8), 0, 0, 0);
        statusText.setTextColor(Color.parseColor("#111827"));
        statusRow.addView(statusText);

        TextView statusHint = new TextView(this);
        statusHint.setTextSize(12);
        statusHint.setTextColor(Color.parseColor("#6B7280"));
        statusHint.setPadding(dp(22), dp(6), 0, 0);
        sc.addView(statusHint);

        // LSPosed hint row + button
        LinearLayout statusBtnRow = new LinearLayout(this);
        statusBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        statusBtnRow.setPadding(0, dp(10), 0, 0);
        statusBtnRow.setVisibility(View.GONE);
        sc.addView(statusBtnRow);
        MaterialButton btnOpenManager = new MaterialButton(this);
        btnOpenManager.setText("Open LSPosed");
        btnOpenManager.setCornerRadius(dp(8));
        statusBtnRow.addView(btnOpenManager);

        // How it works
        root.addView(sectionTitle("HOW IT WORKS"));
        CardView howCard = card(root);
        LinearLayout hc = (LinearLayout) howCard.getTag();
        hc.addView(body("Fixes the edge-stuck camera when using a mouse on Android. Roblox on mobile calculates camera movement from cursor position, so it stops at screen edges. This module unlocks the native pointer-capture path so movement is continuous."));
        TextView howMono = mono("Native gate:  nativeGetMainWindowIsMouseLockedCenter()\nScope:      com.roblox.client only");
        howMono.setPadding(0, dp(10), 0, 0);
        hc.addView(howMono);

        // How to use
        root.addView(sectionTitle("HOW TO USE"));
        CardView useCard = card(root);
        LinearLayout uc = (LinearLayout) useCard.getTag();
        uc.addView(bullet("Install, enable in LSPosed with scope com.roblox.client, reboot."));
        uc.addView(bullet("Connect OTG mouse + keyboard before joining a game."));
        uc.addView(bullet("Ctrl + Alt — toggle lock on/off."));
        uc.addView(bullet("Esc / Back or tap with finger — unlock and return to normal cursor."));

        // Scope
        root.addView(sectionTitle("REQUIRED SCOPE"));
        CardView scopeCard = card(root);
        LinearLayout spc = (LinearLayout) scopeCard.getTag();
        TextView scopeMono = mono("com.roblox.client");
        scopeMono.setTextSize(13);
        scopeMono.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        spc.addView(scopeMono);
        TextView scopeNote = body("In LSPosed, enable only this package. Enabling System Framework is not needed.");
        scopeNote.setTextSize(12);
        scopeNote.setTextColor(Color.parseColor("#6B7280"));
        scopeNote.setPadding(0, dp(8), 0, 0);
        spc.addView(scopeNote);
        MaterialButton btnCopyScope = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCopyScope.setText("Copy package name");
        btnCopyScope.setCornerRadius(dp(8));
        spc.addView(btnCopyScope);

        // Hide root
        root.addView(sectionTitle("HIDE ROOT"));
        CardView hideCard = card(root);
        LinearLayout hdc = (LinearLayout) hideCard.getTag();
        hdc.addView(body("Roblox detects root and LSPosed. Hide it or you will get \"Security Thread Detected\"."));
        hdc.addView(bullet("KernelSU / Magisk + ZygiskNext + LSPosed"));
        hdc.addView(bullet("HMAL or Shamiko — hide com.roblox.client"));
        hdc.addView(bullet("Add Roblox to DenyList / Enforce DenyList off (KernelSU)"));

        // Debug
        root.addView(sectionTitle("DEBUG"));
        CardView dbgCard = card(root);
        LinearLayout dc = (LinearLayout) dbgCard.getTag();
        TextView dbgMono = mono("adb logcat -s RobloxPointerFix");
        dc.addView(dbgMono);
        MaterialButton btnCopyLog = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCopyLog.setText("Copy log command");
        btnCopyLog.setCornerRadius(dp(8));
        dc.addView(btnCopyLog);

        // Footer
        TextView footer = new TextView(this);
        footer.setText("Not affiliated with Roblox.  •  Module is separate from FFlag Force.");
        footer.setTextSize(11);
        footer.setTextColor(Color.parseColor("#9CA3AF"));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, dp(8));
        root.addView(footer);

        // ---- wiring ----
        btnCopyScope.setOnClickListener(v -> copy("com.roblox.client"));
        btnCopyLog.setOnClickListener(v -> copy("adb logcat -s RobloxPointerFix"));
        btnOpenManager.setOnClickListener(v -> {
            for (String pkg : new String[]{"org.lsposed.manager", "io.github.lsposed.manager"}) {
                try {
                    Intent it = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (it != null) { startActivity(it); return; }
                } catch (Throwable ignored) {}
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LSPosed/LSPosed/releases")));
            } catch (Throwable ignored) {}
        });

        setContentView(sv);

        refreshStatus();
    }

    private void refreshStatus() {
        boolean lsposed = isLSPosedInstalled();
        boolean active = isModuleEnabled();

        if (!lsposed) {
            statusDot.setTextColor(Color.parseColor("#F59E0B"));
            statusText.setText("LSPosed not detected");
            statusText.setTextColor(Color.parseColor("#92400E"));
            TextView h = (TextView) ((LinearLayout) ((CardView) statusDot.getParent().getParent()).getTag()).getChildAt(1);
            h.setText("Install LSPosed (JingMatrix fork) via KernelSU / Magisk, then enable this module.");
        } else if (active) {
            statusDot.setTextColor(Color.parseColor("#10B981"));
            statusText.setText("Active");
            statusText.setTextColor(Color.parseColor("#065F46"));
            TextView hint = (TextView) ((LinearLayout) ((CardView) statusDot.getParent().getParent()).getTag()).getChildAt(1);
            hint.setText("Module is loaded. If camera still sticks, verify scope is com.roblox.client and reboot.");
        } else {
            statusDot.setTextColor(Color.parseColor("#F59E0B"));
            statusText.setText("Check LSPosed");
            statusText.setTextColor(Color.parseColor("#92400E"));
            TextView hint = (TextView) ((LinearLayout) ((CardView) statusDot.getParent().getParent()).getTag()).getChildAt(1);
            hint.setText("Open LSPosed → Modules → enable this module → check com.roblox.client → reboot.");
            View row = ((LinearLayout) ((CardView) statusDot.getParent().getParent()).getTag()).getChildAt(2);
            row.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }
}

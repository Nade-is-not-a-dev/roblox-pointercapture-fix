# Roblox PonterCapture Fix

Fix for the stuck camera when using a mouse on Roblox Android (OTG).

Roblox on Android calculates camera movement from cursor position, so the view stops when the cursor hits the screen edge. This module unlocks the native pointer-capture path so movement is continuous — 360° like on PC.

Verified on `libroblox.so` 2.742.1059 (arm64): the only absolute delta path is `mk/e.y()` (`getX/getY`), the only relative path is `mk/e.z()` (`AXIS_RELATIVE_X/Y`) gated by `nativeGetMainWindowIsMouseLockedCenter()`.

## Requirements

- Android 8.0+
- KernelSU / Magisk + ZygiskNext + LSPosed (JingMatrix fork)
- Root hidden from Roblox (otherwise `Security Thread Detected`)

### Hide root

1. Install ZygiskNext + LSPosed via KernelSU/Magisk Manager, reboot.
2. Install HMAL or Shamiko, hide `com.roblox.client`.
3. Add Roblox to DenyList, keep Enforce DenyList off (KernelSU).

## Install

1. Install the APK from Releases.
2. LSPosed Manager → Modules → enable **Pointer Fix** → check **com.roblox.client** only.
3. Reboot.
4. Connect OTG mouse + keyboard before joining a game.

## Use

- `Ctrl + Alt` — toggle lock on/off.
- `Esc` / `Back` or tap with finger — unlock and restore cursor.

When locked, the cursor is hidden and camera rotates continuously. When unlocked, the cursor returns for menus.

## Scope

```
com.roblox.client
```

System Framework is not needed.

## Debug

```
adb logcat -s RobloxPointerFix
```

Or check the in-app status card.

## Build

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## Disclaimer

Not affiliated with Roblox. For rooted devices only.

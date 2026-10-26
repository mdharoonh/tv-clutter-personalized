# Making Home-Personalised your TV's default home, and undoing it

Everything here uses `pm disable-user`, which is reversible. Nothing is uninstalled. Do not disable core system packages. Write down what you disable so you can undo it.

Use your own TV's IP in place of `<tv-ip>`. Prefix commands with `adb -s <tv-ip>:5555`.

## Why it is not just "pick a default"

Android TV picks the Home app by filter priority. The stock launcher (`com.google.android.apps.tv.launcherx` on Google TV) has priority 2, and the setup wizard (`com.google.android.tungsten.setupwraith`) has priority 1. A normal app has 0 and cannot raise it, so the role and "preferred home" settings are ignored until both are disabled. The shell cannot disable a single component, only whole packages.

The package names differ by TV. List what handles Home on yours:

```
adb shell pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Steps

```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.rabbit.tvhome
adb shell cmd package set-home-activity com.rabbit.tvhome/.HomeActivity
adb shell input keyevent 3
```

Re-run the `query-activities` command. Home-Personalised should be the only entry. If another package appears, it is outranking you; decide whether to disable it too.

## Side effects

- Voice search, recommendations and the stock "For you" rows live in the stock launcher and will not appear.

## Undo everything

```
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell pm enable com.google.android.tungsten.setupwraith
adb shell cmd role remove-role-holder --user 0 android.app.role.HOME com.rabbit.tvhome
adb shell pm clear-preferred-activities com.rabbit.tvhome
adb shell input keyevent 3
```

If that still opens the wrong thing: `adb shell pm uninstall --user 0 com.rabbit.tvhome`.

## If ADB stops working and Home is broken

- Open Settings with the remote's settings button, then Apps, show system apps, and enable the stock launcher and setup wizard again.
- You may need to re-enable wireless debugging in Developer options.

## Optional: debloat

Disabling vendor apps you do not use frees memory. Check each package with `pm list packages` first and only disable what you recognise. List what is disabled with `adb shell pm list packages -d`, and re-enable with `pm enable <package>`.

## HDMI tiles

The HDMI tile switches inputs through `TvInputManager`. It matches an input by its label ending in the port number, or by the id suffix `/HW(port+4)` (MediaTek: HW5 is HDMI 1, HW6 is HDMI 2). Other brands may differ; list your inputs with `adb shell dumpsys tv_input`.

## Debugging

- Crashes: `adb logcat -d | grep -E "FATAL EXCEPTION|ANR in|OutOfMemory"`
- Top activity: `adb shell dumpsys activity activities | grep mResumedActivity`
- Memory: `adb shell dumpsys meminfo com.rabbit.tvhome` (about 160 MB total on the test TV)
- Screenshot: `adb exec-out screencap -p > raw.bin`, then cut everything before the PNG header (`\x89PNG`).

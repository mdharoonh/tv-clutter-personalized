package com.rabbit.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.widget.Toast;

/** Opens apps, deep links and HDMI inputs. Every path ends in a toast instead of a crash when the target is missing. */
final class Launch {
    private static final String YOUTUBE_TV = "com.google.android.youtube.tv";

    /** Opens the app, on the link when there is one and the app takes it, otherwise on its home screen. */
    static void app(Context c, Cfg.App a, String link) {
        PackageManager pm = c.getPackageManager();
        String pkg = installed(pm, a);
        if (pkg == null) {
            toast(c, a.label + " is not installed");
            return;
        }
        if (link != null && !link.isEmpty()) {
            try {
                c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(pkg)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // the app does not take that link: open it normally
            }
        }
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            toast(c, a.label + " cannot be opened");
            return;
        }
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    static void wifi(Context c) {
        try {
            c.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            settings(c);
        }
    }

    static void settings(Context c) {
        try {
            c.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(c, "Settings could not be opened");
        }
    }

    /** A web link (the kids' playlists): YouTube's TV app if present, otherwise whatever opens it. */
    static void link(Context c, String url) {
        Intent v = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(new Intent(v).setPackage(YOUTUBE_TV));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
        }
        try {
            c.startActivity(v);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(c, "Nothing can open this link");
        }
    }

    /** Switches the TV to the given HDMI port, the way the system's own Inputs menu does. */
    static void hdmi(Context c, int port) {
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        String id = null;
        if (tim != null) {
            try {
                for (TvInputInfo info : tim.getTvInputList()) {
                    if (!info.isPassthroughInput() || info.getType() != TvInputInfo.TYPE_HDMI || info.getParentId() != null) continue;
                    CharSequence label = info.loadLabel(c);
                    if ((label != null && label.toString().trim().endsWith(String.valueOf(port)))
                            || info.getId().endsWith("/HW" + (port + 4))) { // MediaTek TVs: HW5 = HDMI 1
                        id = info.getId();
                        break;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (id == null) {
            toast(c, "HDMI " + port + " was not found");
            return;
        }
        Uri uri = TvContract.buildChannelUriForPassthroughInput(id);
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(c, "Could not switch to HDMI " + port);
        }
    }

    static String installed(PackageManager pm, Cfg.App a) {
        for (String p : a.packages) {
            try {
                pm.getPackageInfo(p, 0);
                return p;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }
}

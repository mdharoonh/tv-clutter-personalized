package com.rabbit.tvhome;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** assets/config.json, parsed once. Everything the sections show (names, apps, links, art files, positions) comes from here. */
final class Cfg {
    static final class App { String key, label, logo; boolean logoFill; int color; final List<String> packages = new ArrayList<>(); }

    /** One of the four radial slots: an app, or an HDMI port. */
    static final class Slot { String type, app, label, logo; boolean logoFill; int port, color; }

    /** A favourites character. x, y, w, h are fractions of the collage canvas. */
    static final class Fav { String id, title, app, link, cutout, scene; int color; boolean front; float x, y, w, h; }

    /** A kids island. x, y, w, h are fractions of the screen. */
    static final class World { String id, title, desc, link; float x, y, w, h; }

    final Map<String, App> apps = new HashMap<>();
    final Map<String, Slot> radial = new HashMap<>();
    final Map<String, String> radialArt = new HashMap<>();
    final List<Fav> favs = new ArrayList<>();
    final List<World> worlds = new ArrayList<>();
    String kidsScene;
    float favAspect = 0.47f;

    static Cfg load(Context c) throws Exception {
        InputStream in = c.getAssets().open("config.json");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        for (int n; (n = in.read(chunk)) > 0; ) buf.write(chunk, 0, n);
        in.close();
        JSONObject j = new JSONObject(buf.toString("UTF-8"));
        Cfg cfg = new Cfg();
        cfg.favAspect = (float) j.optDouble("favAspect", 0.47);

        JSONObject apps = j.getJSONObject("apps");
        for (Iterator<String> it = apps.keys(); it.hasNext(); ) {
            String k = it.next();
            JSONObject o = apps.getJSONObject(k);
            App a = new App();
            a.key = k;
            a.label = o.getString("label");
            a.color = Color.parseColor(o.getString("color"));
            a.logo = o.optString("logo", "");
            a.logoFill = o.optBoolean("logoFill", false);
            JSONArray p = o.getJSONArray("packages");
            for (int i = 0; i < p.length(); i++) a.packages.add(p.getString(i));
            cfg.apps.put(k, a);
        }

        JSONObject r = j.getJSONObject("radial");
        JSONObject art = r.getJSONObject("art");
        for (Iterator<String> it = art.keys(); it.hasNext(); ) {
            String k = it.next();
            cfg.radialArt.put(k, art.getString(k));
        }
        for (String d : new String[]{"up", "left", "right", "down"}) {
            JSONObject o = r.getJSONObject(d);
            Slot s = new Slot();
            s.type = o.getString("type");
            s.app = o.optString("app", "");
            s.port = o.optInt("port", 0);
            if (s.type.equals("app")) {
                App a = cfg.apps.get(s.app);
                if (a == null) throw new IllegalArgumentException("radial." + d + ": unknown app " + s.app);
                s.label = a.label;
                s.color = a.color;
                s.logo = a.logo;
                s.logoFill = a.logoFill;
            } else {
                s.label = o.getString("label");
                s.color = Color.parseColor(o.optString("color", "#4aa3ff"));
                s.logo = o.optString("logo", "");
            }
            cfg.radial.put(d, s);
        }

        JSONArray f = j.getJSONArray("favourites");
        for (int i = 0; i < f.length(); i++) {
            JSONObject o = f.getJSONObject(i);
            Fav v = new Fav();
            v.id = o.getString("id");
            v.title = o.getString("title");
            v.app = o.getString("app");
            if (!cfg.apps.containsKey(v.app)) throw new IllegalArgumentException(v.id + ": unknown app " + v.app);
            v.link = o.optString("link", "");
            v.cutout = o.getString("cutout");
            v.scene = o.optString("scene", "");
            v.color = Color.parseColor(o.getString("color"));
            v.front = o.getString("row").equals("front");
            v.x = (float) o.getDouble("x");
            v.y = (float) o.getDouble("y");
            v.w = (float) o.getDouble("w");
            v.h = (float) o.getDouble("h");
            cfg.favs.add(v);
        }

        JSONObject k = j.getJSONObject("kids");
        cfg.kidsScene = k.getString("scene");
        JSONArray w = k.getJSONArray("worlds");
        for (int i = 0; i < w.length(); i++) {
            JSONObject o = w.getJSONObject(i);
            World v = new World();
            v.id = o.getString("id");
            v.title = o.getString("title");
            v.desc = o.getString("desc");
            v.link = o.getString("link");
            v.x = (float) o.getDouble("x");
            v.y = (float) o.getDouble("y");
            v.w = (float) o.getDouble("w");
            v.h = (float) o.getDouble("h");
            cfg.worlds.add(v);
        }
        return cfg;
    }
}

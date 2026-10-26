package com.rabbit.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/** Section 1: four apps on a ring around the warrior, who changes pose with the selection. */
final class RadialSection implements Section {
    private static final String[] DIRS = {"up", "left", "right", "down"};

    private final Context ctx;
    private final Cfg cfg;
    private final Map<String, Bitmap> poses = new HashMap<>();
    private final Map<String, Bitmap> logos = new HashMap<>();
    private final View view;
    private String sel = "idle";
    private String prev = "idle";
    private float fade = 1f;
    private ValueAnimator anim;

    RadialSection(Context c, Cfg cfg) {
        this.ctx = c;
        this.cfg = cfg;
        for (Map.Entry<String, String> e : cfg.radialArt.entrySet()) {
            Bitmap b = Art.load(c, e.getValue(), 900);
            if (b != null) poses.put(e.getKey(), b);
        }
        for (String d : DIRS) {
            Cfg.Slot sl = cfg.radial.get(d);
            Bitmap b = Art.load(c, sl.logo, 700);
            if (b != null) logos.put(d, b);
        }
        view = new RadialView(c);
    }

    public View view() { return view; }

    public boolean onDir(int dir) {
        if (sel.startsWith("bar") || sel.equals("wifi")) { // Home, Settings and Wi-Fi along the top
            if (dir == LEFT) { if (sel.equals("wifi")) select("bar1"); else if (sel.equals("bar1")) select("bar0"); }
            else if (dir == RIGHT) { if (sel.equals("bar0")) select("bar1"); else if (sel.equals("bar1")) select("wifi"); }
            else if (dir == DOWN) select("up");
            return true;
        }
        if (dir == UP && sel.equals("up")) { select("bar0"); return true; }
        String d = DIRS[dir == UP ? 0 : dir == LEFT ? 1 : dir == RIGHT ? 2 : 3];
        if (dir == DOWN && sel.equals("down")) return false;
        if (!d.equals(sel)) select(d);
        return true;
    }

    /** Back from a selected option returns the warrior to idle. */
    public boolean onBack() {
        if (sel.equals("idle")) return false;
        select("idle");
        return true;
    }

    public void onOk() {
        if (sel.equals("bar0")) { select("idle"); return; }
        if (sel.equals("bar1")) { Launch.settings(ctx); return; }
        if (sel.equals("wifi")) { Launch.wifi(ctx); return; }
        Cfg.Slot s = cfg.radial.get(sel);
        if (s == null) return;
        if (s.type.equals("hdmi")) Launch.hdmi(ctx, s.port);
        else Launch.app(ctx, cfg.apps.get(s.app), null);
    }

    public void onEnter(boolean fromAbove) {
        select("idle");
    }

    /** Crossfades from the current pose to the new one; the body stays put between poses, so only the arms appear to move. */
    private void select(String d) {
        if (anim != null) anim.cancel();
        prev = sel;
        sel = d;
        fade = 0f;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(200);
        anim.addUpdateListener(a -> { fade = (float) a.getAnimatedValue(); view.invalidate(); });
        anim.start();
    }

    public void onLeave() { }

    private final class RadialView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        RadialView(Context c) {
            super(c);
            setLayerType(LAYER_TYPE_SOFTWARE, null); // pill glow uses setShadowLayer
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h * 0.52f;
            Cfg.Slot s = cfg.radial.get(sel);
            int glow = s != null ? s.color : Color.rgb(90, 110, 170);

            c.drawColor(Color.rgb(6, 8, 14));
            Bitmap bg = poses.get("bg");
            if (bg != null) {
                float sc = Math.max(w / bg.getWidth(), h / bg.getHeight());
                float bw = bg.getWidth() * sc, bh = bg.getHeight() * sc;
                r.set((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2);
                p.setFilterBitmap(true);
                c.drawBitmap(bg, null, r, p);
            }
            p.setShader(new RadialGradient(cx, cy, h * 0.75f,
                    Color.argb(s != null ? 110 : 60, Color.red(glow), Color.green(glow), Color.blue(glow)),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            float ring = h * 0.38f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            p.setColor(Color.argb(50, 255, 255, 255));
            c.drawCircle(cx, cy, ring, p);
            p.setStyle(Paint.Style.FILL);

            p.setFilterBitmap(true);
            if (fade < 1f) drawPose(c, prev, cx, cy, h, 255);
            drawPose(c, sel, cx, cy, h, (int) (255 * fade));
            p.setAlpha(255);

            for (String d : DIRS) {
                Cfg.Slot sl = cfg.radial.get(d);
                float px = cx, py = cy;
                if (d.equals("up")) py = cy - ring;
                else if (d.equals("down")) py = cy + ring;
                else if (d.equals("left")) px = cx - ring * 1.55f;
                else px = cx + ring * 1.55f;
                drawPill(c, sl, logos.get(d), px, py, d.equals(sel));
            }
            drawBar(c, "Home", cx - h * 0.1f, h * 0.045f, sel.equals("bar0"));
            drawBar(c, "Settings", cx + h * 0.1f, h * 0.045f, sel.equals("bar1"));
            drawClock(c, w, h);
            drawWifi(c, w - h * 0.1f, h * 0.045f, sel.equals("wifi"));
        }

        private void drawPose(Canvas c, String name, float cx, float cy, float h, int alpha) {
            Bitmap b = poses.get(name);
            if (b == null) b = poses.get("idle");
            if (b == null) return;
            float ph = h * 0.6f, pw = ph * b.getWidth() / b.getHeight();
            r.set(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2);
            p.setAlpha(alpha);
            c.drawBitmap(b, null, r, p);
            p.setAlpha(255);
        }

        private void drawPill(Canvas c, Cfg.Slot sl, Bitmap logo, float x, float y, boolean on) {
            float pw = getHeight() * 0.3f, ph = getHeight() * 0.1f;
            r.set(x - pw / 2, y - ph / 2, x + pw / 2, y + ph / 2);
            p.setStyle(Paint.Style.FILL);
            if (on) p.setShadowLayer(40f, 0, 0, sl.color);
            p.setColor(on ? Color.argb(200, Color.red(sl.color) / 3, Color.green(sl.color) / 3, Color.blue(sl.color) / 3)
                    : Color.argb(150, 8, 10, 22));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(on ? 4f : 2f);
            p.setColor(on ? sl.color : Color.argb(90, 255, 255, 255));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            p.setStyle(Paint.Style.FILL);
            if (logo != null) {
                p.setFilterBitmap(true);
                p.setAlpha(on ? 255 : 200);
                if (sl.logoFill) { // the logo is its own pill
                    float in = on ? 4f : 2f;
                    r.inset(in, in);
                } else { // transparent logo: fit inside the glass pill
                    float lh = ph * 0.52f, lw = Math.min(lh * logo.getWidth() / logo.getHeight(), pw * 0.7f);
                    lh = lw * logo.getHeight() / logo.getWidth();
                    r.set(x - lw / 2, y - lh / 2, x + lw / 2, y + lh / 2);
                }
                c.drawBitmap(logo, null, r, p);
                p.setAlpha(255);
                return;
            }
            p.setColor(Color.WHITE);
            p.setTypeface(Art.font(getContext()));
            p.setTextSize(ph * 0.42f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(sl.label, x, y + ph * 0.15f, p);
        }

        private final Runnable tick = new Runnable() {
            public void run() { invalidate(); postDelayed(this, 20000); }
        };

        @Override
        protected void onAttachedToWindow() { super.onAttachedToWindow(); post(tick); }

        @Override
        protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }

        private void drawClock(Canvas c, float w, float h) {
            java.util.Date now = new java.util.Date();
            String time = new java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(now);
            String ampm = new java.text.SimpleDateFormat("a", java.util.Locale.getDefault()).format(now);
            String date = new java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(now);
            float x = h * 0.06f;
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(Art.font(getContext()));
            p.setTextAlign(Paint.Align.LEFT);
            p.setShadowLayer(14f, 0, 2, Color.BLACK);
            p.setColor(Color.WHITE);
            p.setTextSize(h * 0.115f);
            float y = h * 0.14f;
            c.drawText(time, x, y, p);
            float tw = p.measureText(time);
            p.setTextSize(h * 0.035f);
            c.drawText(ampm, x + tw + h * 0.012f, y, p);
            p.setColor(Color.argb(225, 255, 255, 255));
            p.setTextSize(h * 0.035f);
            c.drawText(date, x, y + h * 0.055f, p);
            p.clearShadowLayer();
        }

        private void drawWifi(Canvas c, float x, float y, boolean on) {
            String net = netLabel();
            boolean up = !net.equals("Offline");
            float pw = getHeight() * 0.17f, ph = getHeight() * 0.055f;
            r.set(x - pw / 2, y - ph / 2, x + pw / 2, y + ph / 2);
            p.setStyle(Paint.Style.FILL);
            if (on) p.setShadowLayer(30f, 0, 0, Color.WHITE);
            p.setColor(Color.argb(on ? 220 : 150, 8, 10, 22));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(on ? 4f : 2f);
            p.setColor(on ? Color.WHITE : Color.argb(90, 255, 255, 255));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            // wifi glyph: three arcs and a dot
            float gx = r.left + ph * 0.75f, gy = y + ph * 0.2f;
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(ph * 0.075f);
            p.setColor(up ? Color.WHITE : Color.argb(120, 255, 255, 255));
            for (int i = 1; i <= 3; i++) {
                float rad = ph * 0.15f * i + ph * 0.06f;
                r.set(gx - rad, gy - rad, gx + rad, gy + rad);
                c.drawArc(r, 225, 90, false, p);
            }
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(gx, gy, ph * 0.06f, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setTypeface(Art.font(getContext()));
            p.setTextSize(ph * 0.42f);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText(net, x - pw / 2 + ph * 1.35f, y + ph * 0.15f, p);
        }

        private String netLabel() {
            try {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (nc == null) return "Offline";
                if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
                if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
                return "Online";
            } catch (RuntimeException e) {
                return "Wi-Fi";
            }
        }

        private void drawBar(Canvas c, String text, float x, float y, boolean on) {
            float pw = getHeight() * 0.17f, ph = getHeight() * 0.055f;
            r.set(x - pw / 2, y - ph / 2, x + pw / 2, y + ph / 2);
            p.setStyle(Paint.Style.FILL);
            if (on) p.setShadowLayer(30f, 0, 0, Color.WHITE);
            p.setColor(Color.argb(on ? 220 : 150, 8, 10, 22));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(on ? 4f : 2f);
            p.setColor(on ? Color.WHITE : Color.argb(90, 255, 255, 255));
            c.drawRoundRect(r, ph / 2, ph / 2, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTypeface(Art.font(getContext()));
            p.setTextSize(ph * 0.46f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(text, x, y + ph * 0.16f, p);
        }
    }
}

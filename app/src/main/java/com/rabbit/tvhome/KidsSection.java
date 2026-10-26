package com.rabbit.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Section 3: "Little Adventures", an illustrated scene with four worlds; each opens a YouTube playlist or channel. */
final class KidsSection implements Section {
    private final Context ctx;
    private final Cfg cfg;
    private final Bitmap scene;
    private final View view;
    private int sel = 0;

    KidsSection(Context c, Cfg cfg) {
        this.ctx = c;
        this.cfg = cfg;
        scene = Art.load(c, cfg.kidsScene, 1920);
        view = new KidsView(c);
    }

    public View view() { return view; }

    public boolean onDir(int dir) {
        Cfg.World cur = cfg.worlds.get(sel);
        float cx = cur.x + cur.w / 2f, cy = cur.y + cur.h / 2f;
        int best = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < cfg.worlds.size(); i++) {
            if (i == sel) continue;
            Cfg.World w = cfg.worlds.get(i);
            float dx = w.x + w.w / 2f - cx, dy = w.y + w.h / 2f - cy;
            float along = dir == LEFT ? -dx : dir == RIGHT ? dx : dir == UP ? -dy : dy;
            float across = (dir == LEFT || dir == RIGHT) ? Math.abs(dy) : Math.abs(dx);
            if (along <= 0.01f) continue;
            float score = along + 2f * across;
            if (score < bestScore) { bestScore = score; best = i; }
        }
        if (best >= 0) {
            sel = best;
            view.invalidate();
            return true;
        }
        return dir != UP; // nothing above: Home scrolls back to Favourites
    }

    public boolean onBack() { return false; }

    public void onOk() { Launch.link(ctx, cfg.worlds.get(sel).link); }

    public void onEnter(boolean fromAbove) { view.invalidate(); }

    public void onLeave() { }

    private final class KidsView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        KidsView(Context c) {
            super(c);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            c.drawColor(Color.rgb(20, 28, 60));
            if (scene != null) {
                r.set(0, 0, w, h);
                p.setFilterBitmap(true);
                c.drawBitmap(scene, null, r, p);
            }
            for (int i = 0; i < cfg.worlds.size(); i++) {
                Cfg.World k = cfg.worlds.get(i);
                r.set(k.x * w, k.y * h, (k.x + k.w) * w, (k.y + k.h) * h);
                boolean on = i == sel;
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(on ? 6f : 0f);
                if (on) {
                    p.setColor(Color.WHITE);
                    p.setShadowLayer(40f, 0, 0, Color.rgb(255, 220, 120));
                    c.drawRoundRect(r, 36f, 36f, p);
                    p.clearShadowLayer();
                }
            }

            // info card for the focused world
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(Art.font(getContext()));
            p.setTextAlign(Paint.Align.CENTER);
            Cfg.World k = cfg.worlds.get(sel);
            float cw = w * 0.34f, ch = h * 0.11f, cl = (w - cw) / 2f, ct = h * 0.87f;
            r.set(cl, ct, cl + cw, ct + ch);
            p.setColor(Color.argb(190, 10, 14, 30));
            c.drawRoundRect(r, 28f, 28f, p);
            p.setColor(Color.WHITE);
            p.setTextSize(ch * 0.34f);
            c.drawText(k.title, r.centerX(), r.top + ch * 0.45f, p);
            p.setColor(Color.argb(200, 255, 255, 255));
            p.setTextSize(ch * 0.24f);
            c.drawText(k.desc, r.centerX(), r.top + ch * 0.8f, p);
        }
    }
}

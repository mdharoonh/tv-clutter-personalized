package com.rabbit.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Section 2: the favourites as a character line-up. Everyone is black and white; the selected one turns to colour
 * and steps forward (front row) or grows behind the front row (back row), a glow in their colour lights the group
 * and a dark black-and-white scene from their title fades in behind.
 */
final class FavSection implements Section {
    private static final ColorMatrixColorFilter GRAY;
    private static final ColorMatrixColorFilter SCENE;
    static {
        ColorMatrix g = new ColorMatrix();
        g.setSaturation(0);
        GRAY = new ColorMatrixColorFilter(g);
        ColorMatrix s = new ColorMatrix();
        s.setSaturation(0);
        ColorMatrix dim = new ColorMatrix(new float[]{
                .45f, 0, 0, 0, 0,
                0, .45f, 0, 0, 0,
                0, 0, .45f, 0, 0,
                0, 0, 0, 1, 0});
        s.postConcat(dim);
        SCENE = new ColorMatrixColorFilter(s);
    }

    private final Context ctx;
    private final Cfg cfg;
    private final Root root;
    private final List<ImageView> chars = new ArrayList<>();
    private final List<ImageView> scenes = new ArrayList<>();
    private final View glow;
    private final View legs;
    private final TextView title;
    private final ImageView appLogo;
    private final java.util.Map<String, Bitmap> appLogos = new java.util.HashMap<>();
    private final GradientDrawable glowShape = new GradientDrawable();
    private int sel = 0;

    FavSection(Context c, Cfg cfg) {
        this.ctx = c;
        this.cfg = cfg;
        root = new Root(c);
        root.setBackgroundColor(Color.rgb(6, 8, 14));

        // scenes first (behind everything), all preloaded so a switch is just an alpha change
        for (Cfg.Fav f : cfg.favs) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setColorFilter(SCENE);
            Bitmap b = Art.load(c, f.scene, 1280);
            if (b != null) iv.setImageBitmap(b);
            iv.setAlpha(0f);
            root.addView(iv, match());
            scenes.add(iv);
        }

        glowShape.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glowShape.setShape(GradientDrawable.OVAL);
        glow = new View(c);
        glow.setBackground(glowShape);
        root.addView(glow, match());

        for (Cfg.Fav f : cfg.favs) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            Bitmap b = Art.load(c, f.cutout, 600);
            if (b != null) iv.setImageBitmap(b);
            root.addView(iv, match());
            chars.add(iv);
        }

        legs = new View(c);
        legs.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.rgb(6, 8, 14)}));
        legs.setTranslationZ(100f);
        root.addView(legs, match());

        title = text(c, 46, Color.WHITE);
        title.setTypeface(Art.font(c), android.graphics.Typeface.BOLD);
        title.setShadowLayer(12f, 0, 2, Color.BLACK);
        title.getPaint().setFakeBoldText(true);
        title.setIncludeFontPadding(true);
        title.setPadding(0, 0, 0, 12);
        appLogo = new ImageView(c);
        appLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        for (java.util.Map.Entry<String, Cfg.App> e : cfg.apps.entrySet()) {
            Bitmap lb = Art.load(c, e.getValue().logo, 500);
            if (lb != null) appLogos.put(e.getKey(), lb);
        }
        root.addView(title, match());
        root.addView(appLogo, match());
        root.titleView = title;
        root.appView = appLogo;
        root.legsView = legs;
        root.glowView = glow;
        root.chars = chars;
        root.cfg = cfg;
        root.finish(scenes);
        apply();
    }

    private static ViewGroup.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private TextView text(Context c, int sp, int color) {
        TextView t = new TextView(c);
        t.setTypeface(Art.font(c));
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine();
        return t;
    }

    public View view() { return root; }

    public boolean onDir(int dir) {
        Cfg.Fav cur = cfg.favs.get(sel);
        if (dir == LEFT || dir == RIGHT) {
            int best = -1;
            float bestDx = Float.MAX_VALUE;
            for (int i = 0; i < cfg.favs.size(); i++) {
                Cfg.Fav f = cfg.favs.get(i);
                if (f.front != cur.front || i == sel) continue;
                float dx = center(f) - center(cur);
                if ((dir == RIGHT ? dx : -dx) > 0 && Math.abs(dx) < bestDx) {
                    bestDx = Math.abs(dx);
                    best = i;
                }
            }
            if (best >= 0) { sel = best; apply(); }
            return true;
        }
        // Up from the front row goes to the back row, Down from the back row to the front row; otherwise Home scrolls
        boolean wantFront = dir == DOWN;
        if (cur.front == wantFront) return false;
        int best = -1;
        float bestDx = Float.MAX_VALUE;
        for (int i = 0; i < cfg.favs.size(); i++) {
            Cfg.Fav f = cfg.favs.get(i);
            if (f.front != wantFront) continue;
            float dx = Math.abs(center(f) - center(cur));
            if (dx < bestDx) { bestDx = dx; best = i; }
        }
        if (best < 0) return false;
        sel = best;
        apply();
        return true;
    }

    private static float center(Cfg.Fav f) { return f.x + f.w / 2f; }

    public boolean onBack() { return false; }

    public void onOk() {
        Cfg.Fav f = cfg.favs.get(sel);
        Launch.app(ctx, cfg.apps.get(f.app), f.link);
    }

    public void onEnter(boolean fromAbove) {
        // coming from Kids lands on the front row; from Home on the back row's nearest, but keep the last pick either way
        apply();
    }

    public void onLeave() {
        for (ImageView s : scenes) s.animate().alpha(0f).setDuration(250).start();
    }

    private void apply() {
        Cfg.Fav cur = cfg.favs.get(sel);
        for (int i = 0; i < chars.size(); i++) {
            ImageView iv = chars.get(i);
            Cfg.Fav f = cfg.favs.get(i);
            boolean on = i == sel;
            iv.setColorFilter(on ? null : GRAY);
            iv.setAlpha(on ? 1f : 0.7f);
            iv.setPivotX(iv.getWidth() / 2f);
            iv.setPivotY(iv.getHeight());
            float scale = on ? 1.12f : 1f;
            iv.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
            // front row always above back row; selected steps in front of its own row only
            iv.setTranslationZ((f.front ? 20f : 0f) + (on ? 5f : 0f));
            scenes.get(i).animate().alpha(on ? 1f : 0f).setDuration(350).start();
        }
        glowShape.setColors(new int[]{Color.argb(150, Color.red(cur.color), Color.green(cur.color), Color.blue(cur.color)),
                Color.TRANSPARENT});
        title.setText(cur.title);
        Bitmap lb = appLogos.get(cur.app);
        if (lb != null) appLogo.setImageBitmap(lb);
        else appLogo.setImageDrawable(null);
        root.requestLayout();
    }

    /** Places the scenes full-screen and the collage children by the config fractions. */
    private final class Root extends FrameLayout {
        TextView titleView;
        View appView;
        View legsView, glowView;
        List<ImageView> chars;
        Cfg cfg;
        private List<ImageView> sceneViews;

        Root(Context c) { super(c); }

        void finish(List<ImageView> s) { sceneViews = s; }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int w = r - l, h = b - t;
            for (ImageView s : sceneViews) place(s, 0, 0, w, h);

            int cw = (int) (w * 0.78f);
            int ch = (int) (cw * cfg.favAspect);
            int cl = (w - cw) / 2;
            int ct = (int) (h * 0.9f) - ch;
            for (int i = 0; i < chars.size(); i++) {
                Cfg.Fav f = cfg.favs.get(i);
                ImageView iv = chars.get(i);
                place(iv, cl + (int) (f.x * cw), ct + (int) (f.y * ch), (int) (f.w * cw), (int) (f.h * ch));
                iv.setPivotX(iv.getWidth() / 2f);
                iv.setPivotY(iv.getHeight());
            }
            place(glowView, cl - cw / 8, ct - ch / 4, cw + cw / 4, ch + ch / 2);
            place(legsView, 0, ct + (int) (ch * 0.75f), w, h - (ct + (int) (ch * 0.75f)));
            place(titleView, 0, (int) (h * 0.05f), w, (int) (h * 0.105f));
            place(appView, (w - (int) (h * 0.2f)) / 2, (int) (h * 0.155f), (int) (h * 0.2f), (int) (h * 0.065f));
        }

        private void place(View v, int x, int y, int w, int h) {
            v.measure(MeasureSpec.makeMeasureSpec(Math.max(w, 0), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.max(h, 0), MeasureSpec.EXACTLY));
            v.layout(x, y, x + w, y + h);
        }
    }
}

package com.rabbit.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;

import java.io.InputStream;

/** Loads the art in assets/, sampled down so the TV's small RAM is not spent on pixels the screen cannot show. */
final class Art {
    private static Typeface font;

    /** Decodes assets/path at no more than about maxW pixels wide; null when the file is missing. */
    static Bitmap load(Context c, String path, int maxW) {
        if (path == null || path.isEmpty()) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getAssets().open(path);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (o.outWidth / (s * 2) >= maxW) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            in = c.getAssets().open(path);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    static Typeface font(Context c) {
        if (font == null) {
            try {
                font = Typeface.createFromAsset(c.getAssets(), "fonts/outfit.ttf");
            } catch (RuntimeException e) {
                font = Typeface.DEFAULT;
            }
        }
        return font;
    }
}

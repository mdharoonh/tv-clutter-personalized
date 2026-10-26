package com.rabbit.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** The launcher: three sections stacked vertically, one visible at a time, scrolled by Up/Down at their edges. */
public class HomeActivity extends Activity {
    private Section[] sections;
    private FrameLayout track;
    private int page = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(Color.BLACK);
        setContentView(host);
        try {
            Cfg cfg = Cfg.load(this);
            sections = new Section[]{new RadialSection(this, cfg), new FavSection(this, cfg), new KidsSection(this, cfg)};
        } catch (Exception e) {
            TextView t = new TextView(this);
            t.setTextColor(Color.WHITE);
            t.setTextSize(24);
            t.setPadding(80, 80, 80, 80);
            t.setText("Home could not start:\n" + e);
            host.addView(t);
            return;
        }
        track = new FrameLayout(this);
        host.addView(track, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (Section s : sections) {
            track.addView(s.view(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        // sections are full-screen pages placed one screen apart; scrolling moves the whole stack
        track.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> position(false));
        sections[0].onEnter(false);
    }

    private void position(boolean animate) {
        int h = track.getHeight();
        for (int i = 0; i < sections.length; i++) {
            View v = sections[i].view();
            float y = (i - page) * h;
            if (animate) v.animate().translationY(y).setDuration(350).start();
            else v.setTranslationY(y);
        }
    }

    private void go(int to, boolean fromAbove) {
        if (to < 0 || to >= sections.length || to == page) return;
        sections[page].onLeave();
        page = to;
        position(true);
        sections[page].onEnter(fromAbove);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (sections == null) return super.onKeyDown(code, e);
        int dir = -1;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: dir = Section.UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: dir = Section.DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: dir = Section.LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: dir = Section.RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (e.getRepeatCount() == 0) sections[page].onOk();
                return true;
            case KeyEvent.KEYCODE_BACK:
                back();
                return true; // Home is the launcher: Back never leaves it by itself
            case KeyEvent.KEYCODE_HOME:
                goHome();
                return true;
            default:
                return super.onKeyDown(code, e);
        }
        if (!sections[page].onDir(dir)) {
            if (dir == Section.DOWN) go(page + 1, true);
            else if (dir == Section.UP) go(page - 1, false);
        }
        return true;
    }

    /** Back: the selected option first, then back to the first section. Back never leaves the launcher. */
    private void back() {
        if (page != 0) go(0, false);
        else sections[0].onBack();
    }

    private void goHome() {
        if (page == 0) sections[0].onEnter(false);
        else go(0, false);
    }

    @Override
    protected void onNewIntent(android.content.Intent i) {
        super.onNewIntent(i);
        if (sections != null) goHome(); // Home pressed while already here
    }
}

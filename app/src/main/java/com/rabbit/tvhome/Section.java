package com.rabbit.tvhome;

import android.view.View;

/** One of the three stacked parts of Home. The activity routes remote keys to the visible one. */
interface Section {
    int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;

    View view();

    /** A D-pad press. Return false when the section has nothing more in that direction, so Home scrolls to the neighbour. */
    boolean onDir(int dir);

    void onOk();

    /** Back pressed. Return true when the section used it (for example to return to its resting state). */
    boolean onBack();

    /** Scrolled into view (fromAbove: arrived by pressing Down). */
    void onEnter(boolean fromAbove);

    /** Scrolled out of view. */
    void onLeave();
}

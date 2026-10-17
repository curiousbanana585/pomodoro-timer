package com.bouncy.loaf;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** A booked moment has come, or a button on the notice that doesn't need LOAF open was pressed. */
public class Alarms extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String a = in.getAction();
        if (a == null) return;
        int i = in.getIntExtra("i", -1);
        switch (a) {
            case Notices.ACT_STEP: Notices.step(c, i); break;
            case Notices.ACT_POP: Notices.pop(c, i); break;
            case Notices.ACT_SNOOZE: Notices.snooze(c); break;
            case Notices.ACT_SNOOZE_END: Notices.snoozeEnd(c); break;
            case Notices.ACT_REPOST: Notices.showCurrent(c, false); break;          // swiped away: put it back, quietly
            case Notices.ACT_GOT_IT: c.getSystemService(NotificationManager.class).cancel(Notices.POP); break;
            default: break;
        }
    }
}

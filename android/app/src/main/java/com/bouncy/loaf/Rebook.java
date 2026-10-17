package com.bouncy.loaf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms don't survive a restart or an app update, so book the day's moments again. */
public class Rebook extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) { Notices.apply(c); }
}

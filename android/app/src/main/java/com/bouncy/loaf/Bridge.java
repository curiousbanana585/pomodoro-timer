package com.bouncy.loaf;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** window.LoafAndroid in the page. LOAF finds it and hands over its upcoming notices; in a browser it isn't there. */
final class Bridge {
    private final MainActivity act;
    private final Context c;

    Bridge(MainActivity act) { this.act = act; this.c = act.getApplicationContext(); }

    /** The day's moments, as LOAF worked them out. Replaces whatever was booked before. */
    @JavascriptInterface
    public void schedule(String json) {
        try { new JSONObject(json); } catch (JSONException e) { return; }
        Notices.prefs(c).edit().putString("plan", json).apply();
        Notices.clearSnooze(c);
        Notices.apply(c);
    }

    /** Buttons pressed on the notice since LOAF last asked: [{"a":"start","at":...}] */
    @JavascriptInterface
    public String takeEvents() { return takeEvents(c); }

    @JavascriptInterface
    public String permission() {
        boolean on = c.getSystemService(NotificationManager.class).areNotificationsEnabled();
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return Notices.prefs(c).getBoolean("asked", false) && !act.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ? "denied" : "default";
        return on ? "granted" : "denied";
    }

    @JavascriptInterface
    public void askPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        Notices.prefs(c).edit().putBoolean("asked", true).apply();
        act.runOnUiThread(new Runnable() { public void run() { act.requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1); } });
    }

    // ---------- the button queue ----------
    static synchronized void addEvent(Context c, String a) {
        SharedPreferences p = Notices.prefs(c);
        JSONArray L;
        try { L = new JSONArray(p.getString("events", "[]")); } catch (JSONException e) { L = new JSONArray(); }
        try { L.put(new JSONObject().put("a", a).put("at", System.currentTimeMillis())); } catch (JSONException e) { return; }
        p.edit().putString("events", L.toString()).apply();
    }

    static synchronized String takeEvents(Context c) {
        SharedPreferences p = Notices.prefs(c);
        String s = p.getString("events", "[]");
        p.edit().putString("events", "[]").apply();
        return s;
    }
}

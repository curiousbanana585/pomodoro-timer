package com.bouncy.loaf;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The phone's half of LOAF's notices. LOAF (the web page) works out the day's moments and hands them over as JSON:
 * steps = the one live notice as it should look from each moment on (and whether it rings then), pops = meeting heads-ups.
 * Each moment is booked as an alarm, so it arrives on time with LOAF closed and the screen locked.
 */
final class Notices {
    static final int LIVE = 1, POP = 2;
    static final String ACT_STEP = "com.bouncy.loaf.STEP", ACT_POP = "com.bouncy.loaf.POP", ACT_SNOOZE = "com.bouncy.loaf.SNOOZE",
            ACT_SNOOZE_END = "com.bouncy.loaf.SNOOZE_END", ACT_REPOST = "com.bouncy.loaf.REPOST", ACT_GOT_IT = "com.bouncy.loaf.GOT_IT";
    static final long SNOOZE_MS = 5 * 60000L;
    private static final int POP_CODE = 1000, SNOOZE_CODE = 2000;
    private static final int ACCENT = 0xFFD9442B;

    private Notices() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("loaf", Context.MODE_PRIVATE); }

    static JSONObject plan(Context c) {
        try { return new JSONObject(prefs(c).getString("plan", "{}")); } catch (JSONException e) { return new JSONObject(); }
    }

    // ---------- the notice categories Haz can tune in Android settings ----------
    static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        NotificationChannel live = new NotificationChannel("live", "Live countdown", NotificationManager.IMPORTANCE_LOW);
        live.setDescription("The timer in your notices. Silent; it changes at each moment");
        live.setShowBadge(false);
        nm.createNotificationChannel(live);
        String[][] loud = {
            { "bell", "Bells", "End of each focus, break and timebox", "4" },
            { "meet", "Meetings", "Heads up before, and when one runs over", "4" },
            { "nudge", "Nudges", "Lumi's \"still waiting\" reminders", "3" },
            { "recap", "Day recap", "A quiet summary at the finish line", "2" },
        };
        for (String[] x : loud) {
            NotificationChannel ch = new NotificationChannel(x[0], x[1], Integer.parseInt(x[3]));
            ch.setDescription(x[2]);
            if (!"recap".equals(x[0])) { ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), aa); ch.enableVibration(true); }
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }

    // ---------- book the moments LOAF sent ----------
    static synchronized void apply(Context c) {
        channels(c);
        SharedPreferences p = prefs(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        int oldSteps = p.getInt("nSteps", 0), oldPops = p.getInt("nPops", 0);
        for (int i = 0; i < oldSteps; i++) am.cancel(alarm(c, ACT_STEP, i, i));
        for (int i = 0; i < oldPops; i++) am.cancel(alarm(c, ACT_POP, POP_CODE + i, i));
        JSONObject plan = plan(c);
        JSONArray steps = plan.optJSONArray("steps"), pops = plan.optJSONArray("pops");
        int ns = steps == null ? 0 : steps.length(), np = pops == null ? 0 : pops.length();
        p.edit().putInt("nSteps", ns).putInt("nPops", np).apply();
        long now = System.currentTimeMillis();
        for (int i = 0; i < ns; i++) {
            JSONObject s = steps.optJSONObject(i);
            if (s != null && s.optLong("at") > now) book(c, am, s.optLong("at"), alarm(c, ACT_STEP, i, i), rings(c, s));
        }
        for (int i = 0; i < np; i++) {
            JSONObject s = pops.optJSONObject(i);
            if (s != null && s.optLong("at") > now) book(c, am, s.optLong("at"), alarm(c, ACT_POP, POP_CODE + i, i), true);
        }
        showCurrent(c, false);
    }

    // the step that applies now: the latest one that has started
    static int current(JSONArray steps, long now) {
        int k = -1;
        for (int i = 0; steps != null && i < steps.length(); i++) { JSONObject s = steps.optJSONObject(i); if (s != null && s.optLong("at") <= now) k = i; }
        return k;
    }

    static void showCurrent(Context c, boolean ring) {
        JSONArray steps = plan(c).optJSONArray("steps");
        int k = current(steps, System.currentTimeMillis());
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (k < 0) {
            if (steps == null || steps.length() == 0) nm.cancel(LIVE);   // nothing on: no notice
            return;
        }
        post(c, steps.optJSONObject(k), ring);
    }

    static boolean rings(Context c, JSONObject s) {
        return !s.optString("ring").isEmpty() && plan(c).optBoolean("sound", true);
    }

    /** An alarm went off for step i: show it, ringing if LOAF would have rung, unless LOAF is on screen or Haz pressed Snooze. */
    static void step(Context c, int i) {
        JSONArray steps = plan(c).optJSONArray("steps");
        if (steps == null || i >= steps.length()) return;
        if (current(steps, System.currentTimeMillis() + 1000) != i) return;   // a later moment has already taken over
        JSONObject s = steps.optJSONObject(i);
        boolean quiet = System.currentTimeMillis() < prefs(c).getLong("quietUntil", 0);
        post(c, s, rings(c, s) && !quiet && !MainActivity.visible);
    }

    static void pop(Context c, int i) {
        JSONObject plan = plan(c);
        JSONArray pops = plan.optJSONArray("pops");
        if (pops == null || i >= pops.length()) return;
        JSONObject s = pops.optJSONObject(i);
        boolean ring = plan.optBoolean("sound", true) && !MainActivity.visible;
        Notification.Builder b = base(c, ring ? "meet" : "live", s.optString("title"), s.optString("text"))
                .setAutoCancel(true).setOnlyAlertOnce(!ring).setCategory(Notification.CATEGORY_REMINDER).setShowWhen(true).setWhen(System.currentTimeMillis())
                .addAction(new Notification.Action.Builder(null, "Got it", broadcast(c, ACT_GOT_IT, 3000)).build());
        c.getSystemService(NotificationManager.class).notify(POP, b.build());
    }

    /** Snooze: no ringing for 5 minutes, then the notice rings again if LOAF is still waiting. */
    static void snooze(Context c) {
        long until = System.currentTimeMillis() + SNOOZE_MS;
        prefs(c).edit().putLong("quietUntil", until).apply();
        book(c, c.getSystemService(AlarmManager.class), until, alarm(c, ACT_SNOOZE_END, SNOOZE_CODE, 0), true);
        showCurrent(c, false);
    }

    static void snoozeEnd(Context c) {
        JSONArray steps = plan(c).optJSONArray("steps");
        int k = current(steps, System.currentTimeMillis());
        if (k < 0) return;
        JSONObject s = steps.optJSONObject(k);
        boolean still = s.optJSONArray("acts") != null && s.optJSONArray("acts").toString().contains("\"snooze\"");
        boolean sound = plan(c).optBoolean("sound", true);
        if (!still) { post(c, s, false); return; }
        try { if (s.optString("ring").isEmpty()) s = new JSONObject(s.toString()).put("ring", "nudge"); } catch (JSONException e) { /* keep it as it is */ }
        post(c, s, sound && !MainActivity.visible);
    }

    /** LOAF sent a new list: Haz has been in the app, so any snooze is over. */
    static void clearSnooze(Context c) {
        prefs(c).edit().remove("quietUntil").apply();
        c.getSystemService(AlarmManager.class).cancel(alarm(c, ACT_SNOOZE_END, SNOOZE_CODE, 0));
    }

    // ---------- drawing the notice ----------
    private static void post(Context c, JSONObject s, boolean ring) {
        if (s == null) return;
        boolean fin = s.optBoolean("fin");
        String ch = ring ? s.optString("ring") : "live";
        if (!ring && fin) ch = "recap";
        String text = s.optString("text"), sub = s.optString("sub");
        Notification.Builder b = base(c, ch, s.optString("title"), text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setOnlyAlertOnce(!ring)
                .setOngoing(!fin)
                .setAutoCancel(fin)
                .setCategory(ring ? Notification.CATEGORY_REMINDER : Notification.CATEGORY_PROGRESS)
                .setColor(tint(s.optString("tk")));
        if (!sub.isEmpty()) b.setSubText(sub);
        long clock = s.optLong("clock");
        if (clock > 0 && !fin) {
            b.setWhen(clock).setShowWhen(true).setUsesChronometer(true);     // Android ticks the clock itself, with LOAF closed
            if (Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(s.optBoolean("down"));
        } else b.setShowWhen(false);
        if (!fin) b.setDeleteIntent(broadcast(c, ACT_REPOST, 3001));        // swiped away: it comes straight back
        JSONArray acts = s.optJSONArray("acts");
        for (int i = 0; acts != null && i < acts.length() && i < 3; i++) {
            String a = acts.optString(i);
            if ("snooze".equals(a)) b.addAction(new Notification.Action.Builder(null, "Snooze 5 min", broadcast(c, ACT_SNOOZE, 3002)).build());
            else b.addAction(new Notification.Action.Builder(null, label(a, s), open(c, a)).build());
        }
        if (Build.VERSION.SDK_INT >= 36 && !fin) {                           // Android 16 Live Updates: a countdown chip in the status bar
            Bundle x = new Bundle();
            x.putBoolean("android.requestPromotedOngoing", true);
            b.addExtras(x);
        }
        c.getSystemService(NotificationManager.class).notify(LIVE, b.build());
    }

    private static String label(String a, JSONObject s) {
        String t = s.optString("title");
        switch (a) {
            case "start":
                if (t.startsWith("Focus done") || t.startsWith("Set done")) return t.startsWith("Set") ? "Start sofa time" : "Start break";
                if (t.startsWith("Break's over") && !s.optString("text").contains("carries on")) return "Start focus";
                return "Start";
            case "ended": return "Ended";
            case "pause": return "Pause";
            case "resume": return "Carry on";
            default: return "Open LOAF";
        }
    }

    /** The timer screen's colour for what's running: icon, app name and buttons take it (a plain notice can't fill with colour). */
    private static int tint(String tk) {
        switch (tk) {
            case "brk": return 0xFF1F8A4C;
            case "box": return 0xFF2B6A8E;
            case "meet": return 0xFF8A5AAE;
            case "paused": return 0xFFB8892A;
            default: return ACCENT;
        }
    }

    private static Notification.Builder base(Context c, String channel, String title, String text) {
        return new Notification.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_stat_lumi)
                .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.mipmap.ic_launcher))
                .setColor(ACCENT)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(open(c, null));
    }

    // ---------- intents ----------
    private static PendingIntent open(Context c, String act) {
        Intent i = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (act != null) i.putExtra("act", act);
        int code = act == null ? 100 : "start".equals(act) ? 101 : "ended".equals(act) ? 102 : "pause".equals(act) ? 103 : "resume".equals(act) ? 104 : 105;
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent broadcast(Context c, String action, int code) {
        Intent i = new Intent(c, Alarms.class).setAction(action);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent alarm(Context c, String action, int code, int index) {
        Intent i = new Intent(c, Alarms.class).setAction(action).putExtra("i", index);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Ringing moments use the alarm-clock kind of alarm, which Android never holds back. Quiet ones use a plain exact alarm. */
    private static void book(Context c, AlarmManager am, long at, PendingIntent pi, boolean loud) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); return; }
            if (loud) am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, open(c, null)), pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);            // no exact alarms allowed: close enough
        }
    }
}

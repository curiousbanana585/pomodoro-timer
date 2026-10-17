package com.bouncy.loaf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** A thin shell: it opens LOAF (the one and only copy, on GitHub Pages) full screen, and lends it the phone's notices. */
public class MainActivity extends Activity {
    static volatile boolean visible = false;          // LOAF is on screen: it plays its own chime, so the notices stay quiet
    private WebView web;
    private FrameLayout root;
    private int lastBg = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String url;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        url = getString(R.string.app_url);
        Notices.channels(this);
        takeAct(getIntent());

        getWindow().setStatusBarColor(0xFF0E1116);
        getWindow().setNavigationBarColor(0xFF0E1116);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0E1116);
        // The page sits inside a frame that keeps it clear of the status and navigation bars. (Padding the WebView itself
        // doesn't move the page, it only makes it scroll.) The frame takes LOAF's background colour, so the bars blend in.
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF5B1A12);
        root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets wi) {
                if (Build.VERSION.SDK_INT >= 30) {
                    Insets i = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                    v.setPadding(i.left, i.top, i.right, i.bottom);
                    return WindowInsets.CONSUMED;               // the page needn't add its own room for the bars too
                }
                v.setPadding(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
                return wi.consumeSystemWindowInsets();
            }
        });
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                       // LOAF keeps the day, the jar and your thoughts here
        s.setMediaPlaybackRequiresUserGesture(false);       // lets the chime play
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.addJavascriptInterface(new Bridge(this), "LoafAndroid");

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl(), home = Uri.parse(url);
                if (home.getHost() != null && home.getHost().equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { /* nothing to open it with */ }
                return true;                                // other sites open in the browser
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) retryLater();       // no internet and nothing cached yet
            }
        });
        if (state == null) web.loadUrl(url); else web.restoreState(state);

        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
                @Override public void onBackInvoked() { back(); }
            });
        }
    }

    private void retryLater() {
        web.loadData("<body style='background:#0e1116;color:#fff;font:20px sans-serif;display:flex;align-items:center;justify-content:center;height:90vh;margin:0;text-align:center;padding:24px'>"
                + "Lumi can't reach LOAF yet. Trying again in a moment…</body>", "text/html", "utf-8");
        handler.postDelayed(new Runnable() { public void run() { web.loadUrl(url); } }, 5000);
    }

    /** A button on the notice opened the app: LOAF picks it up within a second. */
    private void takeAct(Intent in) {
        if (in == null) return;
        String a = in.getStringExtra("act");
        if (a != null) { Bridge.addEvent(this, a); in.removeExtra("act"); }
    }

    @Override
    protected void onNewIntent(Intent in) { super.onNewIntent(in); setIntent(in); takeAct(in); }

    private void back() { if (web.canGoBack()) web.goBack(); else moveTaskToBack(true); }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() { back(); }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    /** Colours the bars to match LOAF's background, which changes with each bit of the day. */
    private final Runnable matchBg = new Runnable() {
        public void run() {
            web.evaluateJavascript("getComputedStyle(document.body).backgroundColor", new android.webkit.ValueCallback<String>() {
                @Override public void onReceiveValue(String v) {
                    int c = parseRgb(v);
                    if (c != 0 && c != lastBg) {
                        lastBg = c; root.setBackgroundColor(c);
                        if (Build.VERSION.SDK_INT < 35) { getWindow().setStatusBarColor(c); getWindow().setNavigationBarColor(c); }   // older phones draw their own bars
                    }
                }
            });
            handler.postDelayed(this, 600);
        }
    };
    static int parseRgb(String v) {
        if (v == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*([\\d.]+))?").matcher(v);
        if (!m.find()) return 0;
        if (m.group(4) != null && Float.parseFloat(m.group(4)) < 0.5f) return 0;   // see-through: keep the last colour
        return 0xFF000000 | (Integer.parseInt(m.group(1)) << 16) | (Integer.parseInt(m.group(2)) << 8) | Integer.parseInt(m.group(3));
    }

    @Override protected void onResume() { super.onResume(); visible = true; web.onResume(); handler.removeCallbacks(matchBg); handler.post(matchBg); }
    @Override protected void onPause() { visible = false; handler.removeCallbacks(matchBg); web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { visible = false; handler.removeCallbacksAndMessages(null); web.destroy(); super.onDestroy(); }
}

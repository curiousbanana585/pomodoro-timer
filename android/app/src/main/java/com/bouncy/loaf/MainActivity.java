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
        web.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {   // keep LOAF clear of the status and navigation bars
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets wi) {
                if (Build.VERSION.SDK_INT >= 30) {
                    Insets i = wi.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                    v.setPadding(i.left, i.top, i.right, i.bottom);
                } else {
                    v.setPadding(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
                }
                return wi;
            }
        });
        setContentView(web);

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
    @Override protected void onResume() { super.onResume(); visible = true; web.onResume(); }
    @Override protected void onPause() { visible = false; web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { visible = false; handler.removeCallbacksAndMessages(null); web.destroy(); super.onDestroy(); }
}

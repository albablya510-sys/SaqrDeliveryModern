package com.saqr.delivery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private WebView webView;
    private final List<WebView> printWebViews = new ArrayList<>();

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new PrintBridge(this), "AndroidPrint");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class PrintBridge {
        private final Activity activity;
        PrintBridge(Activity activity) { this.activity = activity; }

        @JavascriptInterface
        public void printHtml(final String html, final String title) {
            activity.runOnUiThread(() -> {
                final WebView printWebView = new WebView(activity);
                WebSettings ps = printWebView.getSettings();
                ps.setJavaScriptEnabled(false);
                ps.setDomStorageEnabled(false);
                printWebViews.add(printWebView);
                printWebView.setWebViewClient(new WebViewClient() {
                    private boolean printed = false;
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if (printed) return;
                        printed = true;
                        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        String jobName = (title == null || title.trim().isEmpty()) ? "تقرير الصقر" : title;
                        PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                        PrintAttributes attrs = new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build();
                        printManager.print(jobName, adapter, attrs);
                    }
                });
                printWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidPrint");
            webView.destroy();
        }
        for (WebView w : printWebViews) {
            try { w.destroy(); } catch (Exception ignored) {}
        }
        printWebViews.clear();
        super.onDestroy();
    }
}

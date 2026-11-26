package com.barq.transfer;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.ViewGroup;

public class MainActivity extends Activity {
    private WebView webView;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(16,24,39));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(16,24,39));
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(this), "BarqAndroid");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
    public class Bridge {
        private final Context context;
        Bridge(Context c) { context = c; }
        @JavascriptInterface public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (pm != null) {
                    String job = "barq-receipt-" + System.currentTimeMillis();
                    pm.print(job, webView.createPrintDocumentAdapter(job), new PrintAttributes.Builder().build());
                }
            });
        }
    }
}

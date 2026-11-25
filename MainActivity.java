package com.mme.accounting;

import android.app.Activity;
import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.addJavascriptInterface(new PrintBridge(), "MMEPrinter");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private class PrintBridge {
        @JavascriptInterface
        public void saveBackup(final String json) {
            new Thread(() -> {
                OutputStream out = null;
                try {
                    String filename = "MME-backup-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(new java.util.Date()) + ".json";
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                        values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                        values.put(MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new java.io.IOException("تعذر إنشاء ملف النسخة الاحتياطية");
                        out = getContentResolver().openOutputStream(uri);
                    } else {
                        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                        if (dir == null) throw new java.io.IOException("مجلد التنزيلات غير متاح");
                        out = new FileOutputStream(new File(dir, filename));
                    }
                    if (out == null) throw new java.io.IOException("تعذر فتح ملف النسخة الاحتياطية");
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "تم حفظ النسخة الاحتياطية في مجلد التنزيلات", Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "فشل تصدير النسخة الاحتياطية: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } finally {
                    try { if (out != null) out.close(); } catch (Exception ignored) {}
                }
            }).start();
        }
        @JavascriptInterface
        public void printHtml(final String html) {
            runOnUiThread(() -> {
                WebView printWeb = new WebView(MainActivity.this);
                printWeb.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        if (manager != null) {
                            manager.print("MME Invoice", view.createPrintDocumentAdapter("MME Invoice"), null);
                        }
                    }
                });
                printWeb.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
            });
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
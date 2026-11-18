package com.box.notes;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;

    /** Called from the page: BoxNative.save(name, mime, base64). Saves PNG/JPG to Pictures/BOX and PDF to Downloads/BOX. */
    class Bridge {
        @JavascriptInterface
        public boolean save(String name, String mime, String b64) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                boolean img = mime != null && mime.startsWith("image/");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH,
                            (img ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS) + "/BOX");
                    Uri col = img ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                  : MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    Uri u = getContentResolver().insert(col, v);
                    if (u == null) return say("Could not save the file");
                    OutputStream os = getContentResolver().openOutputStream(u);
                    os.write(data);
                    os.close();
                } else {
                    if (Build.VERSION.SDK_INT >= 23
                            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
                        return say("Allow storage access, then press Save again");
                    }
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            img ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS), "BOX");
                    dir.mkdirs();
                    File f = new File(dir, name);
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(data);
                    fo.close();
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                }
                return true;
            } catch (Exception e) {
                return say("Save failed: " + e.getMessage());
            }
        }

        private boolean say(final String m) {
            runOnUiThread(new Runnable() {
                public void run() { Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show(); }
            });
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#E9D3EA"));
        getWindow().setNavigationBarColor(Color.parseColor("#1B1620"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#1B1620"));
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);   // GPU-composited = smoother animations
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // keeps your notes (localStorage)
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);

        web.addJavascriptInterface(new Bridge(), "BoxNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Let the app close sheets / go back a screen first; exit only from Home.
        web.evaluateJavascript(
            "(function(){return window.__boxBack?window.__boxBack():false})()",
            value -> { if (!"true".equals(value)) finish(); });
    }

    @Override
    protected void onPause() {
        web.evaluateJavascript("window.dispatchEvent(new Event('pagehide'))", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }
}

package com.quanganhaudio.baohanh;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * App Quang Anh MT: mở phần mềm bảo hành (bản web) trong khung app,
 * cho phép camera (quét seri), định vị (chấm công), chọn ảnh, lưu file Excel / PDF.
 */
public class MainActivity extends Activity {
    static final String APP_URL = "https://janieslieu-source.github.io/QUANGANHAUDIO/";
    static final int REQ_PERMS = 1, REQ_FILE = 2;

    WebView web;
    ValueCallback<Uri[]> fileCb;
    PermissionRequest pendingWebPerm;
    final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Android 7–9: cho phép chia sẻ file đã lưu trong Tải xuống
        android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder().build());
        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor("#1d4ed8"));
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " QuangAnhMT-Android/1.0");

        web.addJavascriptInterface(new Bridge(), "ReactNativeWebView");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String h = u.getHost() == null ? "" : u.getHost();
                if (h.endsWith("github.io") || h.endsWith("firebaseapp.com") || h.endsWith("googleapis.com") || h.endsWith("gstatic.com")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }   // link ngoài (Drive, Zalo, bản đồ…) mở bằng app khác
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }
            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                ui.post(new Runnable() { public void run() {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) req.grant(req.getResources());
                    else { pendingWebPerm = req; requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_PERMS); }
                }});
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { startActivityForResult(p.createIntent(), REQ_FILE); }
                catch (Exception e) { fileCb = null; toast("Không mở được trình chọn file"); return false; }
                return true;
            }
        });

        askPerms();
        if (b != null) web.restoreState(b); else web.loadUrl(APP_URL);
    }

    void askPerms() {
        ArrayList<String> need = new ArrayList<String>();
        String[] all = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        for (String p : all) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), REQ_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (pendingWebPerm != null) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) pendingWebPerm.grant(pendingWebPerm.getResources());
            else pendingWebPerm.deny();
            pendingWebPerm = null;
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code == REQ_FILE && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCb = null;
            return;
        }
        super.onActivityResult(code, result, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    void toast(final String m) {
        ui.post(new Runnable() { public void run() { Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show(); } });
    }

    /** Cầu nối với phần mềm web (giống app cũ): lưu file, gửi thông báo đẩy. */
    class Bridge {
        @JavascriptInterface
        public void postMessage(String json) {
            try {
                JSONObject o = new JSONObject(json);
                String type = o.optString("type");
                if ("file".equals(type)) saveFile(o.optString("name", "file"), o.optString("base64"));
                else if ("push".equals(type)) sendPush(o.optJSONArray("messages"));
            } catch (Exception e) { }
        }
    }

    void sendPush(final JSONArray msgs) {
        if (msgs == null || msgs.length() == 0) return;
        new Thread(new Runnable() { public void run() {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://exp.host/--/api/v2/push/send").openConnection();
                c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                OutputStream os = c.getOutputStream(); os.write(msgs.toString().getBytes("UTF-8")); os.close();
                c.getResponseCode(); c.disconnect();
            } catch (Exception e) { }
        }}).start();
    }

    void saveFile(final String name, final String b64) {
        try {
            byte[] data = Base64.decode(b64.contains(",") ? b64.substring(b64.indexOf(',') + 1) : b64, Base64.DEFAULT);
            String mime = name.endsWith(".pdf") ? "application/pdf" : name.endsWith(".xlsx") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : name.endsWith(".csv") ? "text/csv" : name.endsWith(".json") ? "application/json" : name.endsWith(".png") ? "image/png" : "application/octet-stream";
            Uri uri;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = getContentResolver();
                ContentValues v = new ContentValues();
                v.put("_display_name", name); v.put("mime_type", mime); v.put("relative_path", "Download/QuangAnhMT"); v.put("is_pending", 1);
                uri = cr.insert(Uri.parse("content://media/external/downloads"), v);
                OutputStream os = cr.openOutputStream(uri); os.write(data); os.close();
                ContentValues done = new ContentValues(); done.put("is_pending", 0); cr.update(uri, done, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QuangAnhMT");
                dir.mkdirs();
                File f = new File(dir, name);
                FileOutputStream fo = new FileOutputStream(f); fo.write(data); fo.close();
                uri = Uri.fromFile(f);
            }
            final Uri u = uri; final String m = mime;
            ui.post(new Runnable() { public void run() {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Đã lưu file")
                    .setMessage(name + "\n\nThư mục: Tải xuống / QuangAnhMT")
                    .setPositiveButton("Chia sẻ (Zalo, email…)", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int i) {
                        Intent sh = new Intent(Intent.ACTION_SEND); sh.setType(m); sh.putExtra(Intent.EXTRA_STREAM, u); sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(Intent.createChooser(sh, "Chia sẻ file")); } catch (Exception e) { toast("Không có app để chia sẻ"); }
                    }})
                    .setNegativeButton("Đóng", null).show();
            }});
        } catch (Exception e) { toast("Không lưu được file: " + e.getMessage()); }
    }
}

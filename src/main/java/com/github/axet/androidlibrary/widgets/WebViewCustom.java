package com.github.axet.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.github.axet.androidlibrary.R;
import com.github.axet.androidlibrary.net.HttpClient;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

// Custom WebView with POST/GET interception requests.
//
// https://code.google.com/p/android/issues/detail?id=9122#c21
public class WebViewCustom extends WebView {
    public static final String TAG = WebViewCustom.class.getSimpleName();

    String js;
    String js_post;

    String inject;
    Thread thread;
    Handler handler = new Handler();
    HttpClient http;
    String base;
    DownloadListener listener;

    static void logIO(String url, Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof SocketTimeoutException)
            Log.e(TAG, "load timeout " + url);
        else
            Log.e(TAG, url, e);
    }

    public class Interceptor {
        @JavascriptInterface
        public void customSubmit(String method, String action, String form) {
            Log.d(TAG, "customSubmit()");
            if (method.toUpperCase().equals("POST")) {
                String url = null;
                try {
                    url = new URL(new URL(base), action).toString();
                    Map<String, String> nvps = new HashMap<>();
                    JSONObject j = new JSONObject(form);
                    JSONArray list = (JSONArray) j.get("form");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject o = (JSONObject) list.get(i);
                        String key = o.getString("name");
                        String value = o.getString("value");
                        nvps.put(key, value.toString());
                    }
                    postUrl(url, nvps);
                } catch (Exception e) {
                    logIO(url, e);
                    onConsoleMessage(e.getMessage(), 0, "");
                }
                return;
            }
            // TODO GET
        }

        @JavascriptInterface
        public String customAjax(String method, String action, String user, String password, String body) {
            Log.d(TAG, "customAjax()");
            if (method.toUpperCase().equals("GET")) {
                String url = null;
                try {
                    url = new URL(new URL(base), action).toString();
                    HttpClient.DownloadResponse r = get(url);
                    return r.getHtml();
                } catch (Exception e) {
                    logIO(url, e);
                    onConsoleMessage(e.getMessage(), 0, "");
                }
            }
            // TODO POST
            return "";
        }
    }

    public WebViewCustom(Context context) {
        super(context);
        create();
    }

    public WebViewCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public WebViewCustom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebViewCustom(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        create();
    }

    public WebViewCustom(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        create();
    }

    public void create() {
        try {
            inject = IOUtils.toString(getContext().getResources().openRawResource(R.raw.inject), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getSettings().setSupportMultipleWindows(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(true);

        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                WebViewCustom.this.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return true;
            }

            @Override
            public boolean onConsoleMessage(final ConsoleMessage consoleMessage) {
                onConsoleMessage(consoleMessage.message(), consoleMessage.lineNumber(), consoleMessage.sourceId());
                return true;//super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onConsoleMessage(String msg, int lineNumber, String sourceID) {
                WebViewCustom.this.onConsoleMessage(msg, lineNumber, sourceID);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, final String message, JsResult result) {
                Log.d(TAG, message);
                result.confirm();
                WebViewCustom.this.onJsAlert(view, url, message, result);
                return true;//super.onJsAlert(view, url, message, result);
            }
        });

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (js_post != null) {
                    loadUrl("javascript:" + js_post);
                }
                WebViewCustom.this.onPageFinished(view, url);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                WebViewCustom.this.onReceivedHttpError(view, request, errorResponse);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                String str = error.getDescription().toString();
                Log.d(TAG, str);
                WebViewCustom.this.onReceivedError(view, str, request.getUrl().toString());
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // on M will becalled above method
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.d(TAG, description);
                }
                WebViewCustom.this.onReceivedError(view, description, failingUrl);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (js != null) {
                    loadUrl("javascript:" + js);
                }
                WebViewCustom.this.onPageCommitVisible(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                WebViewCustom.this.onLoadResource(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                WebViewCustom.this.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // let user overide it first
                if (WebViewCustom.this.shouldOverrideUrlLoading(view, url))
                    return true;
                // load page using our code, we may need to inject.
                loadUrl(url);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (http != null) {
                    // ignore POST it comes with no data
                    if (request.getMethod().toUpperCase().equals("GET")) {
                        return getBase(request.getUrl().toString());
                    } else {
                        return super.shouldInterceptRequest(view, request);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (http != null)
                    return getBase(url);
                else
                    return super.shouldInterceptRequest(view, url);
            }
        });

        addJavascriptInterface(new Interceptor(), "interception");
    }

    public void setHttpClient(HttpClient http) {
        this.http = http;
    }

    @Override
    public void loadUrl(final String url) {
        if (url.startsWith("javascript")) {
            super.loadUrl(url);
            return;
        }
        if (url.startsWith("data")) {
            super.loadUrl(url);
            return;
        }

        if (http != null) {
            // make updateCookies() mecanics work
            removeWebCookies();
            base = url;
            request(new Runnable() {
                @Override
                public void run() {
                    load(url, get(url));
                }
            });
        } else {
            super.loadUrl(url);
        }
    }

    HttpClient.DownloadResponse getBase(String url) {
        if (url.startsWith("data")) {
            return null;
        }

        if (http != null) {
            // make updateCookies() mecanics work
            removeWebCookies();
        }

        if (base == null) {
            base = url;
            HttpClient.DownloadResponse w = http.getResponse(base, url);
            w.downloadText();
            if (w.getError() == null && w.isHtml()) {
                w.setHtml(loadBase(w.getHtml()));
            }
            return w;
        } else {
            return get(url);
        }
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        if (http.getRequest() != null) {
            if (thread != null) {
                thread.interrupt();
            }
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    http.abort();
                }
            });
            thread.start();
        }
    }

    void request(Runnable run) {
        thread = new Thread(run, "WebViewCustom");
        thread.start();
    }

    @Override
    public void goBack() {
        super.goBack();
        base = null;
    }

    @Override
    public void goForward() {
        super.goForward();
        base = null;
    }

    @Override
    public void reload() {
        super.reload();
        base = null;
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        if (http != null) {
            // make updateCookies() mecanics work
            removeWebCookies();
            base = url;
            load(url, post(url, postData));
        } else {
            super.postUrl(url, postData);
        }
    }

    public void postUrl(String url, Map<String, String> postData) {
        if (http != null) {
            base = url;
            load(url, post(url, postData));
        } else
            super.postUrl(url, HttpClient.encode(postData).getBytes(Charset.defaultCharset()));
    }

    // Network on main Thread
    public void load(String url, final HttpClient.DownloadResponse r) {
        if (!r.downloaded) {
            listener.onDownloadStart(url, r.userAgent, r.contentDisposition, r.getMimeType(), r.contentLength);
            return;
        }
        try {
            String html = IOUtils.toString(r.getData(), r.getEncoding());
            String hist = url;
            if (r.getError() == null && r.isHtml()) {
                html = loadBase(html);
            } else {
                url = "about:error";
            }

            base = url;

            final String baseUrl = url;
            final String data = html;
            final String history = hist;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    loadDataWithBaseURL(baseUrl, data, r.getMimeType(), r.getEncoding(), history);
                }
            });
        } catch (final IOException e) {
            logIO(url, e);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onConsoleMessage(e.getMessage(), 0, "");
                }
            });
        }
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (base != baseUrl) { // external call
            // all inner calles already set url
            if (http != null) {
                // make updateCookies() mecanics work
                removeWebCookies();
            }
            base = baseUrl;
            data = loadBase(data);
        }
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    String loadBase(String data) {
        Document doc = Jsoup.parse(data);
        Element head = doc.getElementsByTag("head").first();
        if (head != null) {
            head.prepend(inject);
        }
        return doc.outerHtml();
    }

    @Override
    public void setDownloadListener(DownloadListener listener) {
        super.setDownloadListener(listener);
        this.listener = listener;
    }

    public static boolean is_adv(String url) {
        String[] adv_hosts = {"marketgid.com", "adriver.ru", "thisclick.network", "hghit.com",
                "onedmp.com", "acint.net", "yadro.ru", "tovarro.com", "marketgid.com", "rtb.com", "adx1.com",
                "directadvert.ru", "rambler.ru", "alltheladyz.xyz", "ofapes.com", "bongacams.com", "scund.com"};
        String[] adv_paths = {"brand", "iframe"};
        Uri u = Uri.parse(url);
        String host = u.getHost();
        for (String item : adv_hosts) {
            if (host != null && host.contains(item)) {
                return true;
            }
        }
        if (host != null && host.contains("rutracker.org")) {
            String path = u.getPath();
            for (String item : adv_paths) {
                if (path.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    // javascript can add cookies. update every new request;
    void updateCookies(String url) {
        CookieManager inst = CookieManager.getInstance();

        if (Build.VERSION.SDK_INT < 21) {
            CookieSyncManager.getInstance().sync();
        } else {
            inst.flush();
        }

        String cookies = inst.getCookie(url);
        if (cookies != null && !cookies.isEmpty())
            http.addCookies(url, cookies);
    }

    public HttpClient.DownloadResponse get(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onLoadResource(WebViewCustom.this, url);
            }
        });

        updateCookies(url);

        try {
            HttpClient.DownloadResponse w = http.getResponse(base, url);
            w.downloadText();
            return w;
        } catch (final RuntimeException e) {
            logIO(url, e);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onReceivedError(WebViewCustom.this, e.getMessage(), url);
                }
            });
            return new HttpClient.HttpError(e);
        }
    }

    public HttpClient.DownloadResponse post(String url, byte[] postData) {
        // TODO postData -> array
        return post(url, (Map<String, String>) null);
    }

    public HttpClient.DownloadResponse post(String url, Map<String, String> postData) {
        updateCookies(url);
        HttpClient.DownloadResponse w = http.postResponse(base, url, postData);
        w.downloadText();
        return w;
    }

    public void onProgressChanged(WebView view, int newProgress) {
    }

    public void onConsoleMessage(String msg, int lineNumber, String sourceID) {
        Log.d(TAG, msg);
    }

    public void onJsAlert(WebView view, String url, final String message, JsResult result) {
    }

    public void onPageFinished(WebView view, String url) {
    }

    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
    }

    public void onPageCommitVisible(WebView view, String url) {
    }

    public void onReceivedError(WebView view, String message, String url) {
    }

    public void onLoadResource(WebView view, String url) {
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    public void setInject(String js) {
        this.js = js;
    }

    public void setInjectPost(String js) {
        this.js_post = js;
    }

    // not working. use removeAllCookies() then add ones you need.
    public void clearCookies(String url) {
        CookieManager inst = CookieManager.getInstance();
        // longer url better, domain only can return null
        String cookies = inst.getCookie(url);

        Uri uri = Uri.parse(url);
        String domain = uri.getAuthority();

        if (cookies != null) {
            // we need to set expires, otherwise WebView will keep deleted cookies forever ("name=")
            String expires = "expires=Thu, 01 Jan 1970 03:00:00 GMT"; // # date -r 0 +%a,\ %d\ %b\ %Y\ %H:%M:%S\ GMT

            SimpleDateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            rfc1123.setTimeZone(TimeZone.getTimeZone("GMT"));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 1);
            expires = "expires=" + rfc1123.format(cal.getTime());

            if (Build.VERSION.SDK_INT < 21) {
                CookieSyncManager.createInstance(getContext());
                CookieSyncManager.getInstance().startSync();
            }
            String[] cc = cookies.split(";");
            for (String c : cc) {
                String[] vv = c.split("=");
                for (File f = new File(uri.getPath()); f != null; f = f.getParentFile()) {
                    String p;
                    String path;
                    if (f.equals(new File(File.separator))) {
                        p = "";
                        path = "";
                    } else {
                        p = f.getPath();
                        path = "; path=" + p;
                    }
                    String cookie = vv[0].trim() + "=" + "; domain=" + uri.getAuthority() + path + "; " + expires;
                    String u = new Uri.Builder().scheme("http").authority(domain).path(p).build().toString();
                    inst.setCookie(u, cookie);
                }
            }
            if (Build.VERSION.SDK_INT < 21) {
                CookieSyncManager.getInstance().stopSync();
                CookieSyncManager.getInstance().sync();
                inst.removeSessionCookie();
                inst.removeExpiredCookie();
            } else {
                inst.flush();
                inst.removeSessionCookies(null);
            }
        }
    }

    public static boolean setCookies2Apache(String url, HttpClient apacheStore) {
        // longer url better, domain only can return null
        String cookies = CookieManager.getInstance().getCookie(url);
        if (cookies == null || cookies.isEmpty()) {
            return false;
        }

        String[] cc = cookies.split(";");

        // check if we have cookies with same name. if yes. then webview set cookie for a different URL.
        // then we have to remove old cookie from apache store (check value apache==webview) and
        // replace it with new cookie from webview. This is come from WebView restriction, when we do not know
        // exact cookie domain/path. and only can call getCookie(url)

        ArrayList<HttpCookie> webviewStore = new ArrayList<>();

        Uri uri = Uri.parse(url);

        for (String c : cc) {
            String[] vv = c.split("=");
            String n = null;
            if (vv.length > 0)
                n = vv[0].trim();
            String v = null;
            if (vv.length > 1)
                v = vv[1].trim();
            if (n == null)
                continue;
            HttpCookie cookie = new HttpCookie(n, v);
            // it may cause troubles. Cookie maybe set for domain, .domain, www.domain or www.domain/path
            // and since we have to cut all www/path same name cookies with different paths will override.
            // need to check if returned cookie sting can contains DOMAIN/PATH values. Until then use domain only.
            cookie.setDomain(uri.getAuthority());
            webviewStore.add(cookie);
        }

        Set<String> dups = new TreeSet<>();

        for (int i = 0; i < webviewStore.size(); i++) {
            for (int k = 0; k < webviewStore.size(); k++) {
                if (k == i)
                    continue;
                String n1 = webviewStore.get(i).getName();
                if (n1.equals(webviewStore.get(k).getName()))
                    dups.add(n1);
            }
        }

        // find dups in Apache store. delete same cookie by values from WebView store
        for (String d : dups) {
            for (int i = apacheStore.getCount() - 1; i >= 0; i--) {
                HttpCookie c = apacheStore.getCookie(i);
                String n = c.getName();
                if (n.equals(d)) {
                    String v = c.getValue();
                    // remove WebView cookies, which name&&value == apache store
                    for (int k = webviewStore.size() - 1; k >= 0; k--) {
                        HttpCookie cw = webviewStore.get(k);
                        if (cw.getName().equals(n) && cw.getValue().equals(v)) {
                            webviewStore.remove(k);
                        }
                    }
                    // remove from apache store
                    HttpCookie rm = new HttpCookie(n, v);
                    rm.setPath(c.getPath());
                    rm.setDomain(c.getDomain());
                    apacheStore.removeCookie(rm);
                }
            }
        }

        // add remaining cookies from WebView store to Apache store
        for (HttpCookie c : webviewStore) {
            apacheStore.addCookie(c);
        }

        // since we have duplicates, (same cookies with different path. one set by setCookies2WebView
        // another set by WebView server call. drop them all.
        if (dups.size() != 0) {
            if (Build.VERSION.SDK_INT >= 21)
                CookieManager.getInstance().removeAllCookies(null);
            else
                CookieManager.getInstance().removeAllCookie();
        }

        return true;
    }

    public static void setCookies2WebView(Context context, HttpClient cookieStore) {
        // share cookies back (Apache --> WebView)
        if (cookieStore != null) {
            CookieSyncManager.createInstance(context);
            CookieManager m = CookieManager.getInstance();
            for (int i = 0; i < cookieStore.getCount(); i++) {
                HttpCookie c = cookieStore.getCookie(i);
                Uri.Builder b = new Uri.Builder();
                if (c.getSecure())
                    b.scheme("https");
                else
                    b.scheme("http");
                b.authority(c.getDomain());
                if (c.getPath() != null) {
                    b.appendPath(c.getPath());
                }
                String url = b.build().toString();
                m.setCookie(url, c.getName() + "=" + c.getValue());
            }
            CookieSyncManager.getInstance().sync();
        }
    }

    public void removeAllCookies() {
        if (http != null) {
            http.clearCookies();
            return;
        }
        removeWebCookies();
    }

    public void removeWebCookies() {
        if (Build.VERSION.SDK_INT >= 21)
            CookieManager.getInstance().removeAllCookies(null);
        else
            CookieManager.getInstance().removeAllCookie();
    }

}

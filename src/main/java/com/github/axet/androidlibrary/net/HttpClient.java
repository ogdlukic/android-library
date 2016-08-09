package com.github.axet.androidlibrary.net;

import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.ParseException;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.client.CredentialsProvider;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.AbstractExecutionAwareRequest;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.client.BasicCredentialsProvider;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.client.LaxRedirectStrategy;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HttpCoreContext;
import cz.msebera.android.httpclient.util.EntityUtils;

// cz.msebera.android.httpclient recommended by apache
//
// https://hc.apache.org/httpcomponents-client-4.5.x/android-port.html

public class HttpClient {
    public static final String TAG = HttpClient.class.getSimpleName();

    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MOB30Y) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.81 Mobile Safari/537.36";

    public static int CONNECTION_TIMEOUT = 10 * 1000;

    CloseableHttpClient httpclient;
    HttpClientContext httpClientContext = HttpClientContext.create();
    AbstractExecutionAwareRequest request;
    RequestConfig config;
    CredentialsProvider credsProvider;

    public static HttpCookie from(Cookie c) {
        HttpCookie cookie = new HttpCookie(c.getName(), c.getValue());
        cookie.setDomain(c.getDomain());
        cookie.setPath(c.getPath());
        cookie.setSecure(c.isSecure());
        return cookie;
    }

    public static BasicClientCookie from(HttpCookie m) {
        BasicClientCookie b = new BasicClientCookie(m.getName(), m.getValue());
        b.setDomain(m.getDomain());
        b.setPath(m.getPath());
        b.setSecure(m.getSecure());
        return b;
    }

    public static List<NameValuePair> from(Map<String, String> map) {
        List<NameValuePair> nvps = new ArrayList<>();
        for (String key : map.keySet()) {
            String value = map.get(key);
            nvps.add(new BasicNameValuePair(key, value));
        }
        return nvps;
    }

    public static String encode(Map<String, String> map) {
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(from(map));
            InputStream is = entity.getContent();
            return IOUtils.toString(is, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // http://stackoverflow.com/questions/1456987/httpclient-4-how-to-capture-last-redirect-url
    public static String getUrl(HttpClientContext context) {
        Object o = context.getAttribute(HttpCoreContext.HTTP_REQUEST);
        if (o instanceof HttpUriRequest) {
            HttpUriRequest currentReq = (HttpUriRequest) o;
            HttpHost currentHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
            return (currentReq.getURI().isAbsolute()) ? currentReq.getURI().toString() : (currentHost.toURI() + currentReq.getURI());
        }
        return null;
    }

    public static class HttpError extends HttpClient.DownloadResponse {
        static final String UTF8 = "UTF8";
        Throwable e;
        String msg;

        public HttpError(Throwable e) {
            super("text/plain", UTF8, (InputStream) null);
            this.e = e;
            while (e.getCause() != null)
                e = e.getCause();
            if (e instanceof ConnectTimeoutException) {
                ConnectTimeoutException t = (ConnectTimeoutException) e;
                setMessage("Connection Timeout: " + t.getMessage());
                return;
            }
            if (e instanceof SocketTimeoutException) {
                SocketTimeoutException t = (SocketTimeoutException) e;
                setMessage("Connection Timeout: " + t.getMessage());
                return;
            }
            setData(getStream(e));
        }

        public static InputStream getStream(Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return getStream(sw.toString());
        }

        public static InputStream getStream(String str) {
            try {
                return new ByteArrayInputStream(str.getBytes(UTF8));
            } catch (IOException ee) {
                Log.e(TAG, "HttpError", ee);
                return null;
            }
        }

        public void setMessage(String msg) {
            this.msg = msg;
            setData(getStream(msg));
        }

        public String getError() {
            if (msg != null)
                return msg;
            return e.getMessage();
        }

        @Override
        public boolean isHtml() {
            return false;
        }
    }

    public static class DownloadResponse extends WebResourceResponse {
        public boolean downloaded;

        public String userAgent;
        public String contentDisposition;
        public long contentLength;
        String url;
        byte[] buf;

        CloseableHttpResponse response;
        HttpEntity entity;
        StatusLine status;
        ContentType contentType;

        static boolean download(String mimetype) {
            String[] types = new String[]{"application/x-bittorrent", "audio", "video"};
            for (String t : types) {
                if (mimetype.startsWith(t))
                    return false;
            }
            return true;
        }

        public DownloadResponse(HttpClientContext context, HttpUriRequest request, CloseableHttpResponse response) {
            super(null, null, null);
            this.response = response;
            entity = response.getEntity();
            contentType = ContentType.getOrDefault(entity);
            url = HttpClient.getUrl(context);
            if (url == null)
                url = request.getURI().toString();
        }

        public DownloadResponse(String mimeType, String encoding, InputStream data) {
            super(mimeType, encoding, data);
            downloaded = true;
        }

        public DownloadResponse(String mimeType, String encoding, String data) {
            super(mimeType, encoding, null);
            setHtml(data);
            downloaded = true;
        }

        public String getUrl() {
            return url;
        }

        public byte[] getBuf() {
            return buf;
        }

        public String getHtml() {
            try {
                return new String(buf, getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public void setHtml(String html) {
            try {
                buf = html.getBytes(getEncoding());
                setData(new ByteArrayInputStream(buf));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public void download() {
            try {
                status = response.getStatusLine();
                setMimeType(contentType.getMimeType());
                if (entity != null) { // old phones it can be null
                    buf = IOUtils.toByteArray(entity.getContent());
                    Charset enc = contentType.getCharset();
                    if (enc == null) {
                        Document doc = Jsoup.parse(new String(buf, Charset.defaultCharset()));
                        Element e = doc.select("meta[http-equiv=Content-Type]").first();
                        if (e != null) {
                            String content = e.attr("content");
                            try {
                                contentType = ContentType.parse(content);
                                enc = contentType.getCharset();
                            } catch (ParseException ignore) {
                            }
                        } else {
                            e = doc.select("meta[charset]").first();
                            if (e != null) {
                                String content = e.attr("charset");
                                try {
                                    enc = Charset.forName(content);
                                } catch (UnsupportedCharsetException ignore) {
                                }
                            }
                        }
                    }
                    setEncoding(Charsets.toCharset(enc).name());
                    setData(new ByteArrayInputStream(buf));
                    downloaded = true;
                }
                consume();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void downloadText() {
            if (download(contentType.getMimeType())) {
                download();
            } else {
                attachment();
            }
        }

        public void attachment() {
            try {
                status = response.getStatusLine();
                setMimeType(contentType.getMimeType());
                Header ct = response.getFirstHeader("Content-Disposition");
                if (ct != null)
                    contentDisposition = ct.getValue();
                if (entity != null) // old phones it can be null
                    contentLength = entity.getContentLength();
                userAgent = USER_AGENT;
                consume();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void consume() throws IOException {
            EntityUtils.consume(entity);
            response.close();
        }

        public String getError() {
            if (status.getStatusCode() != 200)
                return status.getReasonPhrase();
            return null;
        }

        public boolean isHtml() {
            return getMimeType().equals("text/html");
        }

        public String getContentType() {
            return contentType.toString();
        }
    }

    public HttpClient() {
        create();
    }

    public HttpClient(String cookies) {
        create();

        if (cookies != null && !cookies.isEmpty())
            addCookies(cookies);
    }

    public void create() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(CONNECTION_TIMEOUT);
        requestBuilder.setConnectionRequestTimeout(CONNECTION_TIMEOUT);

        if (credsProvider == null)
            credsProvider = new BasicCredentialsProvider();

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        builder.setRedirectStrategy(new LaxRedirectStrategy());
        builder.setDefaultCredentialsProvider(credsProvider);

        // javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0xb89bbee8: Failure in SSL library, usually a protocol error
        // error:14077438:SSL routines:SSL23_GET_SERVER_HELLO:tlsv1 alert internal error (external/openssl/ssl/s23_clnt.c:741 0xaf144a4d:0x00000000)
        if (Build.VERSION.SDK_INT <= 17) {
            TrustManager[] byPassTrustManagers = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            }};
            HostnameVerifier v = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, byPassTrustManagers, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(v);
                builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sc, v));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }

        httpclient = builder.build();
    }

    public void setProxy(String host, int port, String scheme) {
        HttpHost proxy = new HttpHost(host, port, scheme);
        config = RequestConfig.custom().setProxy(proxy).build();
    }

    public void setProxy(String host, int port, String scheme, String login, String pass) {
        setProxy(host, port, scheme);
        credsProvider.setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(login, pass));
    }

    public void clearProxy() {
        config = null;
    }

    public void addCookies(String url, String cookies) {
        Uri u = Uri.parse(url);
        CookieStore s = getCookieStore();
        List<HttpCookie> cc = HttpCookie.parse(cookies);
        for (HttpCookie c : cc) {
            BasicClientCookie m = from(c);
            if (m.getDomain() == null) {
                m.setDomain(u.getAuthority());
            }
            removeCookie(m);
            s.addCookie(m);
        }
    }

    public void addCookies(String cookies) {
        CookieStore s = getCookieStore();
        List<HttpCookie> cc = HttpCookie.parse(cookies);
        for (HttpCookie c : cc) {
            BasicClientCookie m = from(c);
            removeCookie(m);
            s.addCookie(m);
        }
    }

    public void removeCookie(HttpCookie m) {
        removeCookie(from(m));
    }

    public void removeCookie(BasicClientCookie m) {
        try {
            CookieStore s = getCookieStore();
            BasicClientCookie rm = (BasicClientCookie) m.clone();
            rm.setExpiryDate(new Date(0));
            s.addCookie(rm);
        } catch (CloneNotSupportedException e) {
        }
    }

    public HttpCookie getCookie(int i) {
        CookieStore s = getCookieStore();
        Cookie c = s.getCookies().get(i);
        return from(c);
    }

    public void addCookie(HttpCookie c) {
        CookieStore s = getCookieStore();
        s.addCookie(from(c));
    }

    public int getCount() {
        CookieStore s = getCookieStore();
        return s.getCookies().size();
    }

    public String getCookies() {
        String str = "";

        CookieStore s = getCookieStore();
        List<Cookie> list = s.getCookies();
        for (int i = 0; i < list.size(); i++) {
            Cookie c = list.get(i);

            StringBuilder result = new StringBuilder()
                    .append(c.getName())
                    .append("=")
                    .append("\"")
                    .append(c.getValue())
                    .append("\"");
            appendAttribute(result, "path", c.getPath());
            appendAttribute(result, "domain", c.getDomain());

            if (!str.isEmpty())
                str += ", ";
            str += result.toString();
        }
        return str;
    }

    private void appendAttribute(StringBuilder builder, String name, String value) {
        if (value != null && builder != null) {
            builder.append(";");
            builder.append(name);
            builder.append("=\"");
            builder.append(value);
            builder.append("\"");
        }
    }

    public void clearCookies() {
        httpClientContext.setCookieStore(new BasicCookieStore());
    }

    public CookieStore getCookieStore() {
        CookieStore apacheStore = httpClientContext.getCookieStore();
        if (apacheStore == null) {
            apacheStore = new BasicCookieStore();
            httpClientContext.setCookieStore(apacheStore);
        }
        return apacheStore;
    }

    public void setCookieStore(CookieStore store) {
        httpClientContext.setCookieStore(store);
    }

    public AbstractExecutionAwareRequest getRequest() {
        return request;
    }

    public void abort() {
        request.abort();
        request = null;
    }

    public CloseableHttpResponse execute(String base, HttpRequestBase request) throws IOException {
        this.request = request;

        if (config != null)
            request.setConfig(config);
        if (base != null) {
            request.addHeader("Referer", base);
            Uri u = Uri.parse(base);
            request.addHeader("Origin", new Uri.Builder().scheme(u.getScheme()).authority(u.getAuthority()).toString());
            request.addHeader("User-Agent", USER_AGENT);
        }

        return httpclient.execute(request, httpClientContext);
    }

    public String get(String base, String url) {
        try {
            DownloadResponse w = getResponse(base, url);
            w.download();
            return w.getHtml();
        } finally {
            request = null;
        }
    }

    public byte[] getBytes(String base, String url) {
        try {
            DownloadResponse w = getResponse(base, url);
            w.download();
            return w.getBuf();
        } finally {
            request = null;
        }
    }

    // deal with java.lang.IllegalArgumentException: Illegal character in path at index
    String safe(String url) {
        try {
            URL u = new URL(url);
            URI uri = new URI(u.getProtocol(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getRef());
            return uri.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public DownloadResponse getResponse(String base, String url) {
        try {
            HttpGet httpGet = new HttpGet(safe(url));
            CloseableHttpResponse response = execute(base, httpGet);
            return new DownloadResponse(httpClientContext, httpGet, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            request = null;
        }
    }

    public String post(String base, String url, String[][] map) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < map.length; i++) {
            m.put(map[i][0], map[i][1]);
        }
        return post(base, url, m);
    }

    public String post(String base, String url, Map<String, String> map) {
        return post(base, url, from(map));
    }

    public String post(String base, String url, List<NameValuePair> nvps) {
        try {
            DownloadResponse w = postResponse(base, url, nvps);
            w.download();
            return w.getHtml();
        } finally {
            request = null;
        }
    }

    public DownloadResponse postResponse(String base, String url, Map<String, String> map) {
        return postResponse(base, url, from(map));
    }

    public DownloadResponse postResponse(String base, String url, List<NameValuePair> nvps) {
        try {
            HttpPost httpPost = new HttpPost(safe(url));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            CloseableHttpResponse response = execute(base, httpPost);
            return new DownloadResponse(httpClientContext, httpPost, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            request = null;
        }
    }
}

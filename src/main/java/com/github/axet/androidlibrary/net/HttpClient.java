package com.github.axet.androidlibrary.net;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
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
import cz.msebera.android.httpclient.HttpClientConnection;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpException;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpResponse;
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
import cz.msebera.android.httpclient.protocol.HttpContext;
import cz.msebera.android.httpclient.protocol.HttpCoreContext;
import cz.msebera.android.httpclient.protocol.HttpRequestExecutor;
import cz.msebera.android.httpclient.util.EntityUtils;

// cz.msebera.android.httpclient recommended by apache
//
// https://hc.apache.org/httpcomponents-client-4.5.x/android-port.html

public class HttpClient {
    public static final String TAG = HttpClient.class.getSimpleName();

    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MOB30Y) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.81 Mobile Safari/537.36";
    public static final String CONTENTTYPE_HTML = "text/html";

    public static int CONNECTION_TIMEOUT = 10 * 1000;

    protected CloseableHttpClient httpclient;
    protected HttpClientContext httpClientContext = HttpClientContext.create();
    protected AbstractExecutionAwareRequest request;
    protected HttpHost proxy;
    protected CredentialsProvider credsProvider;

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

        public HttpError(Throwable e) {
            super("text/plain", UTF8, (InputStream) null);
            this.e = e;

            while (e.getCause() != null)
                e = e.getCause();

            if (e instanceof ConnectTimeoutException) {
                ConnectTimeoutException t = (ConnectTimeoutException) e;
                setHtml("Connection Timeout: " + t.getMessage());
                return;
            }

            if (e instanceof SocketTimeoutException) {
                SocketTimeoutException t = (SocketTimeoutException) e;
                setHtml("Connection Timeout: " + t.getMessage());
                return;
            }
            setHtml(e);
        }

        public void setHtml(Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            setHtml(sw.toString());
        }

        public void setHtml(String str) {
            try {
                String html = "<html>";
                html += "<meta name=\"viewport\" content=\"initial-scale=1.0,user-scalable=no,maximum-scale=1,width=device-width\">";
                html += "<body>";
                html += TextUtils.htmlEncode(str);
                html += "</body></html>";
                buf = html.getBytes(Charset.defaultCharset().name());
                setData(new ByteArrayInputStream(buf));
            } catch (IOException ee) {
                Log.e(TAG, "HttpError", ee);
            }
        }

        @Override
        public String getError() {
            return e.getMessage();
        }

        @Override
        public void downloadText() {
            // ignore
        }

        @Override
        public void download() {
            // ignore
        }

        @Override
        public void attachment() {
            // ignore
        }

        @Override
        public boolean isHtml() {
            return true;
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
            return getMimeType().equals(CONTENTTYPE_HTML);
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
        if (credsProvider == null)
            credsProvider = new BasicCredentialsProvider();

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setUserAgent(USER_AGENT);
        builder.setDefaultRequestConfig(build((RequestConfig) null));
        builder.setRedirectStrategy(new LaxRedirectStrategy());
        builder.setDefaultCredentialsProvider(credsProvider);
        builder.setRequestExecutor(new HttpRequestExecutor() {
            @Override
            public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws IOException, HttpException {
                return super.execute(request, conn, context);
            }
        });

        // javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0xb89bbee8: Failure in SSL library, usually a protocol error
        // error:14077438:SSL routines:SSL23_GET_SERVER_HELLO:tlsv1 alert internal error (external/openssl/ssl/s23_clnt.c:741 0xaf144a4d:0x00000000)
        if (Build.VERSION.SDK_INT <= 16) {
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

        httpclient = build(builder);
    }

    protected CloseableHttpClient build(HttpClientBuilder builder) {
        return builder.build();
    }

    public RequestConfig build(RequestConfig config) {
        RequestConfig.Builder builder;
        if (config == null) {
            builder = RequestConfig.custom();
        } else {
            builder = RequestConfig.copy(config);
        }
        return build(builder);
    }

    public RequestConfig build(RequestConfig.Builder builder) {
        builder.setConnectTimeout(CONNECTION_TIMEOUT);
        builder.setConnectionRequestTimeout(CONNECTION_TIMEOUT);
        builder.setProxy(proxy);
        return builder.setProxy(proxy).build();
    }

    public void setProxy(String host, int port, String scheme) {
        proxy = new HttpHost(host, port, scheme);
    }

    public void setProxy(String host, int port, String scheme, String login, String pass) {
        setProxy(host, port, scheme);
        credsProvider.setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(login, pass));
    }

    public void clearProxy() {
        proxy = null;
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

    public CloseableHttpResponse execute(String base, HttpRequestBase request) {
        this.request = request;

        if (proxy != null) {
            request.setConfig(build(request.getConfig()));
        }

        if (base != null) {
            if (!base.equals(request.getURI().toString()))
                request.addHeader("Referer", base);
            Uri u = Uri.parse(base);
            request.addHeader("Origin", new Uri.Builder().scheme(u.getScheme()).authority(u.getAuthority()).toString());
        }

        return execute(request);
    }

    public CloseableHttpResponse execute(HttpRequestBase request) {
        try {
            return httpclient.execute(request, httpClientContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        } catch (RuntimeException e) {
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
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            request = null;
        }
    }
}

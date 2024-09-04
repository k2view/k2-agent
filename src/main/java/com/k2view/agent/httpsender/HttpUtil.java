package com.k2view.agent.httpsender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;

import static java.lang.Thread.currentThread;

public class HttpUtil {

    private static final Gson gson = new GsonBuilder().create();
    private static String DEFAULT_CONTENT_TYPE = "application/json";

    public static <T> T rte(WrapFunction<T> wrap) {
        try {
            return wrap.f();
        } catch (Throwable e) {
            checkInterrupt(e);
            throw wrapCause(RuntimeException::new, e);
        }
    }

    public static void rte(WrapProcedure wrap) {
        try {
            wrap.f();
        } catch (Throwable e) {
            checkInterrupt(e);
            throw wrapCause(RuntimeException::new, e);
        }
    }

    /**
     * Return http proxy
     * @param proxyUrl
     * @param proxyUrlPort
     * @return
     */
    public static ProxySelector httpProxy(String proxyUrl, String proxyUrlPort) {
        // Proxy Capabilities Logic
        if (!HttpUtil.isEmpty(proxyUrl)) {
            int proxyPort = 80;
            if (!HttpUtil.isEmpty(proxyUrlPort)) {
                try {
                    proxyPort = Integer.parseInt(proxyUrlPort);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number: " + proxyUrlPort, e);
                }
            }
            return ProxySelector.of(new InetSocketAddress(proxyUrl, proxyPort));
        }
        return null;
    }

    /**
     * Return http version
     * @param httpVersionEnv - Accepted values:  HTTP_1_1, HTTP_2
     * @return
     */
    public static HttpClient.Version httpVersion(String httpVersionEnv) {
        if (!HttpUtil.isEmpty(httpVersionEnv)) {
            return HttpClient.Version.valueOf(httpVersionEnv);
        }
        return HttpClient.Version.HTTP_2;
    }

    @FunctionalInterface
    public interface WrapProcedure {
        void f() throws Throwable;
    }


    @FunctionalInterface
    public interface WrapFunction<T> {
        T f() throws Throwable;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static void checkInterrupt(Throwable e) {
        if (e instanceof InterruptedException) {
            currentThread().interrupt();
        }
    }

    public static <E extends Throwable> E wrapCause(BiFunction<String, Throwable, E> newException, Throwable cause) {
        E wrap = newException.apply(def(cause.getMessage(), cause.toString()), cause);
        //noinspection unchecked
        return wrap.getClass().isInstance(cause) ? (E) cause : wrap;
    }

    public static <T> T def(T val, T def) {
        return val == null ? def : val;
    }

    public static HttpRequest.Builder buildRequest(URI uri, Map<String, String> headers, int timeout) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(timeout));

        if (headers != null) {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                requestBuilder.headers(kv.getKey(), kv.getValue());
            }
        }

        // Add default content type
        if (headers == null || !headers.containsKey("Content-Type")) {
            requestBuilder.header("Content-Type", DEFAULT_CONTENT_TYPE);
        }

        return requestBuilder;
    }

    public static String encode(String s) {
        return new String(java.util.Base64.getEncoder().encode(s.getBytes(StandardCharsets.UTF_8)));
    }

    public static String buildPostString(String... params) {
        return rte(() -> {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length - 1; i += 2) {
                if (!isEmpty(params[i + 1])) {
                    sb.append(params[i]).append("=").append(URLEncoder.encode(params[i + 1], "UTF-8")).append("&");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        });
    }

    public static Map<String, Object> jsonToMap(String jsonStr) {
        return gson.fromJson(jsonStr, Map.class);
    }

    /*
        Some sources send the expiresIn or the issuedAt as String and some as Long in the json response
    */
    public static long toLong(Object val) {
        if (val != null) {
            if (val instanceof Number num) {
                return num.longValue();
            }
            if (val instanceof String str) {
                return str.isEmpty() ? 0 : Long.parseLong(str);
            }
        }
        return 0;
    }
}

package com.k2view.agent.httpsender;

import com.k2view.agent.Utils;
import com.k2view.agent.httpsender.oauthKeyBank.OAuthKeyBankHttpSenderBuilder;
import com.k2view.agent.httpsender.simple.HttpSenderBuilder;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

public interface HttpSender extends AutoCloseable {

    HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception;

    static HttpSender get() {
        String oauthServerUrl = Utils.env("OAUTH_SERVER_URL");
        if (!HttpUtil.isEmpty(oauthServerUrl)) {
            String id = Utils.env("OAUTH_CLIENT_ID");
            String key = Utils.def(Utils.env("OAUTH_CLIENT_SECRET"),Utils.env("OAUTH_CLIENT_KEY"));
            String scope = Utils.env("OAUTH_SCOPE");
            String consumerKey = Utils.env("OAUTH_CONSUMER_KEY");
            String consumerSecret = Utils.env("OAUTH_CONSUMER_SECRET");
            Boolean logAuthServerRequests = Boolean.parseBoolean(Utils.env("OAUTH_LOG_TOKEN_REQUESTS"));

            var builder = new OAuthKeyBankHttpSenderBuilder(oauthServerUrl);

            return builder
                    .id(id)
                    .key(key)
                    .consumerKey(consumerKey)
                    .consumerSecret(consumerSecret)
                    .timeout(60)
                    .scope(scope)
                    .logTokenRequests(logAuthServerRequests)
                    .httpVersion(httpVersion())
                    .proxySelector(httpProxy()).buildSender();

        }

        // Fallback to simple http client
        return new HttpSenderBuilder().httpVersion(httpVersion())
                .proxySelector(httpProxy()).buildSender();
    }

    private static ProxySelector httpProxy() {
        // Proxy Capabilities Logic
        String agentProxyUrl = Utils.env("AGENT_PROXY_HOST");
        if (!HttpUtil.isEmpty(agentProxyUrl)) {
            int proxyPort = 80;
            String agentProxyUrlPort = Utils.env("AGENT_PROXY_PORT");

            if (!HttpUtil.isEmpty(agentProxyUrlPort)) {
                try {
                    proxyPort = Integer.parseInt(agentProxyUrlPort);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number: " + agentProxyUrlPort, e);
                }
            }

            return ProxySelector.of(new InetSocketAddress(agentProxyUrl, proxyPort));
        }
        return null;
    }

    private static HttpClient.Version httpVersion() {
        String httpVersionEnv = Utils.env("HTTP_VERSION");
        if (!HttpUtil.isEmpty(httpVersionEnv)) {
            return HttpClient.Version.valueOf(httpVersionEnv);
        }
        return HttpClient.Version.HTTP_2;
    }
}

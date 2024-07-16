package com.k2view.agent.httpsender;

import com.k2view.agent.Utils;
import com.k2view.agent.httpsender.oauth.OAuthHttpSender;
import com.k2view.agent.httpsender.oauth.OAuthRequestBuilder;
import com.k2view.agent.httpsender.oauth.TokenManager;
import com.k2view.agent.httpsender.simple.HttpSenderSimple;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

public interface HttpSender extends AutoCloseable {

    HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception;

    static HttpSender get() {
        String senderUrl = Utils.env("OAUTH_SERVER_URL");
        if (senderUrl != null && !senderUrl.isEmpty()) {
            String clientId = Utils.env("OAUTH_CLIENT_ID");
            if (clientId == null || clientId.isEmpty()) {
                throw new IllegalArgumentException("Client ID cannot be null or empty");
            }
            String clientSecret = Utils.env("OAUTH_CLIENT_SECRET");
            if (clientSecret == null || clientSecret.isEmpty()) {
                throw new IllegalArgumentException("Client secret cannot be null or empty");
            }

            String scope = Utils.env("OAUTH_SCOPE");
            if (scope == null || scope.isEmpty()) {
                scope = "0";
            }

            String consumerKey = Utils.env("OAUTH_CONSUMER_KEY");
            String consumerSecret = Utils.env("OAUTH_CONSUMER_SECRET");

            var builder = new OAuthRequestBuilder(senderUrl);

            if (!HttpUtil.isEmpty(consumerKey) && !HttpUtil.isEmpty(consumerSecret)) {
                builder.addTokenRequestCustomHeaders("Authorization", "Basic " + HttpUtil.encode(consumerKey + ":" + consumerSecret));
            }

            return builder.clientId(clientId)
                    .clientSecret(clientSecret)
                    .timeout(60)
                    .scope(scope)
                    .buildSender();
        }

        // Default to HTTP/2 if not specified
        HttpClient.Version httpVersion = HttpClient.Version.HTTP_2;
        String httpVersionEnv = Utils.env("HTTP_VERSION");

        if (httpVersionEnv != null && !httpVersionEnv.isEmpty()) {
            switch (httpVersionEnv) {
                case "HTTP_1_1":
                    httpVersion = HttpClient.Version.HTTP_1_1;
                    break;
                case "HTTP_2":
                    httpVersion = HttpClient.Version.HTTP_2;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid HTTP version: " + httpVersionEnv);
            }
        }

        // Http Client Builder
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(httpVersion);

        // Porxy Capabilties Logic
        String agentProxyUrl = Utils.env("AGENT_PROXY_URL");
        if(agentProxyUrl == null || agentProxyUrl.isEmpty()){
            
            int proxyPort = 80; // by default set for http port
            String agentProxyUrlPort = Utils.env("AGENT_PROXY_URL_PORT");

            if (agentProxyUrlPort != null && !agentProxyUrlPort.isEmpty()) {
                try {
                    proxyPort = Integer.parseInt(agentProxyUrlPort);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number: " + agentProxyUrlPort, e);
                }
            }

            httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(agentProxyUrl, proxyPort)));
        }

        // Http Client
        HttpClient httpClient = httpClientBuilder.build();

        return new HttpSenderSimple(httpClient);
    }
}

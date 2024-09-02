package com.k2view.agent.httpsender;

import com.k2view.agent.Utils;
import com.k2view.agent.httpsender.oauth.OAuthHttpSenderBuilder;
import com.k2view.agent.httpsender.simple.HttpSenderBuilder;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public interface HttpSender extends AutoCloseable {

    HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception;
    
    private static void set(String k, String v){
        System.setProperty(k,v);
    }


    static HttpSender get() {
        String oauthServerUrl = Utils.env("OAUTH_SERVER_URL");

        if (!HttpUtil.isEmpty(oauthServerUrl)) {
            // Use Open auth server with grant_type: client_credentials
            String clientId = Utils.env("OAUTH_CLIENT_ID");
            String clientSecret = Utils.env("OAUTH_CLIENT_SECRET");
            String clientIdParamName = Utils.env("OAUTH_CLIENT_ID_PARAM_NAME");
            String clientSecretParamName = Utils.env("OAUTH_CLIENT_SECRET_PARAM_NAME");
            String scope = Utils.env("OAUTH_SCOPE");
            String basicAuthUsername = Utils.env("OAUTH_BASIC_AUTH_USERNAME");
            String basicAuthPassword = Utils.env("OAUTH_BASIC_AUTH_PASSWORD");
            String acceptedType = Utils.env("OAUTH_ACCEPTED_TYPE");
            String contentType = Utils.env("OAUTH_CONTENT_TYPE");
            Boolean logAuthServerRequests = Boolean.parseBoolean(Utils.env("OAUTH_LOG_TOKEN_REQUESTS"));
            String authServerHttpVersion = Utils.env("AUTH_SERVER_HTTP_VERSION");
            String authServerProxyHost = Utils.env("AUTH_SERVER_PROXY_HOST");
            String authServerProxyPort = Utils.env("AUTH_SERVER_PROXY_PORT");

            var builder = new OAuthHttpSenderBuilder(oauthServerUrl);

            return builder
                    .clientId(clientId)
                    .clientIdKeyName(clientIdParamName)
                    .clientSecret(clientSecret)
                    .clientSecretKeyName(clientSecretParamName)
                    .customBasicAuthCredentials(basicAuthUsername, basicAuthPassword)
                    .timeout(60)
                    .scope(scope)
                    .acceptedType(acceptedType)
                    .contentType(contentType)
                    .authServerHttpVersion(HttpUtil.httpVersion(authServerHttpVersion))
                    .authServerProxySelector(HttpUtil.httpProxy(authServerProxyHost, authServerProxyPort))
                    .logTokenRequests(logAuthServerRequests)
                    .httpVersion(HttpUtil.httpVersion(Utils.env("AGENT_HTTP_VERSION")))
                    .proxySelector(HttpUtil.httpProxy(Utils.env("AGENT_PROXY_HOST"), Utils.env("AGENT_PROXY_PORT"))).buildSender();
        }

        // Fallback to simple http client
        return new HttpSenderBuilder().httpVersion(HttpUtil.httpVersion(Utils.env("AGENT_HTTP_VERSION")))
                .proxySelector(HttpUtil.httpProxy(Utils.env("AGENT_PROXY_HOST"), Utils.env("AGENT_PROXY_PORT"))).buildSender();
    }
}

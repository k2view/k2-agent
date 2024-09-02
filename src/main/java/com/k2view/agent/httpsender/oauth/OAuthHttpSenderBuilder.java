package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.httpsender.HttpUtil;
import com.k2view.agent.httpsender.simple.HttpSenderBuilder;

import java.net.ProxySelector;
import java.net.http.HttpClient;

import static java.util.Objects.requireNonNull;

public class OAuthHttpSenderBuilder extends HttpSenderBuilder {

    String clientIdKeyName = "client_id";
    String clientSecretKeyName = "client_secret";
    String authServerUrl;
    String scope;
    String clientId;
    String clientSecret;
    String basicAuthUserId = null;
    String basicAuthPassword = null;
    OAuthHttpSender.ClientAuthentication clientAuthentication = OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody;
    int timeout = 60;
    long tokenExpiration = -1;
    String contentType = "application/x-www-form-urlencoded";
    String acceptedType = "application/json";
    HttpClient.Version tokenServerHttpVersion;
    ProxySelector tokenServerProxySelector;
    boolean logTokenRequests = false;

    public OAuthHttpSenderBuilder(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    @Override
    public HttpSender buildSender() {
        return new OAuthHttpSender(createHttpClient(), new TokenManager(this), timeout);
    }

    public OAuthHttpSenderBuilder logTokenRequests(boolean logRequests) {
        this.logTokenRequests = logRequests;
        return this;
    }

    public OAuthHttpSenderBuilder clientId(String clientId) {
        requireNonNull(clientId, "clientId must be non-null");
        this.clientId = clientId;
        return this;
    }

    public OAuthHttpSenderBuilder clientIdKeyName(String keyName) {
        if(!HttpUtil.isEmpty(keyName)) {
            this.clientIdKeyName = keyName;
        }
        return this;
    }

    public OAuthHttpSenderBuilder clientSecretKeyName(String keyName) {
        if(!HttpUtil.isEmpty(keyName)) {
            this.clientSecretKeyName = keyName;
        }
        return this;
    }

    public OAuthHttpSenderBuilder clientSecret(String clientSecret) {
        requireNonNull(clientSecret, "clientSecret must be non-null");
        this.clientSecret = clientSecret;
        return this;
    }

    public OAuthHttpSenderBuilder basicAuthCredentials(String userid,String password) {
        this.basicAuthUserId = userid;
        this.basicAuthPassword = password;
        return this;
    }

    public OAuthHttpSenderBuilder scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthHttpSenderBuilder tokenExpiration(long tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
        return this;
    }

    public OAuthHttpSenderBuilder authServerHttpVersion(HttpClient.Version version) {
        this.tokenServerHttpVersion = version;
        return this;
    }

    public OAuthHttpSenderBuilder authServerProxySelector(ProxySelector proxySelector) {
        this.tokenServerProxySelector = proxySelector;
        return this;
    }


    public OAuthHttpSenderBuilder contentType(String contentType) {
        if(contentType != null) {
            this.contentType = contentType;
        }
        return this;
    }

    public OAuthHttpSenderBuilder acceptedType(String acceptedType) {
        if(acceptedType != null) {
            this.acceptedType = acceptedType;
        }
        return this;
    }

    public OAuthHttpSenderBuilder timeout(int timeoutInSeconds) {
        this.timeout = timeoutInSeconds;
        return this;
    }
}
package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpUtil;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class OAuthRequestBuilder {

    private HttpClient httpClient;
    String authServerUrl;
    String username;
    String password;
    String scope;
    String clientId;
    String clientSecret;
    OAuthHttpSender.ClientAuthentication clientAuthentication = OAuthHttpSender.ClientAuthentication.BasicAuthHeader;
    int timeout = 60;
    int tokenExpiration = -1;

    Map<String, String> extraHeaders = new HashMap<>();

    public OAuthRequestBuilder(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public OAuthHttpSender buildSender() {
        return new OAuthHttpSender(httpClient, new TokenManager(this), timeout);
    }

    public OAuthRequestBuilder username(String username) {
        requireNonNull(username, "username must be non-null");
        this.username = username;
        return this;
    }

    public OAuthRequestBuilder password(String password) {
        requireNonNull(password, "password must be non-null");
        this.password = password;
        return this;
    }

    public OAuthRequestBuilder clientId(String clientId) {
        requireNonNull(clientId, "clientId must be non-null");
        this.clientId = clientId;
        return this;
    }

    public OAuthRequestBuilder clientSecret(String clientSecret) {
        requireNonNull(clientSecret, "clientSecret must be non-null");
        this.clientSecret = clientSecret;
        return this;
    }

    public OAuthRequestBuilder scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthRequestBuilder timeout(int timeoutInSeconds) {
        this.timeout = timeoutInSeconds;
        return this;
    }

    public OAuthRequestBuilder tokenExpiration(int expirationInSeconds) {
        this.tokenExpiration = expirationInSeconds;
        return this;
    }

    public OAuthRequestBuilder clientAuthentication(OAuthHttpSender.ClientAuthentication clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
        return this;
    }

    public OAuthRequestBuilder extraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders == null ? new HashMap<>() : extraHeaders;
        return this;
    }

    public OAuthRequestBuilder httpClient(HttpClient client) {
        this.httpClient = client;
        return this;
    }

    String buildPostData() {
        String postStr;
        if (!HttpUtil.isEmpty(username)) {
            // grant_type=password
            postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_CONST, "password", "username", username, "password", password, "scope", scope);
        } else {
            // grant_type=client_credentials
            postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_CONST, "client_credentials", "scope", scope);
        }
        if (clientAuthentication == OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody) {
            postStr += "&" + HttpUtil.buildPostString("client_id", clientId, "client_secret", clientSecret);
        }
        return postStr;
    }
}

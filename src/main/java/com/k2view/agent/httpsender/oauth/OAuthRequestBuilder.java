package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpUtil;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class OAuthRequestBuilder {

    String authServerUrl;
    String scope;
    String clientId;
    String clientSecret;
    OAuthHttpSender.ClientAuthentication clientAuthentication = OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody;
    int timeout = 60;
    int tokenExpiration = -1;
    Map<String,String> tokenRequestCustomHeaders =new HashMap<>();


    public OAuthRequestBuilder(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public OAuthHttpSender buildSender() {
        return new OAuthHttpSender(HttpClient.newHttpClient(), new TokenManager(this), timeout);
    }

    public OAuthRequestBuilder clientId(String clientId) {
        requireNonNull(clientId, "clientId must be non-null");
        this.clientId = clientId;
        return this;
    }

    public OAuthRequestBuilder addTokenRequestCustomHeaders(String name,String value){
        requireNonNull(name, "name must be non-null");
        this.tokenRequestCustomHeaders.put(name,value);
        return this;
    }

    public OAuthRequestBuilder clientAuthentication(OAuthHttpSender.ClientAuthentication type){
        this.clientAuthentication = type;
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

    String buildPostData() {
        String postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_CONST, "client_credentials", "scope", scope);
        if (clientAuthentication == OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody) {
            postStr += "&" + HttpUtil.buildPostString("client_id", clientId, "client_secret", clientSecret);
        }
        return postStr;
    }
}
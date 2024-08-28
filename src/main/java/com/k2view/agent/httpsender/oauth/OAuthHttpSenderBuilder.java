package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.httpsender.simple.HttpSenderBuilder;

import java.net.ProxySelector;
import java.net.http.HttpClient;

import static java.util.Objects.requireNonNull;

public class OAuthHttpSenderBuilder extends HttpSenderBuilder {

    String authServerUrl;
    String scope;
    String clientId;
    String clientSecret;
    OAuthHttpSender.ClientAuthentication clientAuthentication = OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody;
    int timeout = 60;
    int tokenExpiration = -1;
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

    public HttpSenderBuilder logTokenRequests(boolean logRequests) {
        this.logTokenRequests = logRequests;
        return this;
    }

    public OAuthHttpSenderBuilder clientId(String clientId) {
        requireNonNull(clientId, "clientId must be non-null");
        this.clientId = clientId;
        return this;
    }

    public String getScope(){
        return scope;
    }

    public int getTimeout(){
        return timeout;
    }

    public boolean getLogTokenRequests(){
        return logTokenRequests;
    }

    public OAuthHttpSender.ClientAuthentication getClientAuthentication(){
        return clientAuthentication;
    }

    public String getContentType(){
        return contentType;
    }

    public String getAcceptedType(){
        return acceptedType;
    }

    public OAuthHttpSenderBuilder clientAuthentication(OAuthHttpSender.ClientAuthentication type){
        this.clientAuthentication = type;
        return this;
    }

    public OAuthHttpSenderBuilder clientSecret(String clientSecret) {
        requireNonNull(clientSecret, "clientSecret must be non-null");
        this.clientSecret = clientSecret;
        return this;
    }

    public OAuthHttpSenderBuilder scope(String scope) {
        this.scope = scope;
        return this;
    }

    public HttpSenderBuilder authServerServerHttpVersion(HttpClient.Version version) {
        this.tokenServerHttpVersion = version;
        return this;
    }


    public HttpSenderBuilder authServerServerProxySelector(ProxySelector proxySelector) {
        this.tokenServerProxySelector = proxySelector;
        return this;
    }

    public OAuthHttpSenderBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public OAuthHttpSenderBuilder acceptedType(String acceptedType) {
        this.acceptedType = acceptedType;
        return this;
    }

    public OAuthHttpSenderBuilder timeout(int timeoutInSeconds) {
        this.timeout = timeoutInSeconds;
        return this;
    }
}
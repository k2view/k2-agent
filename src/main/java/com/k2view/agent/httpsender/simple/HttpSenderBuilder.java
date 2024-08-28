package com.k2view.agent.httpsender.simple;

import com.k2view.agent.httpsender.HttpSender;

import java.net.ProxySelector;
import java.net.http.HttpClient;

public class HttpSenderBuilder {
    HttpClient.Version version = HttpClient.Version.HTTP_2;
    ProxySelector proxySelector;


    public HttpSenderBuilder httpVersion(HttpClient.Version version) {
        this.version = version;
        return this;
    }

    public HttpSenderBuilder proxySelector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    protected HttpClient createHttpClient(){
        return createHttpClient(version,proxySelector);
    }

    public static HttpClient createHttpClient(HttpClient.Version version,ProxySelector proxySelector){
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        if(version != null){
            httpClientBuilder.version(version);
        }
        if(proxySelector != null){
            httpClientBuilder.proxy(proxySelector);
        }
        return httpClientBuilder.build();
    }

    public HttpSender buildSender(){
        return new HttpSenderSimple(createHttpClient());
    }
}

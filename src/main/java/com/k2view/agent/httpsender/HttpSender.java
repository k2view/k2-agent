package com.k2view.agent.httpsender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HttpSender implements AutoCloseable {

    private int DEFAULT_TIMEOUT = 60; // seconds
    private final HttpClient senderClient;

    public HttpSender(HttpClient httpClient){
        this.senderClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
    }

    public HttpSender(){
        this(null);
    }

    public HttpResponse<String> postString(URI uri, String body, Map<String, String> headers) {
        return postString(uri,body,headers,DEFAULT_TIMEOUT);
    }

    public HttpResponse<String> postString(URI uri, String body, Map<String, String> headers,int timeout) {
        final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(uri, headers,timeout);
        return sendString(requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    protected HttpResponse<String> sendString(HttpRequest request) {
        return HttpUtil.rte(() -> senderClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }


    @Override
    public void close() {
        if (senderClient != null) {
            senderClient.close();
        }
    }
}

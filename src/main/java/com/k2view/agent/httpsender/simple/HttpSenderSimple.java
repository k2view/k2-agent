package com.k2view.agent.httpsender.simple;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.httpsender.HttpUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class HttpSenderSimple implements HttpSender {

    private final HttpClient senderClient;

    public HttpSenderSimple(){
        this.senderClient = HttpClient.newHttpClient();
    }

    @Override
    public HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception {
        final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(uri, headers,60);
        HttpRequest request = requestBuilder.POST(ofString(body)).build();
        return senderClient.send(request, BodyHandlers.ofString());
    }

    @Override
    public void close() {
        if (senderClient != null) {
            senderClient.close();
        }
    }
}

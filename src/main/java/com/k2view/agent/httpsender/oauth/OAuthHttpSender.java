package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.httpsender.HttpUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class OAuthHttpSender implements HttpSender {

    public enum ClientAuthentication {BasicAuthHeader, ClientCredentialsInBody}
    static final Set<Integer> NEED_TO_RENEW_TOKEN_ERROR_CODES = Set.of(401);

    public static final String GRANT_TYPE_CONST = "grant_type";

    private final HttpClient httpClient;

    private final TokenManager tokenMgr;

    private final int timeout;

    public OAuthHttpSender(HttpClient httpClient, TokenManager tokenMgr, int timeout) {
        this.httpClient = httpClient;
        this.tokenMgr = tokenMgr;
        this.timeout = timeout;
    }

    @Override
    public HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception {
        Map<String,String> h = new HashMap<>();
        if(headers != null){
            headers.putAll(h);
        }
        final HttpRequest.BodyPublisher b = ofString(body);
        String token = tokenMgr.getToken();
        Map<String, String> postHeaders = addAuth(h, token);
        final HttpRequest request = HttpUtil.buildRequest(uri, postHeaders, timeout)
                .POST(b)
                .build();

        // Send with the token
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
            tokenMgr.invalidateToken();

            // Send with the token
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
                tokenMgr.invalidateToken();
            }
        }
        return response;
    }

    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /*
    https://datatracker.ietf.org/doc/html/rfc6750
     */
    private Map<String, String> addAuth(Map<String, String> headers, String token) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

}

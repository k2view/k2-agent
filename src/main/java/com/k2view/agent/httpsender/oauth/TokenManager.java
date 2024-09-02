package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.Utils;
import com.k2view.agent.httpsender.HttpUtil;
import com.k2view.agent.httpsender.simple.HttpSenderBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class TokenManager {

    private final URI authURI;
    protected final OAuthHttpSenderBuilder oBuilder;

    public TokenManager(OAuthHttpSenderBuilder oBuilder) {
        authURI = URI.create(oBuilder.authServerUrl);
        this.oBuilder = oBuilder;
    }

    private String token = null;
    private long issuedAt = 0; // salesforce specific
    private long expiresIn = 0; // In seconds
    private long tokenCreationTime = 0;

    String getToken() {
        HttpUtil.rte(() -> {
            checkTokensExpiration();
            if (token == null) {
                getNewToken();
            }
        });
        if (HttpUtil.isEmpty(token)) {
            throw new IllegalStateException("Failed to get a valid token");
        }
        return token;
    }

    private void getNewToken() throws IOException, InterruptedException {
        final Map<String, String> headers = authHeaders();
        final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(authURI, headers, oBuilder.timeout);
        String postStr = buildGetTokenPostData();
        requestBuilder.POST(ofString(postStr));
        try {
            if (oBuilder.logTokenRequests) {
                logTokenRequest(authURI.toString(), headers, postStr);
            }
            try (final HttpClient client = HttpSenderBuilder.createHttpClient(oBuilder.tokenServerHttpVersion, oBuilder.tokenServerProxySelector)) {
                final HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 400) {
                    throw new IllegalStateException("Failed to get new token: " + response.body());
                }
                parseResponse(response.body());
            }
        } catch (Exception ex) {
            invalidateToken();
            throw ex;
        }
    }

    private static void logTokenRequest(String url, Map<String, String> headers, String postData) {
        StringBuilder sb = new StringBuilder("POST").append(System.lineSeparator());
        sb.append("'").append(url).append("'").append(System.lineSeparator());
        for (Map.Entry<String, String> h : headers.entrySet()) {
            sb.append("-H ").append("'").append(h.getKey()).append(":").append(" ").append(h.getValue()).append("'").append(System.lineSeparator());
        }
        sb.append("-d ").append("'").append(postData).append("'").append(System.lineSeparator());
        Utils.logMessage("INFO", sb.toString());
    }

    private void parseResponse(String body) {
        final Map<String, Object> response = HttpUtil.jsonToMap(body);
        token = (String) response.get("access_token");
        // Salesforce server only
        issuedAt = response.containsKey("issued_at") ? HttpUtil.toLong(response.get("issued_at")) : 0;
        expiresIn = response.containsKey("expires_in") ? HttpUtil.toLong(response.get("expires_in")) : 0;
        // Some servers will return "expires" and some "expires_in"
        if (expiresIn == 0 && response.containsKey("expires")) {
            expiresIn = HttpUtil.toLong(response.get("expires"));
        }
        tokenCreationTime = System.currentTimeMillis();
    }

    private void checkTokensExpiration() {
        if (token == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean valid = true;
        if (expiresIn > 0) {
            long timeOutToUse = calcTimeout(expiresIn, oBuilder.tokenExpiration);
            if (now - timeOutToUse < tokenCreationTime) {
                valid = false;
            }
        } else if (issuedAt > 0) { // Salesforce specific
            if (oBuilder.tokenExpiration > 0 && issuedAt + (oBuilder.tokenExpiration * 1000L) > now) {
                valid = false;
            }
        } else {
            if (oBuilder.tokenExpiration > 0 && tokenCreationTime + (oBuilder.tokenExpiration * 1000L) > now) {
                valid = false;
            }
        }
        if (!valid) {
            // Token expired
            invalidateToken();
        }
    }

    private static long calcTimeout(long timeout, long userDefinedTokenTimeout) {
        long timeOutToUse = userDefinedTokenTimeout > 0 ? Math.min(userDefinedTokenTimeout, timeout) : timeout;
        return (1000 * timeOutToUse);
    }

    void invalidateToken() {
        token = null;
    }

    protected Map<String, String> authHeaders() {
        Map<String, String> h = new HashMap<>();
        if (!HttpUtil.isEmpty(oBuilder.contentType)) {
            h.put("Content-Type", oBuilder.contentType);
        }
        if (!HttpUtil.isEmpty(oBuilder.acceptedType)) {
            h.put("Accept", oBuilder.acceptedType);
        }

        if (!HttpUtil.isEmpty(oBuilder.basicAuthUserId) && !HttpUtil.isEmpty(oBuilder.basicAuthPassword)) {
            h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.basicAuthUserId + ":" + oBuilder.basicAuthPassword));
        } else if (oBuilder.clientAuthentication == OAuthHttpSender.ClientAuthentication.BasicAuthHeader) {
            h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.clientId + ":" + oBuilder.clientSecret));
        }
        return h;
    }

    protected String buildGetTokenPostData() {
        String postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_KEY_NAME, OAuthHttpSender.GRANT_TYPE_VALUE, "scope", oBuilder.scope);
        if (oBuilder.clientAuthentication == OAuthHttpSender.ClientAuthentication.ClientCredentialsInBody) {
            postStr += "&" + HttpUtil.buildPostString(oBuilder.clientIdKeyName, oBuilder.clientId, oBuilder.clientSecretKeyName, oBuilder.clientSecret);
        }
        return postStr;
    }
}

package com.k2view.agent.httpsender.oauth;

import com.k2view.agent.httpsender.HttpUtil;

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
    private final OAuthRequestBuilder oBuilder;

    public TokenManager(OAuthRequestBuilder oBuilder) {
        authURI = URI.create(oBuilder.authServerUrl);
        this.oBuilder = oBuilder;
    }

    private String token = null;
    private String refreshToken = null;
    private long issuedAt = 0; // salesforce specific
    private long expiresIn = 0; // In seconds
    private long tokenCreationTime = 0;

    String getToken() {
        HttpUtil.rte(() -> {
            checkTokensExpiration();
            if (token == null) {
                if (refreshToken == null) {
                    getNewToken();
                } else {
                    try {
                        refreshToken();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        invalidateRefreshToken();
                        getNewToken();
                    }
                }
            }
        });
        if (HttpUtil.isEmpty(token)) {
            throw new IllegalStateException("Failed to get a valid token");
        }
        return token;
    }

    private void refreshToken() throws IOException, InterruptedException {
        final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(authURI, authHeaders(), oBuilder.timeout);
        final String postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_CONST, "refresh_token", "refresh_token", refreshToken, "scope", oBuilder.scope);
        requestBuilder.POST(ofString(postStr));
        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (OAuthHttpSender.NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
                throw new IllegalStateException("Invalid refresh token");
            }

            parseResponse(response.body());
        }

    }

    private void getNewToken() throws IOException, InterruptedException {
        final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(authURI, authHeaders(), oBuilder.timeout);
        String postStr = oBuilder.buildPostData();
        requestBuilder.POST(ofString(postStr));
        try {
            try (final HttpClient client = HttpClient.newHttpClient()) {
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
        String newRefreshToken = response.containsKey("refresh_token") ? (String) response.get("refresh_token") : null;
        if (!HttpUtil.isEmpty(newRefreshToken)) {
            this.refreshToken = newRefreshToken;
        }
        tokenCreationTime = System.currentTimeMillis();
    }


    private void checkTokensExpiration() {
        long now = System.currentTimeMillis();
        boolean valid = true;
        if (expiresIn > 0) {
            long timeOutToUse = calcTimeout(expiresIn, oBuilder.tokenExpiration);
            if (now - timeOutToUse < tokenCreationTime) {
                valid = false;
            }
        } else if (issuedAt > 0) { // Salesforce specific
            if (oBuilder.tokenExpiration > 0 && issuedAt + (oBuilder.tokenExpiration * 1000) > now) {
                valid = false;
            }
        } else {
            if (oBuilder.tokenExpiration > 0 && tokenCreationTime + (oBuilder.tokenExpiration * 1000) > now) {
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

    private void invalidateRefreshToken() {
        refreshToken = null;
    }

    private Map<String, String> authHeaders() {
        Map<String, String> h = new HashMap<>(oBuilder.extraHeaders);
        h.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        h.put("Accept", "application/json");
        if (oBuilder.clientAuthentication == OAuthHttpSender.ClientAuthentication.BasicAuthHeader) {
            if (!HttpUtil.isEmpty(oBuilder.username)) {
                h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.username + ":" + oBuilder.password));
            } else if (!HttpUtil.isEmpty(oBuilder.clientId) && !HttpUtil.isEmpty(oBuilder.clientSecret)) {
                h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.clientId + ":" + oBuilder.clientSecret));
            }
        }

        return h;
    }
}

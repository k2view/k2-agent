package com.k2view.agent.HttpSender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.util.Objects.requireNonNull;

public class OAuthHttpSender extends HttpSender {

    private static final Set<Integer> NEED_TO_RENEW_TOKEN_ERROR_CODES = Set.of(401);
    static final String GRANT_TYPE_CONST = "grant_type";

    private final TokenManager tokenMgr;

    private OAuthHttpSender(OAuthRequestBuilder oAuthRequestBuilder) {
        super(oAuthRequestBuilder.httpClient);
        tokenMgr = new TokenManager(oAuthRequestBuilder);
    }

    public static OAuthRequestBuilder newOAuthRequestBuilder(String authServerUrl) {
        return new OAuthRequestBuilder(authServerUrl);
    }

    @Override
    public synchronized HttpResponse<String> postString(URI uri, String body, Map<String, String> headers, int timeout) {
        final HttpRequest.BodyPublisher b = ofString(body);
        String token = tokenMgr.getToken();
        Map<String, String> postHeaders = addAuth(headers, token);
        final HttpRequest request = HttpUtil.buildRequest(uri, postHeaders, timeout)
                .POST(b)
                .build();

        // Send with the token
        HttpResponse<String> response = sendString(request);
        if (NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
            tokenMgr.invalidateToken();
            token = tokenMgr.getToken();
            postHeaders = addAuth(headers, token);
            final HttpRequest newRequest = HttpUtil.buildRequest(uri, postHeaders, timeout)
                    .POST(b)
                    .build();

            // Send with the token
            response = sendString(newRequest);
            if (NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
                tokenMgr.invalidateToken();
            }
        }
        return response;
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

    public static class OAuthRequestBuilder {

        private HttpClient httpClient;
        private String authServerUrl;
        private String username;
        private String password;
        private String scope;
        private String clientId;
        private String clientSecret;
        private boolean authInHeader = false;
        private int timeout = 60;
        private int tokenExpiration = -1;

        private Map<String, String> extraHeaders = new HashMap<>();

        private OAuthRequestBuilder(String authServerUrl) {
            this.authServerUrl = authServerUrl;
        }

        public HttpSender buildSender() {
            return new OAuthHttpSender(this);
        }

        public OAuthRequestBuilder username(String username) {
            requireNonNull(username, "username must be non-null");
            this.username = username;
            return this;
        }

        public OAuthRequestBuilder password(String password) {
            requireNonNull(username, "password must be non-null");
            this.password = password;
            return this;
        }

        public OAuthRequestBuilder clientId(String clientId) {
            requireNonNull(username, "clientId must be non-null");
            this.clientId = clientId;
            return this;
        }

        public OAuthRequestBuilder clientSecret(String clientSecret) {
            requireNonNull(username, "clientSecret must be non-null");
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

        public OAuthRequestBuilder authInHeader(boolean inHeader) {
            this.authInHeader = inHeader;
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

        private String buildPostData() {
            String postStr;
            if (!HttpUtil.isEmpty(username)) {
                // grant_type=password
                postStr = HttpUtil.buildPostString(GRANT_TYPE_CONST, "password", "username", username, "password", password, "scope", scope);
            } else {
                // grant_type=client_credentials
                postStr = HttpUtil.buildPostString(GRANT_TYPE_CONST, "client_credentials", "scope", scope);
            }
            if (!authInHeader) {
                postStr += "&" + HttpUtil.buildPostString("client_id", clientId, "client_secret", clientSecret);
            }
            return postStr;
        }
    }

    private static class TokenManager {

        private final URI authURI;
        private final OAuthRequestBuilder oBuilder;

        TokenManager(OAuthRequestBuilder oBuilder) {
            authURI = URI.create(oBuilder.authServerUrl);
            this.oBuilder = oBuilder;
        }

        private String token = null;
        private String refreshToken = null;
        private long issuedAt = 0; // salesforce specific
        private long expiresIn = 0; // In seconds
        private long tokenCreationTime = 0;

        private String getToken() {
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
            final String postStr = HttpUtil.buildPostString(GRANT_TYPE_CONST, "refresh_token", "refresh_token", refreshToken, "scope", oBuilder.scope);
            requestBuilder.POST(ofString(postStr));
            final HttpClient client = HttpClient.newHttpClient();
            final HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (NEED_TO_RENEW_TOKEN_ERROR_CODES.contains(response.statusCode())) {
                throw new IllegalStateException("Invalid refresh token");
            }
            parseResponse(response.body());

        }

        private void getNewToken() throws IOException, InterruptedException {
            final HttpRequest.Builder requestBuilder = HttpUtil.buildRequest(authURI, authHeaders(), oBuilder.timeout);
            String postStr = oBuilder.buildPostData();
            requestBuilder.POST(ofString(postStr));
            try {
                final HttpClient client = HttpClient.newHttpClient();
                final HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                parseResponse(response.body());
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

        private void invalidateToken() {
            token = null;
        }

        private void invalidateRefreshToken() {
            refreshToken = null;
        }

        private Map<String, String> authHeaders() {
            Map<String, String> h = Map.copyOf(oBuilder.extraHeaders);
            h.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            h.put("Accept", "application/json");
            if (oBuilder.authInHeader) {
                if (!HttpUtil.isEmpty(oBuilder.username)) {
                    h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.username + ":" + oBuilder.password));
                } else if (!HttpUtil.isEmpty(oBuilder.clientId) && !HttpUtil.isEmpty(oBuilder.clientSecret)) {
                    h.put("Authorization", "Basic " + HttpUtil.encode(oBuilder.clientId + ":" + oBuilder.clientSecret));
                }
            }

            return h;
        }
    }
}

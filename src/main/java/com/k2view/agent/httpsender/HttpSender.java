package com.k2view.agent.httpsender;

import com.k2view.agent.httpsender.oauth.OAuthRequestBuilder;
import com.k2view.agent.httpsender.simple.HttpSenderSimple;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

public interface HttpSender extends AutoCloseable {

    HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception;

    static HttpSender get(){
        String senderUrl = System.getProperty("OAUTH_SERVER_URL");
        if(senderUrl != null && !senderUrl.isEmpty()){
            String oAuthServerUrl = System.getProperty("OAUTH_SERVER_URL");
            if(oAuthServerUrl == null || oAuthServerUrl.isEmpty()){
                throw new IllegalArgumentException("OAuth server URL cannot be null or empty");
            }
            String clientId = System.getProperty("OAUTH_CLIENT_ID");
            if(clientId == null || clientId.isEmpty()){
                throw new IllegalArgumentException("Client ID cannot be null or empty");
            }
            String clientSecret = System.getProperty("OAUTH_CLIENT_SECRET");
            if(clientSecret == null || clientSecret.isEmpty()){
                throw new IllegalArgumentException("Client secret cannot be null or empty");
            }

            var builder = new OAuthRequestBuilder(oAuthServerUrl);
            builder.clientId(clientId).clientSecret(clientSecret).timeout(60);
            return builder.buildSender();
        }

        return new HttpSenderSimple();
    }
}

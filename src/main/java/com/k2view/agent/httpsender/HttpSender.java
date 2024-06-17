package com.k2view.agent.httpsender;

import com.k2view.agent.Utils;
import com.k2view.agent.httpsender.oauth.OAuthRequestBuilder;
import com.k2view.agent.httpsender.simple.HttpSenderSimple;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

public interface HttpSender extends AutoCloseable {

    HttpResponse<String> send(URI uri, String body, Map<String, String> headers) throws Exception;

    static HttpSender get(){
        String senderUrl = Utils.env("OAUTH_SERVER_URL");
        if(senderUrl != null && !senderUrl.isEmpty()){
            String clientId = Utils.env("OAUTH_CLIENT_ID");
            if(clientId == null || clientId.isEmpty()){
                throw new IllegalArgumentException("Client ID cannot be null or empty");
            }
            String clientSecret = Utils.env("OAUTH_CLIENT_SECRET");
            if(clientSecret == null || clientSecret.isEmpty()){
                throw new IllegalArgumentException("Client secret cannot be null or empty");
            }

            String scope = Utils.env("OAUTH_SCOPE");
            if(scope == null || scope.isEmpty()){
                scope = "0";
            }

            var builder = new OAuthRequestBuilder(senderUrl);
            return builder.clientId(clientId)
                    .clientSecret(clientSecret)
                    .timeout(60)
                    .scope(scope)
                    .buildSender();
        }

        return new HttpSenderSimple();
    }
}

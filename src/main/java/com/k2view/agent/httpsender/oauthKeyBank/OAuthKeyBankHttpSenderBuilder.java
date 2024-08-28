package com.k2view.agent.httpsender.oauthKeyBank;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.httpsender.oauth.OAuthHttpSender;
import com.k2view.agent.httpsender.oauth.OAuthHttpSenderBuilder;

import static java.util.Objects.requireNonNull;

public class OAuthKeyBankHttpSenderBuilder extends OAuthHttpSenderBuilder {
    String id;
    String key;
    String consumerKey;
    String consumerSecret;

    public OAuthKeyBankHttpSenderBuilder(String authServerUrl) {
        super(authServerUrl);
        super.acceptedType("");
    }

    @Override
    public HttpSender buildSender() {
        return new OAuthHttpSender(createHttpClient(), new KeyBankTokenManager(this), getTimeout());
    }
    public OAuthKeyBankHttpSenderBuilder id(String id) {
        requireNonNull(id, "id must be non-null");
        this.id = id;
        return this;
    }

    public OAuthKeyBankHttpSenderBuilder key(String key) {
        requireNonNull(key, "key must be non-null");
        this.key = key;
        return this;
    }

    public OAuthKeyBankHttpSenderBuilder consumerKey(String consumerKey) {
        requireNonNull(consumerKey, "consumerKey must be non-null");
        this.consumerKey = consumerKey;
        return this;
    }

    public OAuthKeyBankHttpSenderBuilder consumerSecret(String consumerSecret) {
        requireNonNull(consumerSecret, "consumerSecret must be non-null");
        this.consumerSecret = consumerSecret;
        return this;
    }
}

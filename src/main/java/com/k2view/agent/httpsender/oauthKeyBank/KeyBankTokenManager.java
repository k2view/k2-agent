package com.k2view.agent.httpsender.oauthKeyBank;

import com.k2view.agent.httpsender.HttpUtil;
import com.k2view.agent.httpsender.oauth.OAuthHttpSender;
import com.k2view.agent.httpsender.oauth.TokenManager;

import java.util.Map;

public class KeyBankTokenManager extends TokenManager {
    public KeyBankTokenManager(OAuthKeyBankHttpSenderBuilder oBuilder) {
        super(oBuilder);
    }


    @Override
    protected String buildGetTokenPostData() {
        String postStr = HttpUtil.buildPostString(OAuthHttpSender.GRANT_TYPE_CONST, "client_credentials", "scope", oBuilder.getScope());
        postStr += "&" + HttpUtil.buildPostString("Id",((OAuthKeyBankHttpSenderBuilder)oBuilder).id, "Key", ((OAuthKeyBankHttpSenderBuilder)oBuilder).key);
        return postStr;
    }

    @Override
    protected Map<String, String> authHeaders(){
        Map<String, String> h = super.authHeaders();
        h.put("Authorization", "Basic " + HttpUtil.encode(((OAuthKeyBankHttpSenderBuilder)oBuilder).consumerKey + ":" + ((OAuthKeyBankHttpSenderBuilder)oBuilder).consumerSecret));
        return h;
    }
}

package com.k2view.agent.postman;

import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.Requests;
import com.k2view.agent.Response;
import com.k2view.agent.Utils;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class CloudManager implements Postman {

    /**
     * The ID of the mailbox used for receiving messages.
     */
    private final String mailboxId;

    private final URI uri;

    /**
     * An instance of the Java HTTP client for sending HTTP requests.
     */
    private final HttpSender client;

    public CloudManager(String mailboxId, String mailboxUrl) {
        this.mailboxId = mailboxId;
        this.uri = URI.create(mailboxUrl);

        /* Grant type "password"   (deprecated. Not recommended)  */
        /*
        final OAuthHttpSender.OAuthRequestBuilder oAuthBuilder = OAuthHttpSender.newOAuthRequestBuilder("{OAUTH server URL}");
        oAuthBuilder.username("admin").password("admin");
        this.client = oAuthBuilder.buildSender();
         */

        /* Grant type "client_credentials"  */
        /*
        final OAuthHttpSender.OAuthRequestBuilder oAuthBuilder = OAuthHttpSender.newOAuthRequestBuilder("{OAUTH server URL}");
        oAuthBuilder.clientId("The client ID").clientSecret("The client secret");
        this.client = oAuthBuilder.buildSender();
         */

        this.client = new HttpSender();
    }

    @Override
    public Requests getInboxMessages(List<Response> responses, String lastTaskId) {
        Utils.logMessage("INFO", "FETCHING MESSAGES FROM: " + uri.toString() + ", ID:" + mailboxId);
        Map<String, Object> r = new HashMap<>();
        r.put("responses", responses);
        r.put("id", mailboxId);
        r.put("since", lastTaskId);
        String body = Utils.gson.toJson(r);

        try {
            final HttpResponse<String> response = client.postString(uri, body, null);
            String jsonArrayString = response.body(); // Check response status code ?
            return Utils.gson.fromJson(jsonArrayString, Requests.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

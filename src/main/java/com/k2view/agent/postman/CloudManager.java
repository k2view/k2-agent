package com.k2view.agent.postman;

import com.k2view.agent.Request;
import com.k2view.agent.httpsender.HttpSender;
import com.k2view.agent.Requests;
import com.k2view.agent.Response;
import com.k2view.agent.Utils;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;


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

    public CloudManager(String mailboxId, String mailboxUrl, HttpSender client) {
        if(mailboxId == null || mailboxId.isEmpty()) {
            throw new IllegalArgumentException("Mailbox ID cannot be null or empty");
        }
        if(mailboxUrl == null || mailboxUrl.isEmpty()) {
            throw new IllegalArgumentException("Mailbox URL cannot be null or empty");
        }
        this.mailboxId = mailboxId;
        this.uri = URI.create(mailboxUrl);
        this.client = client;
    }

    @Override
    public Requests getInboxMessages(List<Response> responses) {
        Utils.logMessage("INFO", "FETCHING MESSAGES FROM: " + uri.toString() + ", ID:" + mailboxId);
        String body = Utils.gson.toJson(new PostmanRequestBody(responses, mailboxId));

        try {
            final HttpResponse<String> response = client.send(uri, body, null);
            if(response.statusCode() != 201) {
                Utils.logMessage("ERROR", "Failed to fetch messages from the server: " + response.body());
                return new Requests(List.of(), 30);
            }
            String jsonArrayString = response.body();
            return Utils.gson.fromJson(jsonArrayString, Requests.class);
        } catch (Exception e) {
            Utils.logMessage("ERROR", "Failed to fetch messages from the server: " + e.getMessage());
            return new Requests(List.of(), 30);
        }
    }
}

package com.k2view.agent.postman;

import com.k2view.agent.Requests;
import com.k2view.agent.Response;
import com.k2view.agent.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

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
    private final HttpClient client;

    public CloudManager(String mailboxId, String mailboxUrl) {
        if(mailboxId == null || mailboxId.isEmpty()) {
            throw new IllegalArgumentException("Mailbox ID cannot be null or empty");
        }
        if(mailboxUrl == null || mailboxUrl.isEmpty()) {
            throw new IllegalArgumentException("Mailbox URL cannot be null or empty");
        }
        this.mailboxId = mailboxId;
        this.uri = URI.create(mailboxUrl);
        this.client = HttpClient.newBuilder().build();
    }

    @Override
    public Requests getInboxMessages(List<Response> responses) {
        Utils.logMessage("INFO", "FETCHING MESSAGES FROM: " + uri.toString() + ", ID:" + mailboxId);
        String body = Utils.gson.toJson(new PostmanRequestBody(responses, mailboxId));
        HttpRequest request = HttpRequest.newBuilder()
                .POST(ofString(body))
                .uri(uri)
                .header("Content-Type", "application/json")
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonArrayString = response.body();
            return Utils.gson.fromJson(jsonArrayString, Requests.class);
        } catch (Exception e) {
            Utils.logMessage("ERROR", "Failed to fetch messages from the server: " + e.getMessage());
            return new Requests(List.of(), 0);
        }
    }
}

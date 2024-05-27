package com.k2view.agent;

import com.k2view.agent.dispatcher.AgentDispatcher;
import com.k2view.agent.dispatcher.AgentDispatcherHttp;
import com.k2view.agent.postman.CloudManager;
import com.k2view.agent.postman.Postman;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.k2view.agent.Utils.*;
import static java.lang.Integer.parseInt;

/**
 * The K2ViewAgent class represents an agent that reads a list of URLs from a REST API and forwards the requests to external URLs via AgentSender object.
 * This class uses the Gson library for JSON serialization and the HttpClient class for sending HTTP requests.
 */
public class K2ViewAgent {

    private static final int MAX_RETRY = 3;

    /**
     * The polling interval in seconds for checking the inbox for new messages.
     */
    private final int pollingInterval;

    /**
     * The `AgentSender` instance used for sending requests and processing responses.
     */
    private final AgentDispatcher dispatcher;

    /**
     * The `Postman` instance used for fetching messages from the REST API.
     */
    private final Postman postman;


    private final Set<String> inProgress = ConcurrentHashMap.newKeySet();

    /**
     * An atomic boolean that determines if the `K2ViewAgent` is running.
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    public K2ViewAgent(Postman postman, int pollingInterval, AgentDispatcher agentSender) {
        this.postman = postman;
        this.dispatcher = agentSender;
        this.pollingInterval = pollingInterval;
    }


    /**
     * Starts the agent by initializing the `agentSender`, `id`, and `since` fields,
     * and calling the `start()` method.
     */
    public static void main(String[] args) {
        Postman postman = new CloudManager(env("K2_MAILBOX_ID"), env("K2_MANAGER_URL"));
        int interval = parseInt(def(env("K2_POLLING_INTERVAL"), "10"));
        AgentDispatcherHttp sender = new AgentDispatcherHttp(10_000);
        var agent = new K2ViewAgent(postman, interval, sender);
        Runtime.getRuntime().addShutdownHook(new Thread(agent::stop));
        agent.run();
    }

    /**
     * Starts the manager thread that continuously checks for new inbox messages
     * and sends them to the `agentSender` for processing.
     */
    public void run() {
        try {
            List<Response> responseList = new ArrayList<>();
            List<Request> retryList = new ArrayList<>();
            long interval;
            while (running.get()) {
                Requests inboxMessages = postman.getInboxMessages(responseList);
                cleanInProcess(responseList);
                interval = inboxMessages.pollInterval() > 0 ? inboxMessages.pollInterval() : pollingInterval;
                for (Request req : mergeWithRetryList(retryList, inboxMessages.requests())) {
                    Utils.logMessage("INFO", "Added URL to the Queue:" + req);
                    dispatcher.send(req);
                }
                var list = dispatcher.receive(interval, TimeUnit.SECONDS);
                var filteredResponse = filterResponses(list);
                responseList = filteredResponse.returnToSender;
                retryList = filteredResponse.retry;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the task ids of the responses from the `inProgress` set.
     * @param responseList the list of responses to clean.
     */
    private void cleanInProcess(List<Response> responseList) {
        for(var res : responseList){
            inProgress.remove(res.taskId());
        }
    }

    /**
     * Merges the `retryList` with the `requests` list.
     * @param retryList the list of requests to retry.
     * @param requests the list of requests to merge with the `retryList`.
     * @return a merged list of requests.
     */
    private List<Request> mergeWithRetryList(List<Request> retryList, List<Request> requests) {
        List<Request> merged = new ArrayList<>(retryList);
        for(Request req : requests){
            if(inProgress.contains(req.taskId())){
                continue;
            }
            inProgress.add(req.taskId());
            merged.add(req);
        }
        return merged;
    }

    record FilteredResponse(List<Response> returnToSender, List<Request> retry) { }

    /**
     * Filters the responses list into two lists:
     * 1. A list of responses to return to the sender.
     * 2. A list of requests to retry.
     * @param responses the list of responses to filter.
     * @return a `FilteredResponse` object containing the two lists.
     */
    private FilteredResponse filterResponses(List<Response> responses) {
        List<Response> returnToSender = new ArrayList<>();
        List<Request> retry = new ArrayList<>();
        for (Response res : responses) {
            logMessage("INFO", "Received response: " + res);
            if(needToRetry(res)){
                Request request = res.request();
                Utils.logMessage("INFO", "Retrying request: " + request);
                request.incrementTryCount();
                retry.add(request);
            } else {
                Utils.logMessage("INFO", "return to sender: " + res);
                returnToSender.add(res);
            }
        }
        return new FilteredResponse(returnToSender, retry);
    }

    private boolean needToRetry(Response res) {
        return res.code() >= 500 && res.request().getTryCount() < MAX_RETRY-1;
    }

    /**
     * Stops the agent by setting the `running` flag to `false`.
     */
    public void stop() {
        Utils.logMessage("INFO", "Stopping agent");
        running.set(false);
    }
}



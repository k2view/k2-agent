package com.k2view.agent.postman;

import com.k2view.agent.Requests;
import com.k2view.agent.Response;
import java.util.List;

public interface Postman {

    /**
     * Retrieves a list of inbox messages from the REST API.X
     *
     * @param responses a list of previous responses received from the server
     * @return a list of `AgentSender.Request` objects
     */
    Requests getInboxMessages(List<Response> responses);

    record PostmanRequestBody(List<Response.ResponseSimple> responses, String id, String since) {
        PostmanRequestBody(List<Response> responses, String id){
            this(responses.stream().map(Response::toSimple).toList(), id, "0");
        }
    }
}

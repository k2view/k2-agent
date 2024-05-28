package com.k2view.agent;

/**
 * Represents an HTTP response received from the server.
 */
public record Response(Request request, int code, String body) {

    public String taskId() {
        return request.taskId();
    }

    @Override
    public String toString() {
        return "Response{ taskId='%s', code='%s', body='%s' }".formatted(request.taskId(), code, body);
    }
}

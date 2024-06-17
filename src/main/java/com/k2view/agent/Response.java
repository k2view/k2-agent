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
        return "Response{ taskId='%s', code='%s' }".formatted(request.taskId(), code);
    }

    public ResponseSimple toSimple() {
        return new ResponseSimple(request.taskId(), code, body);
    }

    public record ResponseSimple(String taskId, int code, String body) {
        @Override
        public String toString() {
            return "ResponseSimple{ taskId='%s', code='%s', }".formatted(taskId, code);
        }
    }
}

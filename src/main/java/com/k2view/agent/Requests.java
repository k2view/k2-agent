package com.k2view.agent;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an HTTP request that is sent to the server.
 */

public record Requests(List<Request> requests, long pollInterval) {

    public static final Requests EMPTY = new Requests(List.of(), 0);

    public boolean isEmpty() {
        return requests.isEmpty();
    }

    public static class Adapter extends TypeAdapter<Requests>{
        @Override
        public void write(JsonWriter jsonWriter, Requests requests) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("requests");
            jsonWriter.beginArray();
            for (Request request : requests.requests) {
                Utils.gson.toJson(request, Request.class, jsonWriter);
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        }

        @Override
        public Requests read(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            List<Request> requests = new ArrayList<>();
            long pollInterval = 0;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                switch (name) {
                    case "requests" -> {
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            requests.add(Utils.gson.fromJson(jsonReader, Request.class));
                        }
                        jsonReader.endArray();
                    }
                    case "pollInterval" -> pollInterval = jsonReader.nextLong();
                    default -> jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            return new Requests(requests, pollInterval);
        }
    }
}

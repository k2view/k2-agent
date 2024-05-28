package com.k2view.agent;

import com.k2view.agent.dispatcher.AgentDispatcherHttp;
import com.k2view.agent.postman.CloudManager;
import com.k2view.agent.postman.Postman;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class E2ETest {

    static class Mailbox {
        static final Mailbox INSTANCE = new Mailbox(List.of(new Request("1", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body"),
                new Request("2", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body"),
                new Request("3", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body")));
        List<Request> requests;
        List<Response> responses;
        CountDownLatch latch;

        boolean sinceOnlyInc = true;
        String lastSince = "0";

        String lastSinceReceived = "0";
        public Mailbox(List<Request> requests) {
            this.requests = new ArrayList<>(requests);
            this.responses = new ArrayList<>();
            this.latch = new CountDownLatch(requests.size());
        }

        void addResponse(Response response) {
            responses.add(response);
            requests.removeIf(request -> request.taskId().equals(response.taskId()));
            if(requests.isEmpty()){
                lastSince = "0";
            }
            latch.countDown();
        }

        List<Request> requests(String since) {
            if(sinceOnlyInc && !requests.isEmpty()){
                sinceOnlyInc = since.compareTo(lastSince) >= 0;
                lastSince = since;
            }
            lastSinceReceived = since;
            return requests.stream().filter(request -> request.taskId().compareTo(since) > 0).toList();
        }

    }



    static class CloudManagerMock implements AutoCloseable {
        HttpServer server;
        public CloudManagerMock() throws Exception {
            HttpServer server = HttpServer.create();
            server.bind(new InetSocketAddress(8081), 0);
            server.createContext("/", new MyHandler());
            server.start();
        }

        @Override
        public void close() throws Exception {
            if (server != null) {
                server.stop(0);
            }
        }

        static class MyHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                InputStream requestBody = exchange.getRequestBody();
                var reader = new BufferedReader(new java.io.InputStreamReader(requestBody));
                var postmanRequestBody = Utils.gson.fromJson(reader, Postman.PostmanRequestBody.class);
                var responses = postmanRequestBody.responses();
                for (Response response : responses) {
                    Mailbox.INSTANCE.addResponse(response);
                }
                var requests = new Requests(Mailbox.INSTANCE.requests(postmanRequestBody.since()), 5);
                String response = Utils.gson.toJson(requests);
                exchange.sendResponseHeaders(200, response.length());
                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(response.getBytes());
                responseBody.close();
            }
        }
    }



    @Before
    public void setup() {
        TestingHttpServer.INSTANCE.reset();
    }



    @Test
    public void test() throws Exception {
        try(var server = new CloudManagerMock();
            var dispatcher = new AgentDispatcherHttp(10_000)){
            var postman = new CloudManager("123", "http://localhost:8081");
            var agent = new K2ViewAgent(postman, 3 , dispatcher);
            var thread = new Thread(agent::run);
            thread.start();
            Mailbox.INSTANCE.latch.await();
            assertTrue(Mailbox.INSTANCE.requests.isEmpty());
            assertEquals(3, Mailbox.INSTANCE.responses.size());
            assertEquals(3, TestingHttpServer.INSTANCE.counter.get());
            assertTrue(Mailbox.INSTANCE.sinceOnlyInc);
            Mailbox.INSTANCE.latch = new CountDownLatch(2);
            Mailbox.INSTANCE.requests.add(new Request("4", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body"));
            Mailbox.INSTANCE.requests.add(new Request("5", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body"));
            Mailbox.INSTANCE.latch.await();
            assertEquals(5, Mailbox.INSTANCE.responses.size());
            assertEquals(5, TestingHttpServer.INSTANCE.counter.get());
            assertTrue(Mailbox.INSTANCE.sinceOnlyInc);
            thread.interrupt();
        }
    }
}

package com.k2view.agent;

import com.k2view.agent.dispatcher.AgentDispatcher;
import com.k2view.agent.postman.Postman;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class K2ViewAgentTest {

    static TestingHttpServer server;
    @BeforeAll
    public static void setup() {
        server = TestingHttpServer.INSTANCE;
        server.reset();
    }

    static class MockPostMan implements Postman {

        long interval = 10;

        Supplier<Requests> requests = ()->new Requests(Collections.singletonList(new Request("test", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body")),
                interval);
        @Override
        public Requests getInboxMessages(List<Response> responses) {
            return requests.get();
        }
    }

    static class MockDispatcher implements AgentDispatcher {

        long interval = 10;
        @Override
        public void send(Request request) {
            assertEquals("test", request.taskId());
            assertEquals("http://localhost:8080", request.url());
            assertEquals("GET", request.method());
            assertEquals("body", request.body());
            assertEquals(Map.of("Content-Type", "application/json"), request.header());
        }

        @Override
        public List<Response> receive(long timeout, TimeUnit unit) throws InterruptedException {
            assertEquals(interval, timeout);
            Thread.sleep(1000);
            return Collections.emptyList();
        }

        @Override
        public void close() throws Exception {
            System.out.println("close");
        }
    }

    @Test
    public void test_agent() throws InterruptedException {
        K2ViewAgent agent = new K2ViewAgent(new MockPostMan(), 60, new MockDispatcher());
        Thread thread = new Thread(()-> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                agent.stop();
            }
        });
        thread.start();
        agent.run();
    }

    @Test
    public void test_no_interval() throws InterruptedException {
        MockPostMan postman = new MockPostMan();
        postman.interval = 0;
        MockDispatcher agentSender = new MockDispatcher();
        agentSender.interval = 60;
        var agent = new K2ViewAgent(postman, 60, agentSender);
        Thread thread = new Thread(()-> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                agent.stop();
            }
        });
        thread.start();
        agent.run();
    }

    @Test
    public void test_no_requests() throws InterruptedException {
        MockPostMan postman = new MockPostMan();
        postman.requests = ()-> Requests.EMPTY;
        MockDispatcher agentSender = new MockDispatcher();
        agentSender.interval = 60;
        var agent = new K2ViewAgent(postman, 60, agentSender);
        Thread thread = new Thread(()-> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                agent.stop();
            }
        });
        thread.start();
        agent.run();
    }

    static Request testRetry = new Request("test", "http://localhost:8080", "GET", Map.of("Content-Type", "application/json"), "body");

    static class MockPostManRetry implements Postman {

        long interval = 10;
        AtomicInteger count = new AtomicInteger(0);

        Requests requests = new Requests(Collections.singletonList(testRetry),
                interval);
        @Override
        public Requests getInboxMessages(List<Response> responses) {
            count.incrementAndGet();
            return requests;
        }
    }

    static class MockDispatcherRetry implements AgentDispatcher {

        int count = 0;
        CountDownLatch latch = new CountDownLatch(1);
        @Override
        public void send(Request request) {
            count++;
            if(count == 3){
                latch.countDown();
            }
        }

        @Override
        public List<Response> receive(long timeout, TimeUnit unit) throws InterruptedException {
            return Collections.singletonList(new Response(testRetry, 500, "error"));
        }

        @Override
        public void close() throws Exception {

        }
    }

    @Test
    public void test_retry() throws InterruptedException {
        MockPostManRetry postman = new MockPostManRetry();
        MockDispatcherRetry agentSender = new MockDispatcherRetry();
        var agent = new K2ViewAgent(postman, 60, agentSender);
        Thread thread = new Thread(()-> {
            try {
                agentSender.latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                agent.stop();
            }
        });
        thread.start();
        agent.run();
        assertEquals(3, agentSender.count);
        assertEquals(3, postman.count.get());
        assertEquals(2, testRetry.getTryCount());
    }
}
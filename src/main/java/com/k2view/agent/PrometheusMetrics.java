package com.k2view.agent;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;

import static com.k2view.agent.Utils.def;
import static com.k2view.agent.Utils.env;

import java.io.IOException;

public class PrometheusMetrics {
    private static PrometheusMetrics instance;
    private HTTPServer server;

    public static final Counter requestCounter = Counter.build()
        .name("requests_total")
        .help("Total number of processed requests")
        .register();
    
    public static final Counter failedRequestCounter = Counter.build()
        .name("failed_requests_total")
        .help("Total number of failed requests")
        .labelNames("taskId")
        .register();

    public static final Gauge activeRequests = Gauge.build()
        .name("active_requests")
        .help("Number of currently active requests")
        .register();

    public static final Histogram responseTime = Histogram.build()
        .name("response_time_seconds")
        .help("Time taken to process a request")
        .register();

    private static final boolean MONITOR_ENABLED = Boolean.parseBoolean((def(env("MONITOR"), "fasle")));

    private PrometheusMetrics() {
        if (!MONITOR_ENABLED) {
            System.out.println("Prometheus monitoring is disabled (MONITOR=false or not set).");
            return; // Skip initialization
        }

        try {
            server = new HTTPServer(9090); // Start Prometheus HTTP server
            System.out.println("Prometheus monitoring started on port 9090.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Prometheus server", e);
        }
    }

    public static synchronized PrometheusMetrics getInstance() {
        if (!MONITOR_ENABLED) {
            return null; // Return null if monitoring is disabled
        }

        if (instance == null) {
            instance = new PrometheusMetrics();
        }
        return instance;
    }

    public void stop() {
        if (server != null) {
            server.stop();
            System.out.println("Prometheus monitoring stopped.");
        }
    }
}

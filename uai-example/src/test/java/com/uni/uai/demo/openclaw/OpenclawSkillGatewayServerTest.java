package com.uni.uai.demo.openclaw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class OpenclawSkillGatewayServerTest {

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Test
    void financeHeuristicRoutesToReimbursement() throws Exception {
        int port = freePort();
        HttpServer server = OpenclawSkillGatewayServer.startGateway(new InetSocketAddress("127.0.0.1", port), false);
        try {
            String body = post(port, OpenclawSkillGatewayServer.PATH_FINANCE, "{\"query\":\"报销发票怎么贴\"}");
            assertTrue(body.contains("\"subIntent\":\"REIMBURSEMENT\""), body);
            assertTrue(body.contains("\"routing\":\"langchain4j-heuristic\""), body);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void analysisHeuristicRoutesToTrend() throws Exception {
        int port = freePort();
        HttpServer server = OpenclawSkillGatewayServer.startGateway(new InetSocketAddress("127.0.0.1", port), false);
        try {
            String body = post(port, OpenclawSkillGatewayServer.PATH_ANALYSIS, "{\"query\":\"Q3 同比环比趋势\"}");
            assertTrue(body.contains("\"subIntent\":\"TREND\""), body);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidJsonReturns400() throws Exception {
        int port = freePort();
        HttpServer server = OpenclawSkillGatewayServer.startGateway(new InetSocketAddress("127.0.0.1", port), false);
        try {
            HttpResponse<String> res = rawPost(port, OpenclawSkillGatewayServer.PATH_FINANCE, "{not-json");
            assertEquals(400, res.statusCode());
            assertTrue(res.body().contains("invalid JSON"), res.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void healthOk() throws Exception {
        int port = freePort();
        HttpServer server = OpenclawSkillGatewayServer.startGateway(new InetSocketAddress("127.0.0.1", port), false);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/health"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, res.statusCode());
            assertTrue(res.body().contains("up"), res.body());
        } finally {
            server.stop(0);
        }
    }

    private static String post(int port, String path, String json) throws Exception {
        HttpResponse<String> res = rawPost(port, path, json);
        assertEquals(200, res.statusCode(), res.body());
        return res.body();
    }

    private static HttpResponse<String> rawPost(int port, String path, String json) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

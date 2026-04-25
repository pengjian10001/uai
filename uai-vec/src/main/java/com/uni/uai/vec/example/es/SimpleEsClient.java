package com.uni.uai.vec.example.es;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SimpleEsClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public SimpleEsClient(String host, int port) {
        this.baseUrl = String.format("http://%s:%d", host, port);
        // 创建一个简单的 HTTP 客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 索引文档 (相当于 SQL 的 INSERT/UPDATE)
     * PUT /my_index/_doc/1
     */
    public void indexDocument(String index, String id, String jsonBody) throws IOException, InterruptedException {
        String url = String.format("%s/%s/_doc/%s", baseUrl, index, id);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.println("✅ 索引成功: " + response.body());
        } else {
            System.err.println("❌ 索引失败 (" + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * 搜索文档 (相当于 SQL 的 SELECT)
     * POST /my_index/_search
     */
    public void search(String index, String jsonQuery) throws IOException, InterruptedException {
        String url = String.format("%s/%s/_search", baseUrl, index);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonQuery))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("✅ 搜索结果:\n" + response.body());
        } else {
            System.err.println("❌ 搜索失败 (" + response.statusCode() + "): " + response.body());
        }
    }

    // --- 测试主方法 ---
    public static void main(String[] args) {
        // 假设 ES 运行在本地 9200 端口
        SimpleEsClient client = new SimpleEsClient("localhost", 9200);

        // 1. 准备数据 (JSON 格式)
        String myData = "{ \"user\": \"Alice\", \"message\": \"Hello JDK HttpClient\", \"timestamp\": \"2026-04-19\" }";

        try {
            // 2. 写入数据
            System.out.println("--- 正在写入数据 ---");
            client.indexDocument("my_test_index", "1", myData);

            // 3. 查询数据 (使用简单的 match_all 查询)
            System.out.println("\n--- 正在查询数据 ---");
            String query = "{ \"query\": { \"match_all\": {} } }";
            client.search("my_test_index", query);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}

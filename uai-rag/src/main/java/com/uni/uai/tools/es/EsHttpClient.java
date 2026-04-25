package com.uni.uai.tools.es;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.nio.charset.StandardCharsets;

/**
 * ES HTTP 客户端底层封装（仅依赖 Apache HttpClient）
 */
public class EsHttpClient {
    private final String esHost; // ES 服务地址（如 http://localhost:9200）
    private final CloseableHttpClient httpClient;

    public EsHttpClient(String esHost) {
        this.esHost = esHost;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * 发送 PUT 请求（创建索引/配置映射）
     */
    public String put(String path, String jsonBody) throws Exception {
        HttpPut put = new HttpPut(esHost + path);
        put.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (jsonBody != null && !jsonBody.isEmpty()) {
            put.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }
        return executeRequest(put);
    }

    /**
     * 发送 GET 请求（搜索/查询索引信息）
     */
    public String get(String path) throws Exception {
        HttpGet get = new HttpGet(esHost + path);
        get.setHeader("Content-Type", "application/json;charset=UTF-8");
        return executeRequest(get);
    }

    /**
     * 发送 POST 请求（测试分析器/搜索）
     */
    public String post(String path, String jsonBody) throws Exception {
        HttpPost post = new HttpPost(esHost + path);
        post.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (jsonBody != null && !jsonBody.isEmpty()) {
            post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }
        return executeRequest(post);
    }

    /**
     * 执行 HTTP 请求并返回响应
     */
    private String executeRequest(HttpUriRequest request) throws Exception {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }
            return "";
        }
    }

    /**
     * 关闭客户端
     */
    public void close() throws Exception {
        httpClient.close();
    }
}

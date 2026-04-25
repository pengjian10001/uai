package com.uni.uai.tools.es;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Apache HttpClient 调用 ES _analyze 端点工具类
 * 特点：不依赖 ES 官方客户端，纯 HTTP 协议交互，支持 HTTPS + ApiKey 认证
 */
public class ESAnalyzeHttpClientUtils {

    // 单例 HttpClient（复用连接池，提升性能）
    private static CloseableHttpClient httpClient;
    // Jackson 实例（解析 JSON 响应）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // ES _analyze 端点 URL（格式：https://host:port/_analyze）
    private static String analyzeUrl;

    /**
     * 初始化 HttpClient 和 ES 配置（项目启动时调用一次）
     *
     * @param esHost    ES 主机地址（如 localhost）
     * @param esPort    ES 端口（默认 9200）
     * @param apiKey    认证 ApiKey
     * @param verifySsl 是否验证 SSL 证书（本地/测试环境建议 false）
     * @throws NoSuchAlgorithmException 算法异常
     * @throws KeyStoreException        密钥库异常
     * @throws KeyManagementException   密钥管理异常
     */
    public static void init(String esHost, int esPort, String apiKey, boolean verifySsl)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // 1. 构建 _analyze 端点 URL
        analyzeUrl = String.format("https://%s:%d/_analyze", esHost, esPort);

        // 2. 配置 SSL（跳过证书验证，适用于本地环境）
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial((chain, authType) -> true) // 信任所有证书
                .build();

        // 3. SSL 连接工厂（禁用主机名验证）
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );

        // 4. 配置请求超时（连接超时 5s，响应超时 10s）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .build();

        // 5. 构建 HttpClient（复用连接池，添加默认 ApiKey 头）
        httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultRequestConfig(requestConfig)
                // 添加全局 ApiKey 认证头
                .setDefaultHeaders(List.of(
                        new org.apache.http.message.BasicHeader(
                                "Authorization", "ApiKey " + apiKey
                        ),
                        new org.apache.http.message.BasicHeader(
                                "Content-Type", "application/json;charset=UTF-8"
                        )
                ))
                .build();
    }

    /**
     * 基础分词：指定全局分词器（如 ik_max_word、ik_smart、standard）
     *
     * @param text     待分词文本
     * @param analyzer 分词器名称
     * @return 分词结果列表（封装词、类型、位置等信息）
     * @throws IOException 网络请求/JSON 解析异常
     */
    public static List<TokenResult> analyzeText(String text, String analyzer) throws IOException {
        // 构建请求体 JSON
        String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                "text", text,
                "analyzer", analyzer
        ));
        // 发送请求并解析结果
        return executeRequest(requestBody);
    }

    /**
     * 进阶分词：自定义分词链（字符过滤器 + 分词器 + 令牌过滤器）
     * 示例场景：过滤特殊字符、转小写、去除停用词等
     *
     * @param text         待分词文本
     * @param tokenizer    令牌化器（如 ik_max_word、standard）
     * @param charFilters  字符过滤器（可选，如 mapping、html_strip）
     * @param tokenFilters 令牌过滤器（可选，如 lowercase、stop）
     * @return 分词结果列表
     * @throws IOException 网络请求/JSON 解析异常
     */
    public static List<TokenResult> analyzeTextWithCustomChain(
            String text,
            String tokenizer,
            List<Map<String, Object>> charFilters,
            List<Map<String, Object>> tokenFilters) throws IOException {

        // 构建请求体（支持自定义分词链）
        Map<String, Object> requestBodyMap = Map.of(
                "text", text,
                "tokenizer", tokenizer
        );
        // 添加字符过滤器（如 mapping 替换特殊字符）
        if (charFilters != null && !charFilters.isEmpty()) {
            requestBodyMap.put("char_filter", charFilters);
        }
        // 添加令牌过滤器（如 lowercase 转小写）
        if (tokenFilters != null && !tokenFilters.isEmpty()) {
            requestBodyMap.put("filter", tokenFilters);
        }

        // 序列化请求体为 JSON
        String requestBody = OBJECT_MAPPER.writeValueAsString(requestBodyMap);
        // 发送请求并解析结果
        return executeRequest(requestBody);
    }

    /**
     * 核心方法：发送 HTTP 请求到 _analyze 端点并解析响应
     */
    private static List<TokenResult> executeRequest(String requestBody) throws IOException {
        if (httpClient == null || analyzeUrl == null) {
            throw new IllegalStateException("工具类未初始化，请先调用 init 方法");
        }

        // 1. 创建 HTTP GET 请求（_analyze 支持 GET/POST，这里用 GET 与之前 curl 保持一致）
        HttpGet httpGet = new HttpGet(analyzeUrl);

        // 2. 设置请求体（GET 请求也可携带请求体，ES 支持）
        HttpEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
        //httpGet.setEntity(entity);

        // 3. 执行请求并处理响应
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            // 4. 检查响应状态码（200 表示成功）
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                String errorMsg = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new IOException(String.format("ES 分词请求失败，状态码：%d，错误信息：%s", statusCode, errorMsg));
            }

            // 5. 解析响应体为 TokenResult 列表
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            Map<String, Object> responseMap = OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {});
            List<Map<String, Object>> tokens = (List<Map<String, Object>>) responseMap.get("tokens");

            // 6. 转换为自定义 TokenResult 实体类
            List<TokenResult> tokenResults = new ArrayList<>();
            for (Map<String, Object> tokenMap : tokens) {
                TokenResult tokenResult = new TokenResult();
                tokenResult.setToken((String) tokenMap.get("token"));
                tokenResult.setType((String) tokenMap.get("type"));
                tokenResult.setPosition(((Number) tokenMap.get("position")).intValue());
                tokenResult.setStartOffset(((Number) tokenMap.get("start_offset")).intValue());
                tokenResult.setEndOffset(((Number) tokenMap.get("end_offset")).intValue());
                tokenResults.add(tokenResult);
            }
            return tokenResults;
        } finally {
            // 释放资源（HttpGet 无需手动关闭，try-with-resources 会处理 response）
            httpGet.releaseConnection();
        }
    }

    /**
     * 关闭 HttpClient（项目关闭时调用，释放连接池资源）
     *
     * @throws IOException 关闭异常
     */
    public static void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * 分词结果封装类（与 ES 响应字段对应）
     */
    public static class TokenResult {
        private String token;       // 分词后的词
        private String type;        // 词类型（如 CN_WORD、ENGLISH、NUMERIC、<IDEOGRAPHIC>）
        private int position;       // 在原文中的位置
        private int startOffset;    // 起始偏移量（字符索引）
        private int endOffset;      // 结束偏移量（字符索引）

        // Getter + Setter
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public int getStartOffset() { return startOffset; }
        public void setStartOffset(int startOffset) { this.startOffset = startOffset; }
        public int getEndOffset() { return endOffset; }
        public void setEndOffset(int endOffset) { this.endOffset = endOffset; }

        @Override
        public String toString() {
            return "TokenResult{" +
                    "token='" + token + '\'' +
                    ", type='" + type + '\'' +
                    ", position=" + position +
                    ", startOffset=" + startOffset +
                    ", endOffset=" + endOffset +
                    '}';
        }
    }
}

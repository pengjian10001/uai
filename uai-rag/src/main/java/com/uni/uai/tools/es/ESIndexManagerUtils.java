package com.uni.uai.tools.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 索引管理工具类（JDK 8 及以下兼容版）
 * 支持：创建/删除/查询索引、修改映射、别名管理、状态检查、设置副本/分片等
 */
public class ESIndexManagerUtils {

    // 单例 HttpClient（复用连接池）
    private static CloseableHttpClient httpClient;
    // Jackson 实例（JSON 序列化/反序列化）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // ES 基础 URL
    private static String esBaseUrl;

    /**
     * 初始化 HttpClient 和 ES 配置（项目启动时调用一次）
     *
     * @param esHost    ES 主机地址（如 localhost）
     * @param esPort    ES 端口（默认 9200）
     * @param apiKey    认证 ApiKey
     * @param verifySsl 是否验证 SSL 证书（本地环境建议 false）
     * @throws NoSuchAlgorithmException 算法异常
     * @throws KeyStoreException        密钥库异常
     * @throws KeyManagementException   密钥管理异常
     */
    public static void init(String esHost, int esPort, String apiKey, boolean verifySsl)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // 1. 构建 ES 基础 URL
        esBaseUrl = String.format("https://%s:%d", esHost, esPort);

        // 2. 配置 SSL（JDK 8 兼容写法）
        SSLContext sslContext = null;
        if (verifySsl) {
            sslContext = SSLContext.getDefault();
        } 

        // 3. SSL 连接工厂
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE
        );

        // 4. 请求超时配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(30000) // 索引操作超时时间更长
                .build();

        // 5. 默认请求头（JDK 8 兼容）
        List<org.apache.http.Header> defaultHeaders = new ArrayList<org.apache.http.Header>();
        defaultHeaders.add(new org.apache.http.message.BasicHeader(
                "Authorization", "ApiKey " + apiKey
        ));
        defaultHeaders.add(new org.apache.http.message.BasicHeader(
                "Content-Type", "application/json;charset=UTF-8"
        ));

        // 6. 构建 HttpClient
        httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultHeaders(defaultHeaders)
                .build();
    }

    // ======================== 索引基础操作 ========================

    /**
     * 创建索引（支持自定义分片、副本、映射）
     *
     * @param indexName  索引名
     * @param shards     主分片数（默认 1）
     * @param replicas   副本数（默认 1）
     * @param mappings   索引映射（JSON 格式的 Map，可为 null）
     * @return true=创建成功，false=创建失败
     * @throws IOException 网络/解析异常
     */
    public static boolean createIndex(String indexName, int shards, int replicas, Map<String, Object> mappings) throws IOException {
        if (isIndexExists(indexName)) {
            System.out.println("索引 " + indexName + " 已存在，无需创建");
            return false;
        }

        // 构建创建索引的请求体
        Map<String, Object> requestBody = new HashMap<String, Object>();
        // 设置分片和副本
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("number_of_shards", shards);
        settings.put("number_of_replicas", replicas);
        requestBody.put("settings", settings);

        // 设置映射
        if (mappings != null && !mappings.isEmpty()) {
            requestBody.put("mappings", mappings);
        }

        String jsonBody = OBJECT_MAPPER.writeValueAsString(requestBody);
        String url = esBaseUrl + "/" + indexName;

        // 发送 PUT 请求创建索引
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                System.out.println("索引 " + indexName + " 创建成功");
                return true;
            } else {
                String errorMsg = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.err.println("创建索引失败：" + errorMsg);
                return false;
            }
        } finally {
            closeResponse(response);
            httpPut.releaseConnection();
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名
     * @return true=删除成功，false=删除失败/索引不存在
     * @throws IOException 网络异常
     */
    public static boolean deleteIndex(String indexName) throws IOException {
        if (!isIndexExists(indexName)) {
            System.out.println("索引 " + indexName + " 不存在，无需删除");
            return false;
        }

        String url = esBaseUrl + "/" + indexName;
        HttpDelete httpDelete = new HttpDelete(url);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpDelete);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("索引 " + indexName + " 删除成功");
                return true;
            } else {
                String errorMsg = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.err.println("删除索引失败：" + errorMsg);
                return false;
            }
        } finally {
            closeResponse(response);
            httpDelete.releaseConnection();
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param indexName 索引名
     * @return true=存在，false=不存在
     * @throws IOException 网络异常
     */
    public static boolean isIndexExists(String indexName) throws IOException {
        String url = esBaseUrl + "/" + indexName;
        HttpHead httpHead = new HttpHead(url);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpHead);
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode == 200; // 200=存在，404=不存在
        } finally {
            closeResponse(response);
            httpHead.releaseConnection();
        }
    }

    /**
     * 获取索引信息（设置、映射等）
     *
     * @param indexName 索引名
     * @return 索引信息 Map，null=索引不存在
     * @throws IOException 网络/解析异常
     */
    public static Map<String, Object> getIndexInfo(String indexName) throws IOException {
        if (!isIndexExists(indexName)) {
            return null;
        }

        String url = esBaseUrl + "/" + indexName;
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(responseBody, new HashMap<String, Object>().getClass());
        } finally {
            closeResponse(response);
            httpGet.releaseConnection();
        }
    }

    // ======================== 映射管理 ========================

    /**
     * 新增/修改索引映射（新增字段）
     *
     * @param indexName 索引名
     * @param mappings  新增的映射（如：{"properties":{"content":{"type":"text","analyzer":"ik_max_word"}}}）
     * @return true=修改成功，false=修改失败
     * @throws IOException 网络/解析异常
     */
    public static boolean updateMapping(String indexName, Map<String, Object> mappings) throws IOException {
        if (!isIndexExists(indexName)) {
            System.err.println("索引 " + indexName + " 不存在，无法修改映射");
            return false;
        }

        String url = esBaseUrl + "/" + indexName + "/_mapping";
        HttpPut httpPut = new HttpPut(url);
        String jsonBody = OBJECT_MAPPER.writeValueAsString(mappings);
        httpPut.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("索引 " + indexName + " 映射修改成功");
                return true;
            } else {
                String errorMsg = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.err.println("修改映射失败：" + errorMsg);
                return false;
            }
        } finally {
            closeResponse(response);
            httpPut.releaseConnection();
        }
    }

    // ======================== 别名管理 ========================

    /**
     * 给索引添加别名
     *
     * @param indexName  索引名
     * @param aliasName  别名
     * @param isReplace  是否替换已存在的别名（true=覆盖，false=不覆盖）
     * @return true=添加成功，false=添加失败
     * @throws IOException 网络/解析异常
     */
    public static boolean addAlias(String indexName, String aliasName, boolean isReplace) throws IOException {
        if (!isIndexExists(indexName)) {
            System.err.println("索引 " + indexName + " 不存在，无法添加别名");
            return false;
        }

        // 先检查别名是否已存在
        if (isAliasExists(aliasName) && !isReplace) {
            System.out.println("别名 " + aliasName + " 已存在，且不允许替换");
            return false;
        }

        // 构建别名请求体
        Map<String, Object> requestBody = new HashMap<String, Object>();
        Map<String, Object> actions = new HashMap<String, Object>();
        if (isReplace && isAliasExists(aliasName)) {
            // 删除原有别名
            Map<String, Object> removeAction = new HashMap<String, Object>();
            removeAction.put("remove", new HashMap<String, Object>() {{
                put("alias", aliasName);
                put("index", "*"); // 匹配所有索引
            }});
            requestBody.put("actions", new ArrayList<Object>() {{
                add(removeAction);
            }});
        }

        // 添加新别名
        Map<String, Object> addAction = new HashMap<String, Object>();
        addAction.put("add", new HashMap<String, Object>() {{
            put("index", indexName);
            put("alias", aliasName);
        }});
        
        List<Object> actionList = (List<Object>) requestBody.get("actions");
        if (actionList == null) {
            actionList = new ArrayList<Object>();
            requestBody.put("actions", actionList);
        }
        actionList.add(addAction);

        String url = esBaseUrl + "/_aliases";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(requestBody), StandardCharsets.UTF_8));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("索引 " + indexName + " 别名 " + aliasName + " 添加成功");
                return true;
            } else {
                String errorMsg = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.err.println("添加别名失败：" + errorMsg);
                return false;
            }
        } finally {
            closeResponse(response);
            httpPost.releaseConnection();
        }
    }

    /**
     * 检查别名是否存在
     *
     * @param aliasName 别名
     * @return true=存在，false=不存在
     * @throws IOException 网络异常
     */
    public static boolean isAliasExists(String aliasName) throws IOException {
        String url = esBaseUrl + "/_alias/" + aliasName;
        HttpHead httpHead = new HttpHead(url);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpHead);
            return response.getStatusLine().getStatusCode() == 200;
        } finally {
            closeResponse(response);
            httpHead.releaseConnection();
        }
    }

    // ======================== 索引状态管理 ========================

    /**
     * 获取索引健康状态（green/yellow/red）
     *
     * @param indexName 索引名（* 表示所有索引）
     * @return 健康状态字符串，null=获取失败
     * @throws IOException 网络/解析异常
     */
    public static String getIndexHealth(String indexName) throws IOException {
        String url = esBaseUrl + "/_cat/health/" + indexName + "?format=json";
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            List<Map<String, Object>> healthList = OBJECT_MAPPER.readValue(responseBody, List.class);
            if (!healthList.isEmpty()) {
                return (String) healthList.get(0).get("status");
            }
            return null;
        } finally {
            closeResponse(response);
            httpGet.releaseConnection();
        }
    }

    /**
     * 关闭 HttpClient（项目关闭时调用）
     *
     * @throws IOException 关闭异常
     */
    public static void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 关闭响应流（JDK 8 兼容）
     */
    private static void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

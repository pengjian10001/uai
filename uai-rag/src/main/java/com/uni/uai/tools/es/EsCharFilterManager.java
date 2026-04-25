package com.uni.uai.tools.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 字符过滤器配置管理工具
 */
public class EsCharFilterManager {
    private final EsHttpClient esHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EsCharFilterManager(String esHost) {
        this.esHttpClient = new EsHttpClient(esHost);
    }

    /**
     * 1. 创建带字符过滤器的索引
     */
    public String createIndexWithCharFilter(EsIndexConfig indexConfig) throws Exception {
        String json = indexConfig.toCreateIndexJson();
        String path = indexConfig.getIndexName();
        return esHttpClient.put(path, json);
    }

    /**
     * 2. 测试自定义分析器（调用 _analyze 端点）
     */
    public String testAnalyzer(String indexName, String text, String analyzerName) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        if (analyzerName != null) {
            request.put("analyzer", analyzerName);
        }
        String json = objectMapper.writeValueAsString(request);
        String path = indexName + "/_analyze";
        return esHttpClient.post(path, json);
    }

    /**
     * 3. 直接测试字符过滤器（无索引，调用全局 _analyze）
     */
    public String testCharFilterDirectly(String text, CharFilterConfig charFilter) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        request.put("char_filter", charFilter.toMap());
        request.put("tokenizer", "keyword"); // 仅测试字符过滤，用 keyword 分词器
        String json = objectMapper.writeValueAsString(request);
        return esHttpClient.post("_analyze", json);
    }

    /**
     * 关闭 HTTP 客户端
     */
    public void close() throws Exception {
        esHttpClient.close();
    }
}

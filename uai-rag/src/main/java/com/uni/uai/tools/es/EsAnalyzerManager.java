package com.uni.uai.tools.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 分析器配置管理工具（业务层）
 */
public class EsAnalyzerManager {
    private final EsHttpClient esHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EsAnalyzerManager(String esHost) {
        this.esHttpClient = new EsHttpClient(esHost);
    }

    /**
     * 1. 创建索引并配置分析器（字段层级 + 索引层级）
     */
    public String createIndexWithAnalyzer(EsAnalyzerConfig config) throws Exception {
        String json = config.toCreateIndexJson();
        String path = "/" + config.getIndexName();
        return esHttpClient.put(path, json);
    }

    /**
     * 2. 测试分析器（调用 _analyze 端点）
     */
    public String testAnalyzer(String indexName, String text, String analyzer) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        if (analyzer != null) {
            request.put("analyzer", analyzer);
        }
        String json = objectMapper.writeValueAsString(request);
        String path = "/" + indexName + "/_analyze";
        return esHttpClient.post(path, json);
    }

    /**
     * 3. 搜索时指定分析器（match 查询）
     */
    public String searchWithAnalyzer(String indexName, String fieldName, String queryText, String analyzer) throws Exception {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> match = new HashMap<>();
        Map<String, Object> fieldQuery = new HashMap<>();
        fieldQuery.put("query", queryText);
        if (analyzer != null) {
            fieldQuery.put("analyzer", analyzer);
        }
        match.put(fieldName, fieldQuery);
        request.put("query", match);
        
        String json = objectMapper.writeValueAsString(request);
        String path = "/" + indexName + "/_search";
        return esHttpClient.post(path, json);
    }

    /**
     * 关闭 HTTP 客户端
     */
    public void close() throws Exception {
        esHttpClient.close();
    }
}

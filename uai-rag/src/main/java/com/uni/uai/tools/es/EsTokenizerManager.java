package com.uni.uai.tools.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 分词器配置管理工具
 */
public class EsTokenizerManager {
    private final EsHttpClient esHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EsTokenizerManager(String esHost) {
        this.esHttpClient = new EsHttpClient(esHost);
    }

    /**
     * 1. 创建带自定义分词器的索引
     */
    public String createIndexWithTokenizer(EsIndexConfig indexConfig) throws Exception {
        String json = indexConfig.toCreateIndexJson();
        String path = indexConfig.getIndexName();
        return esHttpClient.put(path, json);
    }

    /**
     * 2. 测试分词器（调用 _analyze 端点，支持直接指定分词器或分析器）
     * @param indexName 索引名（可为空，使用全局 _analyze）
     * @param text 测试文本
     * @param analyzerName 分析器名称（优先级高于 tokenizerName）
     * @param tokenizerName 分词器名称
     */
    public String testTokenizer(String indexName, String text, String analyzerName, String tokenizerName) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        if (analyzerName != null && !analyzerName.isEmpty()) {
            request.put("analyzer", analyzerName);
        } else if (tokenizerName != null && !tokenizerName.isEmpty()) {
            request.put("tokenizer", tokenizerName);
        }
        String json = objectMapper.writeValueAsString(request);
        
        String path = (indexName == null || indexName.isEmpty()) 
                ? "_analyze" 
                : indexName + "/_analyze";
        return esHttpClient.post(path, json);
    }

    /**
     * 关闭 HTTP 客户端
     */
    public void close() throws Exception {
        esHttpClient.close();
    }
}

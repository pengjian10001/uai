package com.uni.uai.tools.es;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 分析器配置模型（封装索引/字段/搜索分析器配置）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsAnalyzerConfig {
    // 索引名称
    private String indexName;
    // 索引层级默认分析器配置（settings -> analysis -> analyzer）
    private Map<String, Map<String, Object>> indexAnalyzers = new HashMap<>();
    // 索引层级默认搜索分析器名称
    private String defaultSearchAnalyzer;
    // 字段映射配置（mappings -> properties）
    private Map<String, FieldConfig> fieldMappings = new HashMap<>();

    // 字段级配置（包含索引分析器、搜索分析器）
    public static class FieldConfig {
        private String type = "text";
        private String analyzer; // 索引时分析器
        private String searchAnalyzer; // 搜索时分析器
        // 多字段配置（如 description.my）
        private Map<String, FieldConfig> fields;

        // 构造器 & getter/setter
        public FieldConfig() {}
        public FieldConfig(String analyzer) {
            this.analyzer = analyzer;
        }
        public FieldConfig(String analyzer, String searchAnalyzer) {
            this.analyzer = analyzer;
            this.searchAnalyzer = searchAnalyzer;
        }

        // getter/setter 省略，JDK8 需手动生成
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getAnalyzer() { return analyzer; }
        public void setAnalyzer(String analyzer) { this.analyzer = analyzer; }
        public String getSearchAnalyzer() { return searchAnalyzer; }
        public void setSearchAnalyzer(String searchAnalyzer) { this.searchAnalyzer = searchAnalyzer; }
        public Map<String, FieldConfig> getFields() { return fields; }
        public void setFields(Map<String, FieldConfig> fields) { this.fields = fields; }
    }

    // 添加索引层级默认分析器（如 default: keyword）
    public void addIndexDefaultAnalyzer(String analyzerName, String analyzerType) {
        Map<String, Object> analyzer = new HashMap<>();
        analyzer.put("type", analyzerType);
        indexAnalyzers.put(analyzerName, analyzer);
    }

    // 添加字段级配置
    public void addFieldConfig(String fieldName, FieldConfig fieldConfig) {
        fieldMappings.put(fieldName, fieldConfig);
    }

    // 转换为 ES 创建索引的 JSON 字符串
    public String toCreateIndexJson() throws Exception {
        Map<String, Object> root = new HashMap<>();
        
        // 1. 构建 settings 部分
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> analysis = new HashMap<>();
        Map<String, Object> analyzers = new HashMap<>();
        analyzers.putAll(indexAnalyzers);
        // 设置默认分析器（default）和默认搜索分析器（default_search）
        if (!indexAnalyzers.containsKey("default")) {
            analyzers.put("default", indexAnalyzers.getOrDefault("standard", 
                    new HashMap<String, Object>() {{ put("type", "standard"); }}));
        }
        if (defaultSearchAnalyzer != null) {
            analyzers.put("default_search", new HashMap<String, Object>() {{ 
                put("type", defaultSearchAnalyzer); 
            }});
        }
        analysis.put("analyzer", analyzers);
        settings.put("analysis", analysis);
        root.put("settings", settings);
        
        // 2. 构建 mappings 部分
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("properties", fieldMappings);
        root.put("mappings", mappings);
        
        // 3. 序列化 JSON
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(root);
    }

    // getter/setter 省略
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public Map<String, Map<String, Object>> getIndexAnalyzers() { return indexAnalyzers; }
    public void setIndexAnalyzers(Map<String, Map<String, Object>> indexAnalyzers) { this.indexAnalyzers = indexAnalyzers; }
    public String getDefaultSearchAnalyzer() { return defaultSearchAnalyzer; }
    public void setDefaultSearchAnalyzer(String defaultSearchAnalyzer) { this.defaultSearchAnalyzer = defaultSearchAnalyzer; }
    public Map<String, FieldConfig> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(Map<String, FieldConfig> fieldMappings) { this.fieldMappings = fieldMappings; }
}

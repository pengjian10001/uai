package com.uni.uai.tools.es;

import java.util.HashMap;
import java.util.Map;

public class EsAnalyzerDemo {
    public static void main(String[] args) throws Exception {
        // 1. 初始化工具（ES 服务地址）
        EsAnalyzerManager manager = new EsAnalyzerManager("http://localhost:9200");

        // ==================== 场景1：字段层级分析器配置 ====================
        EsAnalyzerConfig fieldLevelConfig = new EsAnalyzerConfig();
        fieldLevelConfig.setIndexName("authors_with_field_level_analyzers");
        
        // 配置字段：name（默认standard）、about（english）、description（多字段+fingerprint）
        fieldLevelConfig.addFieldConfig("name", new EsAnalyzerConfig.FieldConfig()); // 继承默认
        fieldLevelConfig.addFieldConfig("about", new EsAnalyzerConfig.FieldConfig("english"));
        
        // 多字段配置：description.my 使用 fingerprint 分析器
        EsAnalyzerConfig.FieldConfig descriptionConfig = new EsAnalyzerConfig.FieldConfig();
        Map<String, EsAnalyzerConfig.FieldConfig> multiFields = new HashMap<>();
        multiFields.put("my", new EsAnalyzerConfig.FieldConfig("fingerprint"));
        descriptionConfig.setFields(multiFields);
        fieldLevelConfig.addFieldConfig("description", descriptionConfig);
        
        // 创建索引
        String createResult1 = manager.createIndexWithAnalyzer(fieldLevelConfig);
        System.out.println("字段层级分析器创建结果：" + createResult1);

        // ==================== 场景2：索引层级默认分析器配置 ====================
        EsAnalyzerConfig indexLevelConfig = new EsAnalyzerConfig();
        indexLevelConfig.setIndexName("authors_with_default_analyzer");
        // 设置索引默认分析器为 keyword
        indexLevelConfig.addIndexDefaultAnalyzer("default", "keyword");
        // 创建索引
        String createResult2 = manager.createIndexWithAnalyzer(indexLevelConfig);
        System.out.println("索引层级默认分析器创建结果：" + createResult2);
        // 测试默认分析器（JohnDoe 会被解析为单个 token）
        String testResult = manager.testAnalyzer("authors_with_default_analyzer", "JohnDoe", null);
        System.out.println("默认分析器测试结果：" + testResult);

        // ==================== 场景3：字段层级搜索分析器 ====================
        EsAnalyzerConfig searchAnalyzerConfig = new EsAnalyzerConfig();
        searchAnalyzerConfig.setIndexName("authors_with_search_analyzer");
        // author_name：索引时 stop 分析器，搜索时 simple 分析器
        searchAnalyzerConfig.addFieldConfig("author_name", 
                new EsAnalyzerConfig.FieldConfig("stop", "simple"));
        // 创建索引
        String createResult3 = manager.createIndexWithAnalyzer(searchAnalyzerConfig);
        System.out.println("搜索分析器创建结果：" + createResult3);
        // 搜索时指定分析器（覆盖字段配置）
        String searchResult = manager.searchWithAnalyzer(
                "authors_with_search_analyzer", "author_name", "MKonda", "simple");
        System.out.println("搜索结果（指定simple分析器）：" + searchResult);

        // ==================== 场景4：索引层级默认搜索分析器 ====================
        EsAnalyzerConfig defaultSearchConfig = new EsAnalyzerConfig();
        defaultSearchConfig.setIndexName("authors_with_default_search_analyzer");
        // 索引默认分析器：standard，搜索默认分析器：simple
        defaultSearchConfig.addIndexDefaultAnalyzer("default", "standard");
        defaultSearchConfig.setDefaultSearchAnalyzer("simple");
        // 创建索引
        String createResult4 = manager.createIndexWithAnalyzer(defaultSearchConfig);
        System.out.println("默认搜索分析器创建结果：" + createResult4);

        // 关闭资源
        manager.close();
    }
}

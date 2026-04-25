package com.uni.uai.tools.es;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符过滤器类型枚举
 */
enum CharFilterType {
    HTML_STRIP("html_strip"),
    MAPPING("mapping"),
    PATTERN_REPLACE("pattern_replace");

    private final String type;
    CharFilterType(String type) { this.type = type; }
    public String getType() { return type; }
}

/**
 * 字符过滤器配置模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharFilterConfig {
    private String name; // 过滤器名称（如 my_html_strip_filter）
    private CharFilterType type; // 过滤器类型
    private List<String> escapedTags; // html_strip 专用：忽略的标签（如 ["h1"]）
    private List<String> mappings; // mapping 专用：映射规则（如 ["UK=>UnitedKingdom"]）
    private String mappingsPath; // mapping 专用：外部映射文件路径（如 secret_organizations.txt）
    private String pattern; // pattern_replace 专用：正则表达式
    private String replacement; // pattern_replace 专用：替换值

    // 构造器（按需重载）
    public CharFilterConfig(String name, CharFilterType type) {
        this.name = name;
        this.type = type;
    }

    // Getter & Setter（JDK8 需手动生成）
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CharFilterType getType() { return type; }
    public void setType(CharFilterType type) { this.type = type; }
    public List<String> getEscapedTags() { return escapedTags; }
    public void setEscapedTags(List<String> escapedTags) { this.escapedTags = escapedTags; }
    public List<String> getMappings() { return mappings; }
    public void setMappings(List<String> mappings) { this.mappings = mappings; }
    public String getMappingsPath() { return mappingsPath; }
    public void setMappingsPath(String mappingsPath) { this.mappingsPath = mappingsPath; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getReplacement() { return replacement; }
    public void setReplacement(String replacement) { this.replacement = replacement; }

    // 转换为 ES 配置的 Map 结构
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.getType());
        if (escapedTags != null) map.put("escaped_tags", escapedTags);
        if (mappings != null) map.put("mappings", mappings);
        if (mappingsPath != null) map.put("mappings_path", mappingsPath);
        if (pattern != null) map.put("pattern", pattern);
        if (replacement != null) map.put("replacement", replacement);
        return map;
    }
}

/**
 * 自定义分析器配置模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class AnalyzerConfig {
    private String name; // 分析器名称（如 custom_html_strip_filter_analyzer）
    private String tokenizer; // 分词器（如 keyword）
    private List<String> charFilterNames; // 关联的字符过滤器名称列表

    public AnalyzerConfig(String name, String tokenizer, List<String> charFilterNames) {
        this.name = name;
        this.tokenizer = tokenizer;
        this.charFilterNames = charFilterNames;
    }

    // 转换为 ES 配置的 Map 结构
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tokenizer", tokenizer);
        if (charFilterNames != null && !charFilterNames.isEmpty()) {
            map.put("char_filter", charFilterNames);
        }
        return map;
    }

    // Getter & Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTokenizer() { return tokenizer; }
    public void setTokenizer(String tokenizer) { this.tokenizer = tokenizer; }
    public List<String> getCharFilterNames() { return charFilterNames; }
    public void setCharFilterNames(List<String> charFilterNames) { this.charFilterNames = charFilterNames; }
}

/**
 * 索引配置模型（整合字符过滤器 + 分析器）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class EsIndexConfig {
    private String indexName;
    private List<CharFilterConfig> charFilters = new ArrayList<>();
    private List<AnalyzerConfig> analyzers = new ArrayList<>();

    // 添加字符过滤器
    public void addCharFilter(CharFilterConfig charFilter) {
        this.charFilters.add(charFilter);
    }

    // 添加分析器
    public void addAnalyzer(AnalyzerConfig analyzer) {
        this.analyzers.add(analyzer);
    }

    // 转换为创建索引的 JSON 字符串
    public String toCreateIndexJson() throws Exception {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> analysis = new HashMap<>();

        // 1. 构建字符过滤器配置
        Map<String, Object> charFilterMap = new HashMap<>();
        for (CharFilterConfig cfc : charFilters) {
            charFilterMap.put(cfc.getName(), cfc.toMap());
        }
        if (!charFilterMap.isEmpty()) {
            analysis.put("char_filter", charFilterMap);
        }

        // 2. 构建分析器配置
        Map<String, Object> analyzerMap = new HashMap<>();
        for (AnalyzerConfig ac : analyzers) {
            analyzerMap.put(ac.getName(), ac.toMap());
        }
        if (!analyzerMap.isEmpty()) {
            analysis.put("analyzer", analyzerMap);
        }

        // 3. 组装 settings
        settings.put("analysis", analysis);
        root.put("settings", settings);

        // 4. 序列化 JSON
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(root);
    }

    // Getter & Setter
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public List<CharFilterConfig> getCharFilters() { return charFilters; }
    public void setCharFilters(List<CharFilterConfig> charFilters) { this.charFilters = charFilters; }
    public List<AnalyzerConfig> getAnalyzers() { return analyzers; }
    public void setAnalyzers(List<AnalyzerConfig> analyzers) { this.analyzers = analyzers; }
}
package com.uni.uai.tools.es;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分词器类型枚举（覆盖示例中所有核心分词器）
 */
enum TokenizerType {
    STANDARD("standard"),
    NGRAM("ngram"),
    EDGE_NGRAM("edge_ngram"),
    PATTERN("pattern"),
    WHITESPACE("whitespace"),
    KEYWORD("keyword"),
    LOWERCASE("lowercase"),
    UAX_URL_EMAIL("uax_url_email"),
    PATH_HIERARCHY("path_hierarchy");

    private final String type;
    TokenizerType(String type) { this.type = type; }
    public String getType() { return type; }
}

/**
 * 分词器配置模型（封装所有分词器的可配置参数）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenizerConfig {
    private String name; // 分词器名称（如 custom_standard_tokenizer）
    private TokenizerType type; // 分词器类型
    // standard 分词器参数
    private Integer maxTokenLength; // 最大词元长度（默认 255）
    // ngram/edge_ngram 分词器参数
    private Integer minGram; // 最小 n-gram 长度
    private Integer maxGram; // 最大 n-gram 长度
    private List<String> tokenChars; // 保留的字符类型（如 ["letter", "digit"]）
    // pattern 分词器参数
    private String pattern; // 正则表达式（默认 \W+）
    private Boolean flags; // 正则标志（可选）
    private String group; // 正则分组（可选）

    // 构造器（按需重载）
    public TokenizerConfig(String name, TokenizerType type) {
        this.name = name;
        this.type = type;
    }

    // 转换为 ES 配置的 Map 结构
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.getType());
        
        // standard 分词器参数
        if (maxTokenLength != null) map.put("max_token_length", maxTokenLength);
        // ngram/edge_ngram 分词器参数
        if (minGram != null) map.put("min_gram", minGram);
        if (maxGram != null) map.put("max_gram", maxGram);
        if (tokenChars != null && !tokenChars.isEmpty()) map.put("token_chars", tokenChars);
        // pattern 分词器参数
        if (pattern != null) map.put("pattern", pattern);
        if (flags != null) map.put("flags", flags);
        if (group != null) map.put("group", group);
        
        return map;
    }

    // Getter & Setter（JDK8 需手动生成）
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TokenizerType getType() { return type; }
    public void setType(TokenizerType type) { this.type = type; }
    public Integer getMaxTokenLength() { return maxTokenLength; }
    public void setMaxTokenLength(Integer maxTokenLength) { this.maxTokenLength = maxTokenLength; }
    public Integer getMinGram() { return minGram; }
    public void setMinGram(Integer minGram) { this.minGram = minGram; }
    public Integer getMaxGram() { return maxGram; }
    public void setMaxGram(Integer maxGram) { this.maxGram = maxGram; }
    public List<String> getTokenChars() { return tokenChars; }
    public void setTokenChars(List<String> tokenChars) { this.tokenChars = tokenChars; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public Boolean getFlags() { return flags; }
    public void setFlags(Boolean flags) { this.flags = flags; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
}




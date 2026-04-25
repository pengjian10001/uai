package com.uni.uai.tools.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESAnalyzeDemo {
    public static void main(String[] args) {
        // ES 连接配置（替换为你的实际配置）
        String esHost = "localhost";
        int esPort = 9200;
        String apiKey = "OUp2TFZKZ0JLc3QzLWpXOGU4Vlk6UzhQUTREbXFSSUN2bFJVWm5QNVNOZw==";

        try {
            // 1. 初始化工具类（项目启动时调用一次）
            ESAnalyzeHttpClientUtils.init(esHost, esPort, apiKey, false);

            // 2. 基础用法：使用 ik_max_word 分词器（最细粒度拆分）
            String text1 = "特工詹姆斯·邦德007";
            List<ESAnalyzeHttpClientUtils.TokenResult> basicResult = 
                    ESAnalyzeHttpClientUtils.analyzeText(text1, "ik_max_word");
            
            System.out.println("=== 基础分词结果（ik_max_word）===");
            basicResult.forEach(System.out::println);

            // 3. 进阶用法：自定义分词链（过滤特殊字符 + 转小写）
            String text2 = "特工詹姆斯·邦德007！Hello 世界";
            
            // 字符过滤器：替换 "·" 和 "！" 为空（去除特殊字符）
            List<Map<String, Object>> charFilters = new ArrayList<>();
            Map<String, Object> mappingFilter = new HashMap<>();
            mappingFilter.put("type", "mapping");
            mappingFilter.put("mappings", List.of("·=>", "！=>")); // 键值对：原始字符=>替换后字符
            charFilters.add(mappingFilter);

            // 令牌过滤器：转小写（将 Hello 转为 hello）
            List<Map<String, Object>> tokenFilters = new ArrayList<>();
            tokenFilters.add(Map.of("type", "lowercase"));

            // 调用自定义分词链
            List<ESAnalyzeHttpClientUtils.TokenResult> advancedResult = 
                    ESAnalyzeHttpClientUtils.analyzeTextWithCustomChain(
                            text2, "ik_max_word", charFilters, tokenFilters
                    );
            
            System.out.println("\n=== 进阶分词结果（自定义分词链）===");
            advancedResult.forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 4. 项目关闭时关闭 HttpClient（释放资源）
            try {
                ESAnalyzeHttpClientUtils.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

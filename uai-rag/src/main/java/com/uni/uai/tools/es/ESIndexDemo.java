package com.uni.uai.tools.es;

import java.util.HashMap;
import java.util.Map;

public class ESIndexDemo {
    public static void main(String[] args) {
        // ES 连接配置
        String esHost = "localhost";
        int esPort = 9200;
        String apiKey = "OUp2TFZKZ0JLc3QzLWpXOGU4Vlk6UzhQUTREbXFSSUN2bFJVWm5QNVNOZw==";

        try {
            // 1. 初始化工具类
            ESIndexManagerUtils.init(esHost, esPort, apiKey, false);

            // 2. 定义索引映射（示例：文档包含标题、内容、创建时间）
            Map<String, Object> mappings = new HashMap<String, Object>();
            Map<String, Object> properties = new HashMap<String, Object>();
            
            // 标题字段（关键词+文本，支持精确匹配和分词搜索）
            Map<String, Object> titleField = new HashMap<String, Object>();
            titleField.put("type", "text");
            titleField.put("analyzer", "ik_max_word");
            titleField.put("fields", new HashMap<String, Object>() {{
                put("keyword", new HashMap<String, Object>() {{
                    put("type", "keyword");
                    put("ignore_above", 256);
                }});
            }});
            properties.put("title", titleField);

            // 内容字段（中文分词）
            Map<String, Object> contentField = new HashMap<String, Object>();
            contentField.put("type", "text");
            contentField.put("analyzer", "ik_max_word");
            properties.put("content", contentField);

            // 创建时间字段（日期类型）
            Map<String, Object> createTimeField = new HashMap<String, Object>();
            createTimeField.put("type", "date");
            createTimeField.put("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis");
            properties.put("create_time", createTimeField);

            mappings.put("properties", properties);

            // 3. 创建索引（主分片1，副本1）
            String indexName = "test_article";
            ESIndexManagerUtils.createIndex(indexName, 1, 1, mappings);

            // 4. 检查索引是否存在
            boolean exists = ESIndexManagerUtils.isIndexExists(indexName);
            System.out.println("索引 " + indexName + " 是否存在：" + exists);

            // 5. 获取索引信息
            Map<String, Object> indexInfo = ESIndexManagerUtils.getIndexInfo(indexName);
            System.out.println("索引信息：" + indexInfo);

            // 6. 给索引添加别名
            ESIndexManagerUtils.addAlias(indexName, "article_alias", true);

            // 7. 获取索引健康状态
            String health = ESIndexManagerUtils.getIndexHealth(indexName);
            System.out.println("索引健康状态：" + health);

            // 8. （可选）删除索引
            // ESIndexManagerUtils.deleteIndex(indexName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 9. 关闭 HttpClient
            try {
                ESIndexManagerUtils.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
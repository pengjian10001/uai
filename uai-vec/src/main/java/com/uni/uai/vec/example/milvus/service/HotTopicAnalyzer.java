package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.HotTopicCluster;
import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HotTopicAnalyzer {

    private static final Map<String, String> LANGUAGE_LABELS = Map.of(
            "zh", "中文",
            "en", "英文"
    );

    private final MockClusterSummarizer summarizer = new MockClusterSummarizer();

    public List<HotTopicCluster> analyzeTopClusters(int[] labels, List<TopicVectorRecord> records, int topN) {
        Map<Integer, List<TopicVectorRecord>> grouped = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            int label = labels[i];
            if (label < 0) {
                continue;
            }
            grouped.computeIfAbsent(label, key -> new ArrayList<>()).add(records.get(i));
        }

        return grouped.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Integer, List<TopicVectorRecord>> entry) -> entry.getValue().size()).reversed())
                .limit(topN)
                .map(entry -> new HotTopicCluster(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue(),
                        summarizer.summarize(entry.getKey(), entry.getValue())
                ))
                .collect(Collectors.toList());
    }

    public void printReport(List<HotTopicCluster> clusters, int totalRecords, int noiseCount) {
        System.out.println("\n========== 热点话题聚类报告 ==========");
        System.out.printf("主题向量总数：%d｜噪声点：%d｜识别话题簇：%d%n",
                totalRecords, noiseCount, clusters.size());
        System.out.println("说明：簇内样本量越大，越可能是当前舆情热点（无需访问量数据）\n");

        int rank = 1;
        for (HotTopicCluster cluster : clusters) {
            System.out.printf("第 %d 热点｜簇编号：%d｜样本量：%d%n",
                    rank++, cluster.getClusterId(), cluster.getSize());
            System.out.println("主题摘要：" + cluster.getSummary());

            Map<String, Long> languageStats = cluster.getMembers().stream()
                    .collect(Collectors.groupingBy(
                            record -> record.getSegment().getLanguage(),
                            Collectors.counting()
                    ));
            System.out.println("语言分布：" + formatLanguageStats(languageStats));

            System.out.println("代表片段：");
            cluster.getMembers().stream().limit(2).forEach(record -> {
                String lang = LANGUAGE_LABELS.getOrDefault(record.getSegment().getLanguage(), record.getSegment().getLanguage());
                System.out.println("  · " + lang + "：" + truncate(record.getSegment().getTopicText(), 100));
            });
            System.out.println();
        }

        if (!clusters.isEmpty()) {
            HotTopicCluster hottest = clusters.get(0);
            double ratio = hottest.getSize() * 100.0 / totalRecords;
            System.out.printf("当前最大热点：%s，占全部主题向量 %.1f%%%n",
                    extractTopicName(hottest.getSummary()), ratio);
        }
        System.out.println("========================================\n");
    }

    private Map<String, Long> formatLanguageStats(Map<String, Long> raw) {
        Map<String, Long> formatted = new LinkedHashMap<>();
        raw.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> formatted.put(
                        LANGUAGE_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue()
                ));
        return formatted;
    }

    private String extractTopicName(String summary) {
        int start = summary.indexOf('【');
        int end = summary.indexOf('】');
        if (start >= 0 && end > start) {
            return summary.substring(start + 1, end);
        }
        return summary;
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}

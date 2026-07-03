package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 模拟 LLM 对聚类结果做中文主题概括。
 */
public class MockClusterSummarizer {

    public String summarize(int clusterId, List<TopicVectorRecord> members) {
        String topicLabel = inferChineseTopicLabel(members);
        List<String> samples = members.stream()
                .limit(3)
                .map(record -> extractSentence(record.getSegment().getTopicText()))
                .collect(Collectors.toList());

        String joined = String.join("；", samples);
        if (joined.length() > 180) {
            joined = joined.substring(0, 177) + "...";
        }
        return String.format(Locale.CHINA,
                "【%s】共 %d 条相关报道。代表内容：%s",
                topicLabel,
                members.size(),
                joined);
    }

    private String inferChineseTopicLabel(List<TopicVectorRecord> members) {
        String combined = members.stream()
                .map(record -> record.getSegment().getTopicText().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));

        if (containsAny(combined, "人工智能", "ai act", "openai", "claude", "deepmind", "大模型",
                "合规", "智能体", "language model", "reasoning model", "meta 开源")) {
            return "人工智能监管与大模型";
        }
        if (containsAny(combined, "奥运", "olympic", "铁人三项", "塞纳河", "游泳", "triathlon", "seine")) {
            return "巴黎奥运会赛事";
        }
        if (containsAny(combined, "高温", "气候", "野火", "heatwave", "wildfire", "海洋", "干旱", "防汛")) {
            return "极端天气与气候变化";
        }
        if (containsAny(combined, "选举", "选情", "摇摆州", "选票", "election", "campaign", "poll", "选民")) {
            return "美国大选与摇摆州选情";
        }
        return "话题簇-" + members.get(0).getSegment().getTitle();
    }

    private String extractSentence(String topicText) {
        int separator = topicText.indexOf('｜');
        if (separator < 0) {
            separator = topicText.indexOf('|');
        }
        if (separator >= 0 && separator + 1 < topicText.length()) {
            return topicText.substring(separator + 1).trim();
        }
        return topicText;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}

package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.NewsArticle;
import com.uni.uai.vec.example.milvus.model.TopicSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 模拟 LLM 多主题抽取：将长报告按句子切分，保留语义完整的主题句。
 */
public class MockLlmTopicExtractor {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?。！？])\\s+");

    public List<TopicSegment> extractTopics(NewsArticle article) {
        List<TopicSegment> segments = new ArrayList<>();
        String[] sentences = SENTENCE_SPLIT.split(article.getReport().trim());
        int index = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 8) {
                continue;
            }
            segments.add(new TopicSegment(
                    article.getId(),
                    article.getTitle(),
                    article.getLanguage(),
                    article.getSource(),
                    article.getPublishedAtEpochMs(),
                    buildTopicText(article, trimmed),
                    index++
            ));
        }
        if (segments.isEmpty()) {
            segments.add(new TopicSegment(
                    article.getId(),
                    article.getTitle(),
                    article.getLanguage(),
                    article.getSource(),
                    article.getPublishedAtEpochMs(),
                    buildTopicText(article, article.getReport()),
                    0
            ));
        }
        return segments;
    }

    private String buildTopicText(NewsArticle article, String sentence) {
        String langLabel = "zh".equals(article.getLanguage()) ? "中文" : "英文";
        return String.format("【%s】%s｜%s", langLabel, article.getTitle(), sentence);
    }
}

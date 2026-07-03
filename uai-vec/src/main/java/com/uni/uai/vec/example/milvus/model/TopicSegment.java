package com.uni.uai.vec.example.milvus.model;

/**
 * 从单篇新闻报告中抽取的一个主题片段（一篇报告可对应多个 TopicSegment）。
 */
public class TopicSegment {

    private final String newsId;
    private final String title;
    private final String language;
    private final String source;
    private final long publishedAtEpochMs;
    private final String topicText;
    private final int segmentIndex;

    public TopicSegment(String newsId, String title, String language, String source,
                        long publishedAtEpochMs, String topicText, int segmentIndex) {
        this.newsId = newsId;
        this.title = title;
        this.language = language;
        this.source = source;
        this.publishedAtEpochMs = publishedAtEpochMs;
        this.topicText = topicText;
        this.segmentIndex = segmentIndex;
    }

    public String getNewsId() {
        return newsId;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public String getSource() {
        return source;
    }

    public long getPublishedAtEpochMs() {
        return publishedAtEpochMs;
    }

    public String getTopicText() {
        return topicText;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }
}

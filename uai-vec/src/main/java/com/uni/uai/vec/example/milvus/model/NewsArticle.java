package com.uni.uai.vec.example.milvus.model;

public class NewsArticle {

    private String id;
    private String title;
    private String language;
    private String source;
    private long publishedAtEpochMs;
    private String report;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getPublishedAtEpochMs() {
        return publishedAtEpochMs;
    }

    public void setPublishedAtEpochMs(long publishedAtEpochMs) {
        this.publishedAtEpochMs = publishedAtEpochMs;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }
}

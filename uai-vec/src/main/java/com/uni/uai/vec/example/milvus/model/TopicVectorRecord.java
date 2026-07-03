package com.uni.uai.vec.example.milvus.model;

public class TopicVectorRecord {

    private final long id;
    private final TopicSegment segment;
    private final float[] embedding;

    public TopicVectorRecord(long id, TopicSegment segment, float[] embedding) {
        this.id = id;
        this.segment = segment;
        this.embedding = embedding;
    }

    public long getId() {
        return id;
    }

    public TopicSegment getSegment() {
        return segment;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}

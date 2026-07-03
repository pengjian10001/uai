package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.List;

public interface NewsVectorStore extends AutoCloseable {

    String backendName();

    void recreateCollection();

    void insertRecords(List<TopicVectorRecord> records);

    List<TopicVectorRecord> queryAllRecords();

    List<VectorNeighborHit> searchNeighbors(float[] queryVector, int topK);

    @Override
    default void close() {
    }
}

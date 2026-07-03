package com.uni.uai.graph.facesearch.vector;

import com.uni.uai.graph.facesearch.model.FaceRecord;
import com.uni.uai.graph.facesearch.model.MatchHit;

import java.util.List;

public interface VectorStore extends AutoCloseable {

    void insertBatch(List<FaceRecord> records) throws Exception;

    List<MatchHit> search(float[] queryEmbedding, int topK, double minScore) throws Exception;

    long count();

    void clear() throws Exception;

    @Override
    default void close() throws Exception {
    }
}

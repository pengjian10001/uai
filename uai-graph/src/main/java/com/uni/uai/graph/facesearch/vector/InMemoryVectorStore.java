package com.uni.uai.graph.facesearch.vector;

import com.uni.uai.graph.facesearch.model.FaceRecord;
import com.uni.uai.graph.facesearch.model.MatchHit;
import com.uni.uai.graph.facesearch.util.VectorMath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 轻量本地方案：内存余弦相似度检索，无需部署 Milvus。
 */
public final class InMemoryVectorStore implements VectorStore {

    private final List<FaceRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void insertBatch(List<FaceRecord> batch) {
        records.addAll(batch);
    }

    @Override
    public List<MatchHit> search(float[] queryEmbedding, int topK, double minScore) {
        List<MatchHit> hits = new ArrayList<>();
        for (FaceRecord record : records) {
            double score = VectorMath.cosineSimilarity(queryEmbedding, record.embedding());
            if (score >= minScore) {
                hits.add(new MatchHit(record.videoId(), record.frameNo(), record.timeSec(), score));
            }
        }
        hits.sort(Comparator.comparingDouble(MatchHit::score).reversed());
        if (hits.size() > topK) {
            return new ArrayList<>(hits.subList(0, topK));
        }
        return hits;
    }

    @Override
    public long count() {
        return records.size();
    }

    @Override
    public void clear() {
        records.clear();
    }
}

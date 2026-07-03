package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;
import com.uni.uai.vec.example.milvus.util.VectorUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯内存向量库：零依赖本地运行，算法链路与 Milvus 版一致（Top-K 近邻 → 距离矩阵 → 密度聚类）。
 */
public class InMemoryNewsVectorStore implements NewsVectorStore {

    private final Map<Long, TopicVectorRecord> records = new ConcurrentHashMap<>();

    @Override
    public String backendName() {
        return "InMemory (本地内存，无需 Docker)";
    }

    @Override
    public void recreateCollection() {
        records.clear();
    }

    @Override
    public void insertRecords(List<TopicVectorRecord> batch) {
        for (TopicVectorRecord record : batch) {
            records.put(record.getId(), record);
        }
    }

    @Override
    public List<TopicVectorRecord> queryAllRecords() {
        List<TopicVectorRecord> all = new ArrayList<>(records.values());
        all.sort(Comparator.comparingLong(TopicVectorRecord::getId));
        return all;
    }

    @Override
    public List<VectorNeighborHit> searchNeighbors(float[] queryVector, int topK) {
        List<VectorNeighborHit> hits = new ArrayList<>();
        for (TopicVectorRecord record : records.values()) {
            double distance = VectorUtils.cosineDistance(queryVector, record.getEmbedding());
            hits.add(new VectorNeighborHit(record.getId(), distance));
        }
        hits.sort(Comparator.comparingDouble(VectorNeighborHit::getDistance));
        int limit = Math.min(topK, hits.size());
        return hits.subList(0, limit);
    }
}

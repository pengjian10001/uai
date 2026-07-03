package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参照 Milvus 官方 HDBSCAN 教程：利用向量库 Top-K 近邻检索构建稀疏距离矩阵。
 */
public class DistanceMatrixBuilder {

    private final NewsVectorStore vectorStore;

    public DistanceMatrixBuilder(NewsVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public DistanceMatrix build(List<TopicVectorRecord> records, int topK) {
        int size = records.size();
        double[][] matrix = new double[size][size];
        for (double[] row : matrix) {
            Arrays.fill(row, DistanceMatrix.INF);
        }
        for (int i = 0; i < size; i++) {
            matrix[i][i] = 0.0;
        }

        Map<Long, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < size; i++) {
            idToIndex.put(records.get(i).getId(), i);
        }

        for (TopicVectorRecord record : records) {
            List<VectorNeighborHit> neighbors = vectorStore.searchNeighbors(record.getEmbedding(), topK);
            int sourceIndex = idToIndex.get(record.getId());

            for (VectorNeighborHit hit : neighbors) {
                Integer targetIndex = idToIndex.get(hit.getId());
                if (targetIndex == null) {
                    continue;
                }
                matrix[sourceIndex][targetIndex] = hit.getDistance();
                matrix[targetIndex][sourceIndex] = hit.getDistance();
            }
        }

        return new DistanceMatrix(matrix, records);
    }
}

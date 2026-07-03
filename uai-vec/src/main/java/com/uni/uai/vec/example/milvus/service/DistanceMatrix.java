package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.List;

public class DistanceMatrix {

    public static final double INF = Double.POSITIVE_INFINITY;

    private final double[][] values;
    private final List<TopicVectorRecord> records;

    public DistanceMatrix(double[][] values, List<TopicVectorRecord> records) {
        this.values = values;
        this.records = records;
    }

    public double[][] getValues() {
        return values;
    }

    public List<TopicVectorRecord> getRecords() {
        return records;
    }

    public int size() {
        return records.size();
    }
}

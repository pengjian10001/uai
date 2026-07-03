package com.uni.uai.vec.example.milvus.service;

public class VectorNeighborHit {

    private final long id;
    private final double distance;

    public VectorNeighborHit(long id, double distance) {
        this.id = id;
        this.distance = distance;
    }

    public long getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }
}

package com.uni.uai.graph.facesearch.model;

/**
 * 单次向量检索命中结果。
 */
public record MatchHit(String videoId, long frameNo, double timeSec, double score) {
}

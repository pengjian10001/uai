package com.uni.uai.graph.facesearch.pipeline;

import com.uni.uai.graph.facesearch.face.OpenCvFaceService;
import com.uni.uai.graph.facesearch.model.MatchHit;
import com.uni.uai.graph.facesearch.model.TimeSegment;
import com.uni.uai.graph.facesearch.vector.VectorStore;

import java.nio.file.Path;
import java.util.List;

/**
 * 目标人物图 → 特征向量 → ANN/余弦检索 → 时间段聚合。
 */
public final class PersonSearcher {

    private final OpenCvFaceService faceService;
    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final int topK;
    private final TimeSegmentMerger merger;

    public PersonSearcher(
            OpenCvFaceService faceService,
            VectorStore vectorStore,
            double similarityThreshold,
            int topK,
            double mergeGapSec) {
        this.faceService = faceService;
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
        this.topK = topK;
        this.merger = new TimeSegmentMerger(mergeGapSec);
    }

    public SearchResult search(Path targetPersonImage) throws Exception {
        float[] query = faceService.extractPrimaryEmbedding(targetPersonImage);
        List<MatchHit> hits = vectorStore.search(query, topK, similarityThreshold);
        List<TimeSegment> segments = merger.merge(hits);
        return new SearchResult(query, hits, segments);
    }

    public record SearchResult(float[] queryEmbedding, List<MatchHit> hits, List<TimeSegment> segments) {
    }
}

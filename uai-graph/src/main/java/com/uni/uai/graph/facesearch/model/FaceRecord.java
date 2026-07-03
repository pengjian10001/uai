package com.uni.uai.graph.facesearch.model;

/**
 * 入库的人脸向量记录，绑定视频元数据。
 */
public record FaceRecord(
        String videoId,
        long frameNo,
        double timeSec,
        float[] embedding,
        float detectionScore) {
}

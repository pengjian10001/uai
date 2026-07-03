package com.uni.uai.graph.facesearch.pipeline;

import com.uni.uai.graph.facesearch.face.OpenCvFaceService;
import com.uni.uai.graph.facesearch.model.FaceDetection;
import com.uni.uai.graph.facesearch.model.FaceRecord;
import com.uni.uai.graph.facesearch.util.ImageUtils;
import com.uni.uai.graph.facesearch.vector.VectorStore;
import com.uni.uai.graph.facesearch.video.VideoFrameExtractor;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频批量抽帧 → 人脸检测 → 特征提取 → 向量入库。
 */
public final class VideoIndexer {

    private final OpenCvFaceService faceService;
    private final VectorStore vectorStore;
    private final int sampleFps;
    private final int batchSize;

    public VideoIndexer(OpenCvFaceService faceService, VectorStore vectorStore, int sampleFps, int batchSize) {
        this.faceService = faceService;
        this.vectorStore = vectorStore;
        this.sampleFps = sampleFps;
        this.batchSize = batchSize;
    }

    public IndexStats indexVideo(String videoId, Path videoPath) throws Exception {
        List<FaceRecord> buffer = new ArrayList<>();
        long[] frameCounter = {0};
        long[] faceCounter = {0};

        VideoFrameExtractor.extractFrames(videoPath.toString(), sampleFps, (frame, frameNum, timeSec) -> {
            frameCounter[0]++;
            Mat image = ImageUtils.frameToMat(frame);
            try {
                List<FaceDetection> faces = faceService.detectFaces(image);
                for (FaceDetection face : faces) {
                    float[] embedding = faceService.extractEmbedding(image, face);
                    buffer.add(new FaceRecord(videoId, frameNum, timeSec, embedding, face.score()));
                    faceCounter[0]++;
                    if (buffer.size() >= batchSize) {
                        vectorStore.insertBatch(buffer);
                        buffer.clear();
                    }
                }
            } finally {
                image.close();
            }
        });

        if (!buffer.isEmpty()) {
            vectorStore.insertBatch(buffer);
        }
        return new IndexStats(videoId, frameCounter[0], faceCounter[0], vectorStore.count());
    }

    public record IndexStats(String videoId, long sampledFrames, long faceVectors, long totalVectors) {
    }
}

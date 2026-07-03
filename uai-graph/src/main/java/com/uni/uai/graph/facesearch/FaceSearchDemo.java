package com.uni.uai.graph.facesearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.uni.uai.graph.facesearch.face.OpenCvFaceService;
import com.uni.uai.graph.facesearch.model.MatchHit;
import com.uni.uai.graph.facesearch.model.TimeSegment;
import com.uni.uai.graph.facesearch.pipeline.PersonSearcher;
import com.uni.uai.graph.facesearch.pipeline.VideoIndexer;
import com.uni.uai.graph.facesearch.util.ModelDownloader;
import com.uni.uai.graph.facesearch.vector.InMemoryVectorStore;
import com.uni.uai.graph.facesearch.vector.MilvusVectorStore;
import com.uni.uai.graph.facesearch.vector.VectorStore;

import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI 演示入口：
 * <pre>
 *   mvn -q exec:java -Dexec.args="index --video demo-data/sample.mp4 --video-id demo"
 *   mvn -q exec:java -Dexec.args="search --target demo-data/target.jpg"
 *   mvn -q exec:java -Dexec.args="run --video demo-data/sample.mp4 --target demo-data/target.jpg"
 * </pre>
 */
public final class FaceSearchDemo {

    private static final int SAMPLE_FPS = 2;
    private static final double SIMILARITY_THRESHOLD = 0.45;
    private static final double MERGE_GAP_SEC = 3.0;
    private static final int TOP_K = 10_000;
    private static final int BATCH_SIZE = 64;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        Map<String, String> options = parseArgs(args);
        String command = args[0];
        Path modelDir = Paths.get(options.getOrDefault("--model-dir", "models"));
        Files.createDirectories(modelDir);
        // Haar 检测器自动下载 cascade；SFace 为可选增强模型
        try {
            ModelDownloader.ensureModels(modelDir);
        } catch (Exception ex) {
            System.out.println("可选模型下载跳过: " + ex.getMessage());
        }

        boolean useMilvus = Boolean.parseBoolean(options.getOrDefault("--milvus", "false"));
        String milvusHost = options.getOrDefault("--milvus-host", "127.0.0.1");
        int milvusPort = Integer.parseInt(options.getOrDefault("--milvus-port", "19530"));

        try (OpenCvFaceService faceService = new OpenCvFaceService(modelDir);
             VectorStore vectorStore = createVectorStore(
                     useMilvus, milvusHost, milvusPort, faceService.embeddingDimension())) {

            switch (command) {
                case "index" -> runIndex(options, faceService, vectorStore);
                case "search" -> runSearch(options, faceService, vectorStore);
                case "run" -> {
                    runIndex(options, faceService, vectorStore);
                    runSearch(options, faceService, vectorStore);
                }
                default -> printUsage();
            }
        }
    }

    private static void runIndex(Map<String, String> options, OpenCvFaceService faceService, VectorStore vectorStore)
            throws Exception {
        Path video = requirePath(options, "--video");
        String videoId = options.getOrDefault("--video-id", video.getFileName().toString());
        VideoIndexer indexer = new VideoIndexer(faceService, vectorStore, SAMPLE_FPS, BATCH_SIZE);
        System.out.println("开始索引视频: " + video);
        VideoIndexer.IndexStats stats = indexer.indexVideo(videoId, video);
        System.out.printf("索引完成 video=%s, sampledFrames=%d, faceVectors=%d, storeSize=%d%n",
                stats.videoId(), stats.sampledFrames(), stats.faceVectors(), stats.totalVectors());
    }

    private static void runSearch(Map<String, String> options, OpenCvFaceService faceService, VectorStore vectorStore)
            throws Exception {
        Path target = requirePath(options, "--target");
        double threshold = Double.parseDouble(options.getOrDefault("--threshold", String.valueOf(SIMILARITY_THRESHOLD)));
        PersonSearcher searcher = new PersonSearcher(faceService, vectorStore, threshold, TOP_K, MERGE_GAP_SEC);

        System.out.println("开始检索目标人物: " + target);
        PersonSearcher.SearchResult result = searcher.search(target);

        System.out.println("\n=== 匹配帧（按相似度排序）===");
        for (MatchHit hit : result.hits()) {
            System.out.printf("video=%s time=%.2fs frame=%d score=%.4f%n",
                    hit.videoId(), hit.timeSec(), hit.frameNo(), hit.score());
        }

        System.out.println("\n=== 合并时间段 ===");
        if (result.segments().isEmpty()) {
            System.out.println("未找到匹配片段，可尝试降低 --threshold 或更换更清晰的目标正面照。");
        } else {
            for (TimeSegment segment : result.segments()) {
                System.out.println(segment);
            }
        }

        Path output = Paths.get(options.getOrDefault("--output", "face-search-result.json"));
        writeJson(output, result);
        System.out.println("\n结果已写入: " + output.toAbsolutePath());
    }

    private static void writeJson(Path output, PersonSearcher.SearchResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hitCount", result.hits().size());
        payload.put("hits", result.hits());
        payload.put("segments", result.segments());
        mapper.writeValue(output.toFile(), payload);
    }

    private static VectorStore createVectorStore(boolean useMilvus, String host, int port, int dimension) {
        if (useMilvus) {
            System.out.println("使用 Milvus 向量库: " + host + ":" + port);
            return new MilvusVectorStore(host, port, dimension, true);
        }
        System.out.println("使用内存向量库（轻量本地方案）");
        return new InMemoryVectorStore();
    }

    private static Path requirePath(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少参数: " + key);
        }
        Path path = Paths.get(value);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + path);
        }
        return path;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            String token = args[i];
            if (token.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(token, args[++i]);
                } else {
                    map.put(token, "true");
                }
            }
        }
        return map;
    }

    private static void printUsage() {
        System.out.println("""
                人脸视频检索 Demo

                命令:
                  index  索引视频人脸向量
                  search 按目标人物图检索
                  run    先索引再检索（单机演示推荐）

                示例:
                  mvn -q exec:java -Dexec.args="run --video demo-data/sample.mp4 --target demo-data/target.jpg"
                  mvn -q exec:java -Dexec.args="index --video demo-data/sample.mp4 --video-id demo"
                  mvn -q exec:java -Dexec.args="search --target demo-data/target.jpg --threshold 0.45"
                  mvn -q exec:java -Dexec.args="run --video demo.mp4 --target person.jpg --milvus true"

                参数:
                  --video        视频路径
                  --target       目标人物图片路径
                  --video-id     视频 ID（默认文件名）
                  --model-dir    ONNX 模型目录（默认 models）
                  --threshold    余弦相似度阈值（默认 0.45，SFace 模型建议 0.40~0.55）
                  --milvus       是否使用 Milvus（默认 false，使用内存库）
                  --milvus-host  Milvus 地址（默认 127.0.0.1）
                  --milvus-port  Milvus 端口（默认 19530）
                  --output       检索结果 JSON 输出路径
                """);
    }
}

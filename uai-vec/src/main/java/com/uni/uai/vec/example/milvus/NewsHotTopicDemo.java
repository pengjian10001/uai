package com.uni.uai.vec.example.milvus;

import com.uni.uai.vec.example.milvus.config.MilvusSettings;
import com.uni.uai.vec.example.milvus.service.InMemoryNewsVectorStore;
import com.uni.uai.vec.example.milvus.service.MilvusNewsVectorStore;
import com.uni.uai.vec.example.milvus.service.NewsHotTopicPipeline;
import com.uni.uai.vec.example.milvus.service.NewsVectorStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * 多语言新闻热点发现 Demo。
 *
 * 默认本地模式（零 Docker 依赖，直接可运行）：
 *   mvn compile exec:java
 *
 * Milvus 模式（需先 docker compose up -d）：
 *   mvn compile exec:java -Dexec.args="--milvus"
 */
public class NewsHotTopicDemo {

    private enum RunMode {
        LOCAL,
        MILVUS,
        AUTO
    }

    public static void main(String[] args) throws Exception {
        RunMode mode = parseMode(args);
        NewsHotTopicPipeline pipeline = new NewsHotTopicPipeline();

        System.out.println("=== 新闻热点聚类 Demo（中文为主）===");

        if (mode == RunMode.LOCAL) {
            runLocal(pipeline);
            return;
        }

        if (mode == RunMode.MILVUS) {
            runMilvus(pipeline);
            return;
        }

        if (isMilvusReachable()) {
            System.out.println("检测到 Milvus 可用，使用 Milvus 模式。");
            runMilvus(pipeline);
        } else {
            System.out.println("未检测到 Milvus（" + MilvusSettings.URI + "），自动切换本地内存模式。");
            System.out.println("如需 Milvus：cd uai-vec && docker compose up -d\n");
            runLocal(pipeline);
        }
    }

    private static void runLocal(NewsHotTopicPipeline pipeline) throws Exception {
        try (NewsVectorStore store = new InMemoryNewsVectorStore()) {
            pipeline.run(store);
        }
        System.out.println("Demo 执行完成（本地模式）。");
    }

    private static void runMilvus(NewsHotTopicPipeline pipeline) throws Exception {
        try (MilvusNewsVectorStore store = new MilvusNewsVectorStore()) {
            pipeline.run(store);
            Map<Long, Integer> ivfHint = store.queryIvfClusterDistributionHint();
            System.out.println("Milvus IVF 原生聚类桶数(nlist): " + ivfHint.values().iterator().next());
            System.out.println("提示: 生产环境可将近 24h 新闻作为 filter 子集后重复聚类，实现实时舆情窗口分析。");
        } catch (Exception ex) {
            System.err.println("\nMilvus 模式运行失败: " + ex.getMessage());
            System.err.println("请先启动 Milvus: cd uai-vec && docker compose up -d");
            System.err.println("或使用本地模式: mvn compile exec:java -Dexec.args=\"--local\"");
            throw ex;
        }
        System.out.println("Demo 执行完成（Milvus 模式）。");
    }

    private static RunMode parseMode(String[] args) {
        if (args == null || args.length == 0) {
            return RunMode.LOCAL;
        }
        for (String arg : args) {
            String normalized = arg.trim().toLowerCase(Locale.ROOT);
            if ("--local".equals(normalized) || "-l".equals(normalized)) {
                return RunMode.LOCAL;
            }
            if ("--milvus".equals(normalized) || "-m".equals(normalized)) {
                return RunMode.MILVUS;
            }
            if ("--auto".equals(normalized) || "-a".equals(normalized)) {
                return RunMode.AUTO;
            }
        }
        throw new IllegalArgumentException("未知参数: " + Arrays.toString(args)
                + "，支持 --local | --milvus | --auto");
    }

    private static boolean isMilvusReachable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:9091/healthz"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }
}

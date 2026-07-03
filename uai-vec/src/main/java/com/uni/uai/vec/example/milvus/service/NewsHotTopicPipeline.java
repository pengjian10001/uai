package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.config.MilvusSettings;
import com.uni.uai.vec.example.milvus.model.HotTopicCluster;
import com.uni.uai.vec.example.milvus.model.NewsArticle;
import com.uni.uai.vec.example.milvus.model.TopicSegment;
import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.ArrayList;
import java.util.List;

public class NewsHotTopicPipeline {

    private final SampleNewsLoader newsLoader = new SampleNewsLoader();
    private final MockLlmTopicExtractor topicExtractor = new MockLlmTopicExtractor();
    private final MockEmbeddingService embeddingService = new MockEmbeddingService();
    private final HotTopicAnalyzer analyzer = new HotTopicAnalyzer();

    public void run(NewsVectorStore vectorStore) throws Exception {
        System.out.println("向量后端: " + vectorStore.backendName());

        List<NewsArticle> articles = newsLoader.load();
        long zhCount = articles.stream().filter(a -> "zh".equals(a.getLanguage())).count();
        long enCount = articles.stream().filter(a -> "en".equals(a.getLanguage())).count();
        System.out.printf("加载样本新闻：%d 篇（中文 %d 篇，英文 %d 篇）%n", articles.size(), zhCount, enCount);

        List<TopicSegment> allSegments = new ArrayList<>();
        for (NewsArticle article : articles) {
            allSegments.addAll(topicExtractor.extractTopics(article));
        }
        System.out.println("主题片段总数（单篇可拆多个主题）: " + allSegments.size());

        List<TopicVectorRecord> vectorRecords = new ArrayList<>();
        long nextId = 1L;
        for (TopicSegment segment : allSegments) {
            float[] embedding = embeddingService.embed(segment.getTopicText());
            vectorRecords.add(new TopicVectorRecord(nextId++, segment, embedding));
        }

        System.out.println("\n[1/4] 初始化集合并写入向量 ...");
        vectorStore.recreateCollection();
        vectorStore.insertRecords(vectorRecords);
        System.out.println("写入完成，共 " + vectorRecords.size() + " 条主题向量");

        System.out.println("\n[2/4] 读取向量并构建 Top-K 近邻距离矩阵 ...");
        List<TopicVectorRecord> storedRecords = vectorStore.queryAllRecords();
        DistanceMatrixBuilder matrixBuilder = new DistanceMatrixBuilder(vectorStore);
        DistanceMatrix distanceMatrix = matrixBuilder.build(storedRecords, MilvusSettings.SEARCH_TOP_K);
        System.out.println("距离矩阵规模: " + distanceMatrix.size() + " x " + distanceMatrix.size());

        System.out.println("\n[3/4] 运行密度聚类（基于预计算距离矩阵）...");
        PrecomputedDensityClusterer clusterer = new PrecomputedDensityClusterer();
        int[] labels = clusterer.cluster(
                distanceMatrix,
                MilvusSettings.MIN_SAMPLES,
                MilvusSettings.MIN_CLUSTER_SIZE
        );

        int noiseCount = 0;
        for (int label : labels) {
            if (label < 0) {
                noiseCount++;
            }
        }

        System.out.println("\n[4/4] 统计最大簇并生成热点报告 ...");
        List<HotTopicCluster> hotTopics = analyzer.analyzeTopClusters(labels, storedRecords, 5);
        analyzer.printReport(hotTopics, storedRecords.size(), noiseCount);
    }
}

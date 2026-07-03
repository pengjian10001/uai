package com.uni.uai.vec.example.milvus.service;

import com.uni.uai.vec.example.milvus.config.MilvusSettings;
import com.uni.uai.vec.example.milvus.util.VectorUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

/**
 * 模拟多语言 Embedding 模型（如 BGE-M3）。
 * 同一语义主题会映射到相近向量空间，便于聚类演示。
 * 生产环境替换为真实 Embedding API 即可。
 */
public class MockEmbeddingService {

    public float[] embed(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        long seed = seedForSemanticBucket(normalized);
        Random random = new Random(seed);

        float[] vector = new float[MilvusSettings.VECTOR_DIM];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) random.nextGaussian();
        }

        // 注入与文本哈希相关的噪声，使同主题不同语言仍相近、不同主题可区分
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vector[i % vector.length] += (bytes[i] % 17) * 0.003f;
        }

        return VectorUtils.normalize(vector);
    }

    /**
     * 用关键词桶模拟跨语言语义对齐（中文为主、英文为辅）。
     */
    private long seedForSemanticBucket(String text) {
        if (containsAny(text, "人工智能", "ai act", "openai", "claude", "deepmind", "大模型",
                "合规", "智能体", "language model", "reasoning model", "meta 开源", "anthropic")) {
            return 11L;
        }
        if (containsAny(text, "奥运", "olympic", "铁人三项", "塞纳河", "游泳", "triathlon", "seine", "赛程")) {
            return 22L;
        }
        if (containsAny(text, "高温", "气候", "野火", "heatwave", "wildfire", "海洋", "干旱", "防汛", "heatstroke")) {
            return 33L;
        }
        if (containsAny(text, "选举", "选情", "摇摆州", "选票", "election", "campaign", "poll", "选民", "swing state")) {
            return 44L;
        }
        return text.hashCode();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

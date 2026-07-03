package com.uni.uai.vec.example.milvus.config;

public final class MilvusSettings {

    public static final String URI = env("MILVUS_URI", "http://localhost:19530");
    public static final String TOKEN = env("MILVUS_TOKEN", "root:Milvus");
    public static final String COLLECTION = env("MILVUS_COLLECTION", "news_topic_vectors");
    public static final int VECTOR_DIM = 384;
    public static final int IVF_NLIST = 32;
    public static final int SEARCH_TOP_K = 15;
    public static final int MIN_CLUSTER_SIZE = 3;
    public static final int MIN_SAMPLES = 2;

    private MilvusSettings() {
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

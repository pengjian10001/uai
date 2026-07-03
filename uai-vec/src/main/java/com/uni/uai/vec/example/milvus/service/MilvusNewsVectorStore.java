package com.uni.uai.vec.example.milvus.service;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uni.uai.vec.example.milvus.config.MilvusSettings;
import com.uni.uai.vec.example.milvus.model.TopicSegment;
import com.uni.uai.vec.example.milvus.model.TopicVectorRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量存储：IVF 索引内置 K-Means 分簇，检索时仅比对邻近簇。
 */
public class MilvusNewsVectorStore implements NewsVectorStore {

    private static final Gson GSON = new Gson();

    private final MilvusClientV2 client;
    private final String collectionName;

    public MilvusNewsVectorStore() {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(MilvusSettings.URI)
                .token(MilvusSettings.TOKEN)
                .build();
        this.client = new MilvusClientV2(connectConfig);
        this.collectionName = MilvusSettings.COLLECTION;
    }

    @Override
    public String backendName() {
        return "Milvus Standalone (" + MilvusSettings.URI + ", IVF nlist=" + MilvusSettings.IVF_NLIST + ")";
    }

    @Override
    public void recreateCollection() {
        if (Boolean.TRUE.equals(client.hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName)
                .build()))) {
            client.dropCollection(DropCollectionReq.builder().collectionName(collectionName).build());
        }

        CreateCollectionReq.CollectionSchema schema = client.createSchema();
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(false)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("news_id")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("language")
                .dataType(DataType.VarChar)
                .maxLength(16)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("source")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("topic_text")
                .dataType(DataType.VarChar)
                .maxLength(2048)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("published_at")
                .dataType(DataType.Int64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("embedding")
                .dataType(DataType.FloatVector)
                .dimension(MilvusSettings.VECTOR_DIM)
                .build());

        IndexParam indexParam = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("nlist", MilvusSettings.IVF_NLIST))
                .build();

        client.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(Collections.singletonList(indexParam))
                .build());

        client.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
    }

    @Override
    public void insertRecords(List<TopicVectorRecord> records) {
        List<JsonObject> rows = new ArrayList<>();
        for (TopicVectorRecord record : records) {
            TopicSegment segment = record.getSegment();
            JsonObject row = new JsonObject();
            row.addProperty("id", record.getId());
            row.addProperty("news_id", segment.getNewsId());
            row.addProperty("language", segment.getLanguage());
            row.addProperty("source", segment.getSource());
            row.addProperty("topic_text", segment.getTopicText());
            row.addProperty("published_at", segment.getPublishedAtEpochMs());
            row.add("embedding", GSON.toJsonTree(toDoubleList(record.getEmbedding())));
            rows.add(row);
        }

        client.insert(InsertReq.builder()
                .collectionName(collectionName)
                .data(rows)
                .build());
    }

    @Override
    public List<TopicVectorRecord> queryAllRecords() {
        QueryResp queryResp = client.query(QueryReq.builder()
                .collectionName(collectionName)
                .filter("id >= 0")
                .outputFields(List.of("id", "news_id", "language", "source", "topic_text", "published_at", "embedding"))
                .limit(10000L)
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .build());

        List<TopicVectorRecord> records = new ArrayList<>();
        for (QueryResp.QueryResult result : queryResp.getQueryResults()) {
            Map<String, Object> entity = result.getEntity();
            long id = ((Number) entity.get("id")).longValue();
            String newsId = String.valueOf(entity.get("news_id"));
            String language = String.valueOf(entity.get("language"));
            String source = String.valueOf(entity.get("source"));
            String topicText = String.valueOf(entity.get("topic_text"));
            long publishedAt = ((Number) entity.get("published_at")).longValue();
            float[] embedding = toFloatArray(entity.get("embedding"));

            TopicSegment segment = new TopicSegment(newsId, extractTitle(topicText), language, source, publishedAt, topicText, 0);
            records.add(new TopicVectorRecord(id, segment, embedding));
        }
        records.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return records;
    }

    @Override
    public List<VectorNeighborHit> searchNeighbors(float[] queryVector, int topK) {
        Map<String, Object> searchParams = Map.of("nprobe", Math.max(4, MilvusSettings.IVF_NLIST / 4));
        SearchResp searchResp = client.search(SearchReq.builder()
                .collectionName(collectionName)
                .annsField("embedding")
                .topK(topK)
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .outputFields(List.of("id"))
                .searchParams(searchParams)
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .build());

        List<VectorNeighborHit> hits = new ArrayList<>();
        for (SearchResp.SearchResult result : searchResp.getSearchResults().get(0)) {
            long hitId = ((Number) result.getId()).longValue();
            hits.add(new VectorNeighborHit(hitId, 1.0 - result.getScore()));
        }
        return hits;
    }

    public Map<Long, Integer> queryIvfClusterDistributionHint() {
        // Milvus IVF 索引在构建时使用 K-Means 划分 nlist 个桶；此处通过 nlist 配置说明原生聚类能力
        Map<Long, Integer> hint = new HashMap<>();
        hint.put((long) MilvusSettings.IVF_NLIST, MilvusSettings.IVF_NLIST);
        return hint;
    }

    @Override
    public void close() {
        client.close();
    }

    private List<Double> toDoubleList(float[] vector) {
        List<Double> values = new ArrayList<>(vector.length);
        for (float v : vector) {
            values.add((double) v);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private float[] toFloatArray(Object raw) {
        if (raw instanceof List<?> list) {
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = ((Number) list.get(i)).floatValue();
            }
            return vector;
        }
        throw new IllegalStateException("Unsupported embedding payload: " + raw);
    }

    private String extractTitle(String topicText) {
        int pipe = topicText.indexOf('|');
        if (pipe > 0 && pipe + 1 < topicText.length()) {
            return topicText.substring(0, pipe).trim();
        }
        return topicText;
    }
}

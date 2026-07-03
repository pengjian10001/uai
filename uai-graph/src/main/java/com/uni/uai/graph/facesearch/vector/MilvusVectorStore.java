package com.uni.uai.graph.facesearch.vector;

import com.uni.uai.graph.facesearch.model.FaceRecord;
import com.uni.uai.graph.facesearch.model.MatchHit;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 企业级分布式方案：Milvus Standalone + Java SDK。
 */
public final class MilvusVectorStore implements VectorStore {

    public static final String COLLECTION = "video_face";
    private static final String VECTOR_FIELD = "face_vector";
    private static final String ID_FIELD = "id";

    private final MilvusServiceClient client;
    private final int dimension;

    public MilvusVectorStore(String host, int port, int dimension, boolean recreate) {
        this.dimension = dimension;
        this.client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build());
        if (recreate) {
            dropIfExists();
            createCollection();
        } else if (!collectionExists()) {
            createCollection();
        }
    }

    private boolean collectionExists() {
        R<Boolean> response = client.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(COLLECTION).build());
        return Boolean.TRUE.equals(response.getData());
    }

    private void dropIfExists() {
        if (collectionExists()) {
            client.dropCollection(DropCollectionParam.newBuilder().withCollectionName(COLLECTION).build());
        }
    }

    private void createCollection() {
        List<FieldType> fields = new ArrayList<>();
        fields.add(FieldType.newBuilder()
                .withName(ID_FIELD)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build());
        fields.add(FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build());
        fields.add(FieldType.newBuilder()
                .withName("vid")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build());
        fields.add(FieldType.newBuilder()
                .withName("frame_no")
                .withDataType(DataType.Int64)
                .build());
        fields.add(FieldType.newBuilder()
                .withName("time_sec")
                .withDataType(DataType.Double)
                .build());

        client.createCollection(CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION)
                .withFieldTypes(fields)
                .build());

        client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION)
                .withFieldName(VECTOR_FIELD)
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .build());
        flushCollection();
    }

    @Override
    public void insertBatch(List<FaceRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        List<List<Float>> vectors = new ArrayList<>();
        List<String> videoIds = new ArrayList<>();
        List<Long> frameNos = new ArrayList<>();
        List<Double> timeSecs = new ArrayList<>();

        for (FaceRecord record : records) {
            List<Float> vector = new ArrayList<>(record.embedding().length);
            for (float v : record.embedding()) {
                vector.add(v);
            }
            vectors.add(vector);
            videoIds.add(record.videoId());
            frameNos.add(record.frameNo());
            timeSecs.add(record.timeSec());
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(VECTOR_FIELD, vectors));
        fields.add(new InsertParam.Field("vid", videoIds));
        fields.add(new InsertParam.Field("frame_no", frameNos));
        fields.add(new InsertParam.Field("time_sec", timeSecs));

        R<MutationResult> response = client.insert(InsertParam.newBuilder()
                .withCollectionName(COLLECTION)
                .withFields(fields)
                .build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus insert failed: " + response.getMessage());
        }
        flushCollection();
    }

    private void flushCollection() {
        client.flush(FlushParam.newBuilder().addCollectionName(COLLECTION).build());
    }

    @Override
    public List<MatchHit> search(float[] queryEmbedding, int topK, double minScore) {
        List<Float> query = new ArrayList<>(queryEmbedding.length);
        for (float v : queryEmbedding) {
            query.add(v);
        }

        R<SearchResults> response = client.search(SearchParam.newBuilder()
                .withCollectionName(COLLECTION)
                .withVectorFieldName(VECTOR_FIELD)
                .withVectors(Collections.singletonList(query))
                .withTopK(topK)
                .withMetricType(MetricType.COSINE)
                .withOutFields(List.of("vid", "frame_no", "time_sec"))
                .build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus search failed: " + response.getMessage());
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<MatchHit> hits = new ArrayList<>();
        if (wrapper.getRowRecords(0).isEmpty()) {
            return hits;
        }
        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            double score = wrapper.getIDScore(0).get(i).getScore();
            if (score < minScore) {
                continue;
            }
            var row = wrapper.getRowRecords(0).get(i);
            String videoId = row.get("vid").toString();
            long frameNo = Long.parseLong(row.get("frame_no").toString());
            double timeSec = Double.parseDouble(row.get("time_sec").toString());
            hits.add(new MatchHit(videoId, frameNo, timeSec, score));
        }
        return hits;
    }

    @Override
    public long count() {
        return -1;
    }

    @Override
    public void clear() {
        dropIfExists();
        createCollection();
    }

    @Override
    public void close() {
        client.close();
    }
}

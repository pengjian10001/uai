package com.uni.uai.vec.example.milvus.model;

import java.util.List;

public class HotTopicCluster {

    private final int clusterId;
    private final int size;
    private final List<TopicVectorRecord> members;
    private final String summary;

    public HotTopicCluster(int clusterId, int size, List<TopicVectorRecord> members, String summary) {
        this.clusterId = clusterId;
        this.size = size;
        this.members = members;
        this.summary = summary;
    }

    public int getClusterId() {
        return clusterId;
    }

    public int getSize() {
        return size;
    }

    public List<TopicVectorRecord> getMembers() {
        return members;
    }

    public String getSummary() {
        return summary;
    }
}

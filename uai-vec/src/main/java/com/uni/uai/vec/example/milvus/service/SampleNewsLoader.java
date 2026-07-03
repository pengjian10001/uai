package com.uni.uai.vec.example.milvus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.uai.vec.example.milvus.model.NewsArticle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SampleNewsLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<NewsArticle> load() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/sample-news.json")) {
            if (inputStream == null) {
                throw new IOException("sample-news.json not found in classpath");
            }
            return objectMapper.readValue(inputStream, new TypeReference<List<NewsArticle>>() {
            });
        }
    }
}

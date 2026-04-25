package com.uni.uai.rag.llm;

import java.time.Duration;

import com.uni.ubag.common.util.JSONUtil;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

public class EmbeddingModelFactory {
	private static EmbeddingModelFactory instance = new EmbeddingModelFactory();
	public static EmbeddingModelFactory getInstance() {
		return instance;
	}
	
	static EmbeddingModel model = null;
	static {
		model = OpenAiEmbeddingModel.builder()
				.baseUrl("https://openapi-ait.ke.com/v1")
			    .apiKey("ba7d8f57-7b1e-4e46-b5ba-38697c018148")
                .modelName("text-embedding-ada-002")
                .logRequests(true) //如果你想在日志中查看通信情况
			    .logResponses(true)
			    .timeout(Duration.ofMillis(20000))  // 设置超时时间（20秒）
                .build();
	}
	
	public EmbeddingModel getDefaultEmbeddingModel() {
		return model;
	}
	
	public static void main(String[] args) {
		EmbeddingModel model = EmbeddingModelFactory.getInstance().getDefaultEmbeddingModel();
		Response<Embedding> response = model.embed("你好");
        Embedding embedding = response.content();
        float[] vector = embedding.vector();
        System.out.println(vector.length);
        System.out.println(JSONUtil.toJSONString(vector));
        
	}

}

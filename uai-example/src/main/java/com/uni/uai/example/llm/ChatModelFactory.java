package com.uni.uai.example.llm;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class ChatModelFactory {
	private static ChatModelFactory instance = new ChatModelFactory();
	public static ChatModelFactory getInstance() {
		return instance;
	} 
	
	private Map<String, ChatModel> models = new HashMap<String, ChatModel>();
	
	static StreamingChatModel streamingChatmodel = null;
	static ChatModel model = null;
	static {
		String baseUrl = getConfig("UAI_LLM_BASE_URL", "uai.llm.base-url", "http://langchain4j.dev/demo/openai/v1");
		String modelName = getConfig("UAI_LLM_MODEL_NAME", "uai.llm.model-name", "gpt-4o-mini");
		//String apiKey = getRequiredConfig("UAI_LLM_API_KEY", "uai.llm.api-key");

		String apiKey = "demo";
		model = OpenAiChatModel.builder()
				.baseUrl("http://langchain4j.dev/demo/openai/v1")
			    .apiKey(apiKey)
			    .modelName(OpenAiChatModelName.GPT_4_O_MINI)
			    .logRequests(true) //如果你想在日志中查看通信情况
			    .logResponses(true)
			    //.strictJsonSchema(true)
			    .build();
		
		
		/**model = OpenAiChatModel.builder()
				.baseUrl(baseUrl)
			    .apiKey(apiKey)
			    //.httpClientBuilder(new MyHttpClientBuilder())  //自定义httpclient
			    //.modelName(OpenAiChatModelName.GPT_4_O_MINI)
			    .modelName(modelName)
			    //.modelName("deepseek-chat")
			    .listeners(List.of(new MyChatModelListener())) //注册模型请求监听器
			    .logRequests(true) //如果你想在日志中查看通信情况
			    .logResponses(true)
			    .timeout(Duration.ofMillis(120000))  // 设置超时时间（120秒）
			    //.strictJsonSchema(true)
			    .build();
		**/
		
		try {
			streamingChatmodel = OpenAiStreamingChatModel.builder()
					.baseUrl(baseUrl)
				    .apiKey(apiKey)
				    .modelName(modelName)
				    .logRequests(true) //如果你想在日志中查看通信情况
				    .logResponses(true)
				    .timeout(Duration.ofMillis(120000))  // 设置超时时间（120秒）
				    .build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static String getConfig(String envKey, String propertyKey, String defaultValue) {
		String value = System.getenv(envKey);
		if(value == null || value.trim().isEmpty()) {
			value = System.getProperty(propertyKey);
		}
		if(value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		return value;
	}

	private static String getRequiredConfig(String envKey, String propertyKey) {
		String value = getConfig(envKey, propertyKey, null);
		if(value == null || value.trim().isEmpty()) {
			throw new IllegalStateException(
				"Missing LLM API key. Please set environment variable " + envKey
				+ " or JVM property " + propertyKey + "."
			);
		}
		return value;
	}
	
	public ChatModel getDefaultChatModel() {
		return model;
	}
	
	public StreamingChatModel getDefaultStreamingChatModel() {
		return streamingChatmodel;
	}

}

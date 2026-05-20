package com.uni.uai.mcp.llm;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uni.uai.mcp.llm.listener.MyChatModelListener;
import com.uni.uai.mcp.utils.EnvFileLoader;
import com.uni.uai.mcp.utils.YmlConfigUtil;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * LLM 模型工厂：统一创建阻塞模型与流式模型。
 * <p>
 * MCP 对话流程中，工具判定阶段使用阻塞模型（{@link #getDefaultChatModel()}），
 * 最终回答阶段使用流式模型（{@link #getDefaultStreamingChatModel()}）。
 * 两者必须共用相同的 baseUrl / modelName / apiKey，否则可能出现流式阶段中文乱码。
 * </p>
 * <p>
 * 配置来源及优先级见 {@link #resolveConfig(String, String, String)}。
 * 启动时会先调用 {@link EnvFileLoader#loadIfPresent()} 加载 .env。
 * </p>
 */
public class ChatModelFactory {
	private static ChatModelFactory instance = new ChatModelFactory();
	public static ChatModelFactory getInstance() {
		return instance;
	} 
	
	private Map<String, ChatModel> models = new HashMap<String, ChatModel>();
	
	static StreamingChatModel streamingChatmodel = null;
	static ChatModel model = null;
	static {
		// 必须在 resolveConfig 之前：将 .env 中的 UAI_LLM_* 注入 System Property
		EnvFileLoader.loadIfPresent();

		String baseUrl = resolveConfig("UAI_LLM_BASE_URL", "uai.llm.base-url", "https://api.deepseek.com");
		String modelName = resolveConfig("UAI_LLM_MODEL_NAME", "uai.llm.model-name", "deepseek-chat");
		String apiKey = resolveConfig("UAI_LLM_API_KEY", "uai.llm.api-key", null);
		if (apiKey == null || apiKey.trim().isEmpty()) {
			// langchain4j.dev demo 端点允许无 key
			if (baseUrl.contains("langchain4j.dev")) {
				apiKey = "demo";
			} else {
				throw new IllegalStateException(
					"Missing LLM API key. Please set environment variable UAI_LLM_API_KEY"
					+ " or JVM property uai.llm.api-key."
				);
			}
		}

		model = OpenAiChatModel.builder()
				.baseUrl(baseUrl)
			    .apiKey(apiKey)
			    .modelName(modelName)
			    .listeners(List.of(new MyChatModelListener()))
			    .logRequests(true)
			    .logResponses(true)
			    .timeout(Duration.ofMillis(120000))
			    .build();
		
		streamingChatmodel = OpenAiStreamingChatModel.builder()
				.baseUrl(baseUrl)
			    .apiKey(apiKey)
			    .modelName(modelName)
			    .logRequests(true)
			    .logResponses(true)
			    .timeout(Duration.ofMillis(120000))
			    .build();
	}

	/**
	 * 按优先级解析单项配置：
	 * <ol>
	 *   <li>OS 环境变量（如 {@code UAI_LLM_API_KEY}）</li>
	 *   <li>JVM System Property（如 {@code uai.llm.api-key}，含 .env 注入的值）</li>
	 *   <li>{@code application-{profile}.yml}（通过 {@link YmlConfigUtil}）</li>
	 *   <li>传入的 {@code defaultValue}</li>
	 * </ol>
	 */
	private static String resolveConfig(String envKey, String propertyKey, String defaultValue) {
		String value = System.getenv(envKey);
		if (value == null || value.trim().isEmpty()) {
			value = System.getProperty(propertyKey);
		}
		if (value == null || value.trim().isEmpty()) {
			value = YmlConfigUtil.getInstance().getYmlConfigValue(propertyKey);
		}
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		return value;
	}
	
	/** 阻塞模型，用于 MCP 工具判定等同步调用 */
	public ChatModel getDefaultChatModel() {
		return model;
	}
	
	/** 流式模型，用于 SSE 最终回答的 token 级推送 */
	public StreamingChatModel getDefaultStreamingChatModel() {
		return streamingChatmodel;
	}

}

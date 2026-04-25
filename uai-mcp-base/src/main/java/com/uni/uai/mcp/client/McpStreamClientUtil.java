package com.uni.uai.mcp.client;

import java.util.Map;

import com.uni.uai.mcp.chatmemory.ChatMemoryWithStore;
import com.uni.uai.mcp.utils.JacksonUtil;
import com.uni.uai.mcp.utils.context.TemplateSseUtil;
import com.uni.uai.mcp.utils.context.TemplateSseUtil.SseContext;
import com.uni.uai.mcp.utils.context.TemplateUtil;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.client.McpClient;

public class McpStreamClientUtil extends McpClientUtilBase{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static McpStreamClientUtil instance = new McpStreamClientUtil();
	public static McpStreamClientUtil getInstance() {
		return instance;
	}
	
	
	/**
	 * 仅单纯测试LLM，与query()对比:
	 * 1.不同的是，没有db相关操作，例如chatMemory、label等。
	 * 2.相同的是，仍使用mcp，有工具调用，因此仍通过循环调用LLM
	 * @param queryTextRegin
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	@Override
	public String queryLLMPure(final String queryTextRegin, Map<String, Object> properties) throws Exception{
		QueryProperty queryProperty = JacksonUtil.getInstance().mapToObject(properties, QueryProperty.class);
		String queryText = this.handleQueryTextAndQueryProperty(queryTextRegin, queryProperty);
		String logKey = "llm-query-strem" + TemplateUtil.getInstance().substring(queryTextRegin, 10) + "...";
		if(queryText==null || queryText.trim().length()==0) {
			//如果用户问题为空，则快速返回
			SseContext context = TemplateSseUtil.getInstance().getSseContext();
			context.send("\n\n");
			context.complete();
		}else {
			String sessionId = queryProperty.getSessionId();
			String clientName = queryProperty.getClientName();
			
			dev.langchain4j.data.message.SystemMessage systemMessage = this.getSystemPrompt(clientName);
			UserMessage userMessage = getUserPrompt(queryText, queryProperty); //用户查询构建UserMessage
			//--1.记录ChatMemory
	        ChatMemoryWithStore chatMemory = this.getAndSetChatMemory(sessionId, systemMessage, userMessage);
			
			this.invokeModelStream(chatMemory, logKey);
			//stream方式调用LLM，是异步，需要在onComplete中执行chatMemory.flushToDB()
			//chatMemory.flushToDB();
		}
		return "";
	}
	
	/**
	 * 带有mcp的LLM请求
	 */
	@Override
	public String query(final String queryTextRegin, Map<String, Object> properties) throws Exception{
		QueryProperty queryProperty = JacksonUtil.getInstance().mapToObject(properties, QueryProperty.class);
		String queryText = this.handleQueryTextAndQueryProperty(queryTextRegin, queryProperty);
		String logKey = "llm-query-strem" + TemplateUtil.getInstance().substring(queryTextRegin, 10) + "...";
		if(queryText==null || queryText.trim().length()==0) {
			//如果用户问题为空，则快速返回
			SseContext context = TemplateSseUtil.getInstance().getSseContext();
			context.send("\n\n");
			context.complete();
		}else {
			String sessionId = queryProperty.getSessionId();
			String clientName = queryProperty.getClientName();
			dev.langchain4j.data.message.SystemMessage systemMessage = this.getSystemPrompt(clientName);
			UserMessage userMessage = getUserPrompt(queryText, queryProperty); //用户查询构建UserMessage
			//--1.记录ChatMemory
	        ChatMemoryWithStore chatMemory = this.getAndSetChatMemory(sessionId, systemMessage, userMessage);
			
			McpClient mcpClient = null;
			try {
				mcpClient = getMcpClient(clientName);
				this.invokeModelStreamWithMcp(mcpClient, chatMemory, logKey);
				//stream方式调用LLM，是异步，需要在onComplete中执行chatMemory.flushToDB()
				//chatMemory.flushToDB();
			} finally {
				mcpClient.close();
			}
		}
		return "";
	}
	

	

	
	
	
	
}

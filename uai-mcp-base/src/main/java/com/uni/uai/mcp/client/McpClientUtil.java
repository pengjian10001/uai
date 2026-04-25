package com.uni.uai.mcp.client;

import java.util.List;
import java.util.Map;

import com.uni.uai.mcp.chatmemory.ChatMemoryWithStore;
import com.uni.uai.mcp.utils.JacksonUtil;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.RegexUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;

public class McpClientUtil extends McpClientUtilBase{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static McpClientUtil instance = new McpClientUtil();
	public static McpClientUtil getInstance() {
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
		if(queryText==null || queryText.trim().length()==0) {
			return "";
		}else {
			String sessionId = queryProperty.getSessionId();
			String clientName = queryProperty.getClientName();
			
			dev.langchain4j.data.message.SystemMessage systemMessage = this.getSystemPrompt(clientName);
			UserMessage userMessage = getUserPrompt(queryText, queryProperty); //用户查询构建UserMessage
			//--1.记录ChatMemory
	        ChatMemoryWithStore chatMemory = this.getAndSetChatMemory(sessionId, systemMessage, userMessage);
			
	        ChatModel chatLanguageModel = getChatModel();
	        AiMessage aiMessage = invokeModel(chatLanguageModel, chatMemory);
	        //--7.将chatMemory中的数据刷入数据库
	        //TODO 如果以上方法报错，此处就不会入库
	        //此方法不要放到finally中，因为如果带有工具的调用，如果工具的message添加到memory，但是llm最终报错导致AiMessage没有进入chatmemory，会导致下次请求chatmemory不完整而无法使用chatmemory
	        chatMemory.flushToDB();
	        String result = aiMessage.text();
			result = this.handlLLMResult(result);
			System.out.println("queryLLMPure结果：" + result);
			return result;
		}
	}
	
	public String query(final String queryTextRegin, Map<String, Object> properties) throws Exception{
		QueryProperty queryProperty = JacksonUtil.getInstance().mapToObject(properties, QueryProperty.class);
		String queryText = this.handleQueryTextAndQueryProperty(queryTextRegin, queryProperty);
		if(queryText==null || queryText.trim().length()==0) {
			return "";
		}else {
			String sessionId = queryProperty.getSessionId();
			String clientName = queryProperty.getClientName();
			dev.langchain4j.data.message.SystemMessage systemMessage = this.getSystemPrompt(clientName);
			UserMessage userMessage = getUserPrompt(queryText, queryProperty); //用户查询构建UserMessage
			//--1.记录ChatMemory
	        ChatMemoryWithStore chatMemory = this.getAndSetChatMemory(sessionId, systemMessage, userMessage);
	        ChatModel chatLanguageModel = getChatModel();
			McpClient mcpClient = null;
			try {
				mcpClient = getMcpClient(clientName);
				AiMessage aiMessage = invokeModelWithMcp(mcpClient, chatLanguageModel, chatMemory);
				//--7.将chatMemory中的数据刷入数据库
		        //TODO 如果以上方法报错，此处就不会入库，
				//此方法不要放到finally中，因为如果带有工具的调用，如果工具的message添加到memory，但是llm最终报错导致AiMessage没有进入chatmemory，会导致下次请求chatmemory不完整而无法使用chatmemory
		        chatMemory.flushToDB();
				String result = aiMessage.text();
				result = this.handlLLMResult(result);
				System.out.println("query结果：" + result);
				return result;
			} finally {
				mcpClient.close();
			}
		}
	}
	
	public static void main(String[] args) {
		String queryText = "  @abc,aaaaaaa";
		List<String> list = RegexUtil.findMatchs(queryText, matchClientNameRegex, 1);
		System.out.println(list);
		queryText = "@_mcp，有哪些角色，用html表格返回";
		list = RegexUtil.findMatchs(queryText, matchClientNameRegex, 1);
		System.out.println(list);
		
		queryText = "aaaaaaa";
		list = RegexUtil.findMatchs(queryText, matchClientNameRegex, 1);
		System.out.println(list);
	}
	

}

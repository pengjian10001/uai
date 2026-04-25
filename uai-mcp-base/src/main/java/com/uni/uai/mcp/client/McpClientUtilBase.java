package com.uni.uai.mcp.client;

import java.io.FileInputStream;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.chatmemory.ChatMemoryWithStore;
import com.uni.uai.mcp.common.FileInfo;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.llm.ChatModelFactory;
import com.uni.uai.mcp.model.LabelPO;
import com.uni.uai.mcp.server.McpExtendUtil;
import com.uni.uai.mcp.server.ServerUtil;
import com.uni.uai.mcp.utils.JSONUtil;
import com.uni.uai.mcp.utils.context.TemplateSseUtil;
import com.uni.uai.mcp.utils.context.TemplateUtil;
import com.uni.uai.mcp.utils.context.TemplateSseUtil.SseContext;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.FileUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.log.proxy.ProxyAction;
import com.uni.ubag.log.util.UbagLogUtil;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolExecutionResult;

public abstract class McpClientUtilBase {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	//提取 “@clientname，这个要放在前面，避免用户问题中包含Java注解（例如@Discription等）而识别为clientName。
	//用户问题”中的clientname，clientname和用户问题之间，可以有空白，标点符号，中文标点，clientname名字中可以包含_-
	protected static final String matchClientNameRegex = "^[\\s]*@[\\\\s]*((_|-|[^\\s\\p{Punct}\\p{Zs}\\u3000-\\u303F\\uFF01-\\uFF65])*)(.*)";
	
	/**
	 * 获取同步请求的LLM
	 * @return
	 */
	protected ChatModel getChatModel() {
		ChatModel model = ChatModelFactory.getInstance().getDefaultChatModel();
		return model;
	}
	
	/**
	 * 获取流式输出的LLM
	 * @return
	 */
	private StreamingChatModel getStreamChatModel() {
		StreamingChatModel model = ChatModelFactory.getInstance().getDefaultStreamingChatModel();
		return model;
	}
	
	
	private String getSSEFullUrl() {
		String url = UbagConf.getString(UaiConf.MCP_SERVER_URL, null);
		if(url != null) {
			TimeTrace.markCall("serverurl-从uaiconf中获取="+url, 0L, 0L);
			return url + "/sse";
		}
		url = UbagConf.getString(UbagConf.WebConf.request_full_url, null);
		if(url != null) {
			//获取服务器的host，例如http://localhost:8080
			List<String> list = RegexUtil.findMatchs(url, "(http[s]?://[^/]+)/.*", 1);
			if(list != null && list.size() > 0) {
				TimeTrace.markCall("serverurl-从fullurl中获取="+url, 0L, 0L);
				return list.get(0) + "/sse";
			}
		}
		TimeTrace.markCall("serverurl-从默认值中获取="+url, 0L, 0L);
		return "http://localhost:8080/sse";
	}
	
	protected McpClient getMcpClient(String clientName) {
		String sseUrl = this.getSSEFullUrl(); // 如果要查看线上情况，可直接替换为线上的"http://mcp-odin.api.ke.com/sse"
		System.out.println("sseUrl:" + sseUrl);
		//附加logId
		//sseUrl = TemplateUtil.getInstance().appendParam(sseUrl, "logId", UbagConf.getlogId());
		McpTransport transport = new HttpMcpTransport.Builder()
			    .sseUrl(sseUrl)
			    .logRequests(true) //如果你想在日志中查看通信情况
			    .logResponses(true)
			    .build();
		McpClient mcpClient = new DefaultMcpClient.Builder()
				.clientName(clientName)
			    .transport(transport)
			    .toolExecutionTimeout(Duration.ofSeconds(120))  //设置工具执行60s超时，默认60s
			    .resourcesTimeout(Duration.ofSeconds(60))
			    .promptsTimeout(Duration.ofSeconds(60))
			    .build(); 
		return mcpClient;
	}
	
	protected List<ToolSpecification> listTools(String clientName) throws Exception{
		List<ToolSpecification> tools = null;
		McpClient mcpClient = null;
		try {
			mcpClient = this.getMcpClient(clientName);
			tools = mcpClient.listTools();
		} finally {
			mcpClient.close();
		}
		return tools;
	}
	
	/**
	 * 生成并记录ChatMessage
	 * @param sessionId
	 * @param chatMessages
	 * @return
	 */
	protected ChatMemoryWithStore getAndSetChatMemory(String sessionId, ChatMessage... chatMessages) {
		ChatMemoryWithStore chatMemory = new ChatMemoryWithStore(sessionId, UaiConf.CHAT_MEMORY_MAX_MESSAGE);
		for(int i = 0; i < chatMessages.length; i++) {
			ChatMessage chatMessage = chatMessages[i];
			if(chatMessage!=null) {
				chatMemory.add(chatMessage);
			}
		}
		return chatMemory;
	}
	
	/**
	 * 根据用户文本处理QueryProperty
	 * @param queryTextRegin
	 * @param queryProperty
	 */
	protected String handleQueryTextAndQueryProperty(String queryTextRegin, QueryProperty queryProperty) {
		String clientName = queryProperty.getClientName();
		String queryText = queryTextRegin;
		//如果query以 @XXX开头，则提取XXX作为clientName
		List<String> list = RegexUtil.findMatchs(queryText, matchClientNameRegex, 1);
		if(list != null && list.size() > 0) {
			//如果从queryText中获取到clientName，则替换参数传入的clientName
			clientName = list.get(0);
			queryProperty.setClientName(clientName);
			//去掉@clientname之后的用户问题
			queryText = RegexUtil.findMatchs(queryText, matchClientNameRegex, 3).get(0);
		}
		//根据用户文本，设置logId，这样处理能与工具调用传递的logId打通
		McpExtendUtil.setLogIdFromQuery(queryText);
		//返回修改后的queryText
		return queryText;
	}
	
	protected dev.langchain4j.data.message.SystemMessage getSystemPrompt(String clientName){
		String systemText = "";
		//clientName对应标签名，即角色，可以从标签表获取对应的角色描述
		LabelPO labelpo = ServerUtil.getInstance().getLabelPO(clientName);
		if(labelpo != null) {
			systemText = labelpo.getDescription();
		}else {
			systemText = PromptFactory.defaultSystemPrompt();
		}
		dev.langchain4j.data.message.SystemMessage systemMessage = null;
        if(systemText != null && systemText.trim().length() > 0) {
        	systemMessage = dev.langchain4j.data.message.SystemMessage.from(systemText);
        }else {
        	systemMessage= dev.langchain4j.data.message.SystemMessage.from("""
                    无论用户输入什么语言，如果没有显示指定以某种语言输出，则默认用简体中文回复。\n
                    """);
        }
        return systemMessage;
	}
	
	protected UserMessage getUserPrompt(String queryText, QueryProperty queryProperty) throws Exception{
		//封装所有用户相关的内容
		List<Content> contents = new ArrayList<Content>();
		//1. 直接封装用户的提问为TextContent
		String userStrengMsg = PromptFactory.strengToolUserPrompt(queryText); 
		TextContent textContent = TextContent.from(userStrengMsg);
		contents.add(textContent);
		//TODO 还可以补充其他信息，例如ucid，logId等
		
		//2. 查看是否有上传文件
		List<FileInfo> fileInfoList = queryProperty.getFileInfoList();
		if(fileInfoList != null && fileInfoList.size() > 0) {
			for(FileInfo fileInfo : fileInfoList) {
				TimeTrace.markCall(String.format("fileInfo to UserMessage, %s, %s", fileInfo.getFileName(), fileInfo.getContentType()), 0L, 0L);
				String contentType = fileInfo.getContentType();
				String tempFilePath = fileInfo.getTempFilePath();
				if(contentType == null) {
					continue;
				}
				File file = new File(tempFilePath);
				URI uri = file.toURI();
				if(contentType.contains("text")) {
					String text = FileUtil.getInstance().getFileContent(file.getAbsolutePath());
					contents.add(TextContent.from(text));
					//contents.add(TextFileContent.from(uri));
				}else if(contentType.contains("image")) {
			        // 创建字节数组，大小为文件长度
			        byte[] fileContent = new byte[(int) file.length()];
			        // 使用文件输入流读取文件内容
			        try (FileInputStream fis = new FileInputStream(file)) {
			            fis.read(fileContent);
			        }
			        // 使用Java内置的Base64编码器进行编码
			        String base64Str = Base64.getEncoder().encodeToString(fileContent);
					contents.add(ImageContent.from(base64Str, contentType));
				}else if(contentType.contains("audio")) {
					contents.add(AudioContent.from(uri));
				}else if(contentType.contains("video")) {
					contents.add(VideoContent.from(uri));
				}else if(contentType.contains("pdf")) {
					contents.add(PdfFileContent.from(uri));
				}else if(contentType.contains("excel") 
						|| contentType.contains("sheet")) {
					//application/vnd.ms-excel 或 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
					//对于excel文件，读取到sqlite中，读取逻辑在JsonSqlContentRetriever工具中，其能对Sqlite中的数据进行分析
					//UaiConf.setExcelFileInfo(fileInfo);
					//由于通过mcp client调用工具，会重新发起http请求，所以，此处上传的信息，不能通过Threadlocal传递，而是需要通过消息传递
					// TODO 由于mcp client调用tools，会另发起http请求，所以在分布式部署时，应该把文件上传到s3等，在mcp工具使用时，也从远程获取。
					TextContent contentFile = TextContent.from(String.format("用户上传excel文件信息: %s", JSONUtil.getInstance().toJSONString(fileInfo)));
					contents.add(contentFile);
					TextContent content = TextContent.from(String.format("使用名为[%s]的工具回答。", ServerUtil.getInstance().jsonSqlAnalysisToolName));
					contents.add(content);
				}else if(contentType.contains("word")) {
					//application/msword 或 application/vnd.openxmlformats-officedocument.wordprocessingml.document
					//TODO
				}else{
					TimeTrace.markCall("无法识别的contentType="+contentType, 0L, 0L);
				}
			}
		}
		
		//3. 将所有用户内容封装到UserMessage中
        UserMessage userMessage = UserMessage.from(contents);
        return userMessage;
	}
	
	/**
	 * 通过流式调用LLM
	 * @param chatMemory
	 * @param logKey
	 */
	protected void invokeModelStream(ChatMemoryWithStore chatMemory, String logKey) {
		SseContext context = TemplateSseUtil.getInstance().getSseContext();
		StreamingChatResponseHandler handler = new MyStreamingChatResponseHandler(context, chatMemory, logKey, UbagConf.getlogId());
		ChatRequest chatRequest = ChatRequest.builder()
        		//.messages(chatMessages)
        		.messages(chatMemory.messages())
        		//.toolSpecifications(toolSpecifications)
        		.build();
		
		StreamingChatModel model = getStreamChatModel();
		//异步chat，没有返回值
		model.chat(chatRequest, handler);
	}
	
	/**
	 * 带着MCP服务，流式调用LLM
	 * @param mcpClient
	 * @param chatMemory
	 * @param logKey
	 * @throws Exception
	 */
	protected void invokeModelStreamWithMcp(McpClient mcpClient, ChatMemoryWithStore chatMemory, String logKey) throws Exception {
		//--2.获取所有可用工具
		List<ToolSpecification> toolSpecifications = mcpClient.listTools();
		//logger.info("总共有" + toolSpecifications.size() + "个工具");
		TimeTrace.markCall("总共有" + toolSpecifications.size() + "个工具", 0L, 0L);
		//对于工具调用，采用阻塞的方式调用LLM获取结果。
		ChatModel chatLanguageModel = getChatModel();
		AiMessage aiMessage = this.invokeModelWithMcp(mcpClient, chatLanguageModel, chatMemory);
		if(aiMessage.hasToolExecutionRequests()) {
			//如果有工具请求，则表明这不是一个最终的AI输出，还需要继续调用LLM，此时不关闭context
			List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
			this.executeTool(mcpClient, chatMemory, toolExecutionRequests);
			
		}
		//执行完工具调用后，再使用流式
		//带mcp调用的流失，默认请求两次模型，如果没有工具，此处也无需第一次模型的调用直接输出。
		//如果希望无工具调用时，只调用一次模型，则封装一个API，调用queryLLMPure方法即可
		this.invokeModelStream(chatMemory, logKey);
	}
	
	/**
	 * 阻塞方式调用LLM。无工具调用
	 * @param model
	 * @param chatMemory
	 * @return
	 */
	protected AiMessage invokeModel(ChatModel model, ChatMemoryWithStore chatMemory) {
		AiMessage result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "invokeModel", String.format("model=%s, chatMemory_size=%s", model.provider().name(), chatMemory.messages().size()), new ProxyAction<AiMessage>() {
			public AiMessage exec() throws Throwable{
				ChatRequest chatRequest = ChatRequest.builder()
		        		//.messages(chatMessages)
		        		//--2.由于使用chatmeomry，此处改为从chatmemory中获取
		        		.messages(chatMemory.messages())
		        		//.toolSpecifications(toolSpecifications)
		        		.build();
		        
		        ChatResponse response = model.chat(chatRequest);
		        AiMessage aiMessage = response.aiMessage();
		        //--3.记录ChatMemory
		        chatMemory.add(aiMessage);
		        return aiMessage;
			}
		});
		return result;
	}
	
	/**
	 * 以阻塞的方式，同步调用LLM，有工具调用
	 * @param mcpClient
	 * @param model
	 * @param chatMemory
	 * @param toolSpecifications
	 * @return
	 */
	protected AiMessage invokeModelWithMcp(McpClient mcpClient, ChatModel model, ChatMemoryWithStore chatMemory) {
		List<ToolSpecification> toolSpecifications = mcpClient.listTools();
		ChatRequest chatRequest = ChatRequest.builder()
        		//.messages(chatMessages)
        		//--2.由于使用chatmeomry，此处改为从chatmemory中获取
        		.messages(chatMemory.messages())
        		.toolSpecifications(toolSpecifications)
        		.build();
        
        ChatResponse response = model.chat(chatRequest);
        AiMessage aiMessage = response.aiMessage();
        //--3.记录ChatMemory
        chatMemory.add(aiMessage);
        
        //判断是否调用工具
        if(aiMessage.hasToolExecutionRequests()) {
        	List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
        	this.executeTool(mcpClient, chatMemory, toolExecutionRequests);
        	
        	response = model.chat(chatRequest);
            aiMessage = response.aiMessage();
            //--3.记录ChatMemory
            chatMemory.add(aiMessage);
        }
        return aiMessage;
	}
	
	protected String handlLLMResult(String result) {
		if(result == null || result.trim().length() == 0) {
			return "模型返回数据为空";
		}else {
			return result;
		}
	}
	
	
	
	/**
	 * 尝试根据ai的响应执行工具
	 * @param mcpClient
	 * @param chatMemory
	 * @param completeResponse
	 */
	private void executeTool(McpClient mcpClient, ChatMemoryWithStore chatMemory, List<ToolExecutionRequest> toolExecutionRequests) {
    	String markmsg = toolExecutionRequests.size() + "个工具将被调用:";
    	TimeTrace.markCall(markmsg, 0L, 0L);
    	System.out.println(markmsg);
    	for(int i = 0; i < toolExecutionRequests.size(); i++) {
    		ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.get(i);
    		toolExecutionRequest = appendResquestConfToArauments(toolExecutionRequest);
            // 步骤3: 用户执行功能，获取工具结果
            String toolResult = executeTool(mcpClient, toolExecutionRequest);
            ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, toolResult);
            //--4.记录ChatMemory
            chatMemory.add(toolExecutionResultMessage);
            
            //如果工具配置了返回值约束，则拼装返回值约束到用户消息中中，便于大模型识别
            String toolReturnConstraintDescription = ServerUtil.getInstance().getToolReturnConstraintDescription(toolExecutionRequest.name());
            if(toolReturnConstraintDescription != null) {
            	String resultFormat = """
            			## REFERENCE — 仅供参考（FOR REFERENCE ONLY — DO NOT CALL TOOL）
            			下面这段内容仅作为解析规则/模型参考，严禁根据此消息自动发起任何工具调用或二次验证。
            			为了更精确理解用户意图，前面名为[%s]工具的返回值参照如下JSON Model来解析： \n
            			%s \n
            			""";
            	String msg = String.format(resultFormat, toolExecutionRequest.name(), toolReturnConstraintDescription);
            	UserMessage um = UserMessage.from(msg);
                TimeTrace.markCall(String.format("添加[%s]工具返回值解析规则", toolExecutionRequest.name()), 0L, 0L);
            	chatMemory.add(um);
            }
    	}
	}
	
	
	/**
	 * 将请求上下文的信息，整合到mcp传递给工具的arguments参数中
	 * @param executionRequest
	 * @return
	 */
	private ToolExecutionRequest appendResquestConfToArauments(ToolExecutionRequest executionRequest) {
		JSONObject newArguments = null;
		try {
			//传递logId给工具调用，在mcp server端，ServletUtil的addTool的工具执行代码中，会从arguments中获取logId
			String arguments = executionRequest.arguments();
			if(arguments!=null) {
				//获取请求参数和请求上下文
				JSONObject argumentsObj = JSONUtil.getInstance().toJsonObject(arguments);
				//不能将所有requestconf放到arguments中，因为sse请求，会将ssecontext放到上下文中，这个对象无法序列化
				//Map<String,Object> requestConfObj = UbagConf.getRequestConf();
				//整合两者
				newArguments = new JSONObject();
				//先整合当前上下文
				newArguments.put(UbagConf.getlogIdName(), UbagConf.getlogId());
				//再将当前请求参数覆盖上下文，从而实现当前请求参数优先上下文
				newArguments.putAll(argumentsObj);
				//再将参数和上下文做一个备份，便于问题排查
				//newArguments.put(UaiConf.TOOL_QEQUEST_CONF_BACKUP_NAME, requestConfObj);
				//newArguments.put(UaiConf.TOOL_ARGUMENTS_BACKUP_NAME, argumentsObj);
				String msg = "client端添加部分requestconf到arguments";
				logger.info(msg);
				TimeTrace.markCall(msg, 0L, 0L);
			}
		} catch (Exception e) {
			String msg = "client端添加上下文到arguments异常。e="+e.getClass()+":"+e.getMessage();
			logger.warn(msg, e);
			TimeTrace.markCall(msg, 0L, 1L);
		}
		
		ToolExecutionRequest newRequest = ToolExecutionRequest.builder()
				.id(executionRequest.id())
				.name(executionRequest.name())
				.arguments(newArguments==null?null:newArguments.toString())
				.build();
		return newRequest;
	}
	
	/**
	 * 执行指定的工具请求
	 * @param mcpClient
	 * @param toolExecutionRequest
	 * @return
	 */
	private String executeTool(McpClient mcpClient, ToolExecutionRequest toolExecutionRequest) {
		String logKey = String.format("客户端调用[%s]工具", toolExecutionRequest.name());
		String result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.RPC, logKey, toolExecutionRequest.arguments(), new ProxyAction<String>() {
			public String exec() throws Throwable{
				//传递上下文（包括logId）给工具调用，在mcp server端，ServletUtil的addTool的工具执行代码中，会从arguments中获取logId
				//这部分代码在MyMcpClient中
				ToolExecutionRequest newRequest = extendToolExecutionRequest(toolExecutionRequest);
				ToolExecutionResult toolExecutionResult = mcpClient.executeTool(newRequest);
				String result = toolExecutionResult.resultText();
	    		logger.info(String.format("执行[%s]工具, 工具参数: %s, result=%s", toolExecutionRequest.name(), toolExecutionRequest.arguments(), TemplateUtil.getInstance().substring(result, 100) + "..."));
	    		return result;
			}

			@Override
			public String nullHandler(Throwable e) {
				String result = null;
				if(e!=null) {
					//TODO 工具执行异常不抛出。可优化，根据传递的参数，来定制错误时的行为。
					result = String.format("客户端调用[%s]工具异常，e=%s", toolExecutionRequest.name(),  e.getClass() + ":" + e.getMessage());
				}
				return result;
			}
			
		});
		return result;
	}
	
	/**
	 * 扩展ToolExecutionRequest，例如，加入日志id到参数中
	 * @param toolExecutionRequest
	 * @return
	 */
	private ToolExecutionRequest extendToolExecutionRequest(ToolExecutionRequest toolExecutionRequest) {
		try {
			String arguments = toolExecutionRequest.arguments();
			JSONObject obj = JSONUtil.getInstance().toJsonObject(arguments);
			if(!obj.containsKey("logId")) {
				obj.put("logId", UbagConf.getlogId());
			}
			ToolExecutionRequest newRequest = ToolExecutionRequest.builder()
					.id(toolExecutionRequest.id())
					.name(toolExecutionRequest.name())
					.arguments(obj.toString())
					.build();
			return newRequest;
		} catch (Exception e) {
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "extendToolExecutionRequest异常", ExceptionUtil.toStackTrace(e), e, false, 0L, String.format("name=%s,arguments=%s", toolExecutionRequest.name(),toolExecutionRequest.arguments()));
			//如果有异常，则不修改
			return toolExecutionRequest;
		}
	}
	
	/**
	 * 流式输出的LLM响应处理类
	 */
	private static class MyStreamingChatResponseHandler implements StreamingChatResponseHandler{
		long start = System.currentTimeMillis();
		String logKey;
		String logId;
		//StreamingChatModel会用异步的方式调用StreamingChatResponseHandler
		//所以，此处不要使用TemplateSseUtil.getInstance().send()方法，否则会报out is null
		//此处显示获取context
		//PrintWriter out = TemplateSseUtil.getInstance().getSseWriter();
		SseContext context;
		ChatMemoryWithStore chatMemory;
		public MyStreamingChatResponseHandler(SseContext context, ChatMemoryWithStore chatMemory, String logKey, String logId) {
			super();
			this.context = context;
			this.chatMemory = chatMemory;
			this.logKey = logKey;
			this.logId = logId;
		}
		@Override
		public void onPartialResponse(String partialResponse) {
			context.send(partialResponse);
		}
		@Override
		public void onPartialThinking(PartialThinking partialThinking) {
			if(partialThinking.text()!=null) {
				context.send(partialThinking.text());
			}
		}
		@Override
		public void onPartialToolCall(PartialToolCall partialToolCall) {
			// TODO Auto-generated method stub
			StreamingChatResponseHandler.super.onPartialToolCall(partialToolCall);
		}
		@Override
		public void onCompleteToolCall(CompleteToolCall completeToolCall) {
			// TODO Auto-generated method stub
			StreamingChatResponseHandler.super.onCompleteToolCall(completeToolCall);
		}
		@Override
		public void onCompleteResponse(ChatResponse completeResponse) {
			UbagConf.setlogId(logId);
			AiMessage aiMessage = completeResponse.aiMessage();
			String result = aiMessage.text();
			System.out.println("onCompleteResponse:" + result);
			//--3.记录ChatMemory
	        chatMemory.add(aiMessage);
			//--7.将chatMemory中的数据刷入数据库
	        //TODO 如果以上方法报错，此处就不会入库
	        //此方法不要放到finally中，因为如果带有工具的调用，如果工具的message添加到memory，但是llm最终报错导致AiMessage没有进入chatmemory，会导致下次请求chatmemory不完整而无法使用chatmemory
	        chatMemory.flushToDB();
	        TimeTrace.markCall("存入chatMemory", 0L, 0L);
	        context.complete();
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.RPC.getCode(), logKey, result, null, true, System.currentTimeMillis()-start, "", logId);
			
		}
		@Override
		public void onError(Throwable error) {
			UbagConf.setlogId(logId);
			String msg = String.format("onError: logId=%s，e=%s", UbagConf.getlogId(),  error.getClass() + ":" + error.getMessage());
			System.err.println();
			context.send(msg);
			context.complete();
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), logKey, ExceptionUtil.toStackTrace(error), error, false, System.currentTimeMillis()-start, msg, logId);
			
		}
		
		/**
		@Override
		public void onPartialResponse(String partialResponse) {
            context.send(partialResponse);
		}

		
		@Override
		public void onCompleteResponse(ChatResponse completeResponse) {
			UbagConf.setlogId(logId);
			AiMessage aiMessage = completeResponse.aiMessage();
			String result = aiMessage.text();
			System.out.println("onCompleteResponse:" + result);
			//--3.记录ChatMemory
	        chatMemory.add(aiMessage);
			//--7.将chatMemory中的数据刷入数据库
	        //TODO 如果以上方法报错，此处就不会入库
	        //此方法不要放到finally中，因为如果带有工具的调用，如果工具的message添加到memory，但是llm最终报错导致AiMessage没有进入chatmemory，会导致下次请求chatmemory不完整而无法使用chatmemory
	        chatMemory.flushToDB();
	        TimeTrace.markCall("存入chatMemory", 0L, 0L);
	        context.complete();
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.RPC.getCode(), logKey, result, null, true, System.currentTimeMillis()-start, "", logId);
		}

		@Override
		public void onError(Throwable error) {
			UbagConf.setlogId(logId);
			String msg = String.format("onError: logId=%s，e=%s", UbagConf.getlogId(),  error.getClass() + ":" + error.getMessage());
			System.err.println();
			context.send(msg);
			context.complete();
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), logKey, ExceptionUtil.toStackTrace(error), error, false, System.currentTimeMillis()-start, msg, logId);
		}
		**/
		
		
		

	}
	/**
	 * 纯调用大模型的请求，无工具调用
	 * @param queryTextRegin
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	protected abstract String queryLLMPure(final String queryTextRegin, Map<String, Object> properties) throws Exception;
	/**
	 * 附带工具调用的LLM请求
	 * @param queryTextRegin
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	protected abstract String query(final String queryTextRegin, Map<String, Object> properties) throws Exception;
}

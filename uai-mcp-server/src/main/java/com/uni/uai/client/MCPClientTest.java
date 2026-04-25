package com.uni.uai.client;

import java.time.Duration;
import java.util.Map;

import com.uni.ubag.common.util.JSONUtil;

import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Root;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

public class MCPClientTest {
	public static void main(String[] args) {
		
		McpClientTransport transport = new HttpClientSseClientTransport("http://localhost:8080");
		
		// 创建具有自定义配置的同步客户端
		McpSyncClient client = McpClient.sync(transport)
		    .requestTimeout(Duration.ofSeconds(10))
		    .capabilities(ClientCapabilities.builder()
		        .roots(true)      // 启用root功能
		        .sampling()       // 启用采样功能
		        .build())
		    //.sampling(request -> new CreateMessageResult(response))
		    .build();

		// 初始化连接
		client.initialize();

		// 列出可用的工具
		ListToolsResult tools = client.listTools();
		System.out.println("tools:" + JSONUtil.toJSONString(tools));

		// 调用一个工具
		CallToolResult result = client.callTool(
		    new CallToolRequest("test_tianqi", 
		        Map.of("location", "北京", "date", "2020-01-01"))
		);

		// 列出和读取resource
		/**
		ListResourcesResult resources = client.listResources();
		logger.info("resources:" + JSONUtil.toJSONString(resources));
		ReadResourceResult resource = client.readResource(
		    new ReadResourceRequest("resource://uri")
		);
		**/

		// 列出并使用prompts
		
		//ListPromptsResult prompts = client.listPrompts();
		//logger.info("prompts:" + JSONUtil.toJSONString(prompts));
		GetPromptResult prompt = client.getPrompt(
		    new GetPromptRequest("explain-code", Map.of("code", "var a=1"))
		);
		System.out.println("prompt:" + JSONUtil.toJSONString(prompt));
		

		// 添加/删除 roots
		client.addRoot(new Root("file:///path", "description"));
		client.removeRoot("file:///path");

		// 关闭client
		client.closeGracefully();
	}

}

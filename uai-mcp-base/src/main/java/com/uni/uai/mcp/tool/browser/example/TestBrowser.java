package com.uni.uai.mcp.tool.browser.example;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.uni.uai.mcp.llm.ChatModelFactory;

import dev.langchain4j.community.browser.playwright.PlaywrightBrowserExecutionEngine;
import dev.langchain4j.community.tool.browseruse.BrowserUseTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class TestBrowser {
	interface Assistant {
	    String chat(String userMessage);
	}

	public static void main(String[] args) {
		ChatModel model = ChatModelFactory.getInstance().getDefaultChatModel();
		
		// 1. 创建 Playwright 实例并配置浏览器启动参数
		Playwright playwright = Playwright.create();
		// 浏览器启动配置：非无头模式、使用Chrome通道、开启Chromium沙箱、操作延迟500毫秒
		BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
		        .setHeadless(false) // 非无头模式（可见浏览器界面）
		        .setChannel("chrome") // 使用Chrome浏览器通道
		        .setChromiumSandbox(true) // 开启Chromium沙箱，提升安全性
		        .setSlowMo(500); // 每个浏览器操作延迟500毫秒，便于观察
		// 启动Chrome浏览器
		Browser browser = playwright.chromium().launch(options);

		// 2. 构建智能体助手，集成浏览器工具
		Assistant assistant = AiServices.builder(Assistant.class)
		        .chatModel(model) // 配置聊天模型（需提前定义model实例）
		        // 集成Playwright浏览器执行引擎到BrowserUseTool工具
		        .tools(BrowserUseTool.from(PlaywrightBrowserExecutionEngine.builder().browser(browser).build()))
		        // 配置聊天记忆，最多保留10条消息
		        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
		        .build();

		// 3. 用自然语言发送浏览器操作指令，获取结果
		String question = "打开页面 'https://docs.langchain4j.dev/'，并总结该页面文本";
		// 执行指令并打印结果
		System.out.println(assistant.chat(question));


	}

}

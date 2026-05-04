package com.uni.uai.mcp.agent.example;

import com.uni.uai.mcp.agent.example.TestBase;
import com.uni.uai.mcp.llm.ChatModelFactory;

import dev.langchain4j.model.chat.ChatModel;

public class TestBase {
	static ChatModel BASE_MODEL = ChatModelFactory.getInstance().getDefaultChatModel();
}

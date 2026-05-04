package com.uni.uai.example.agent;

import com.uni.uai.example.agent.TestBase;
import com.uni.uai.example.llm.ChatModelFactory;

import dev.langchain4j.model.chat.ChatModel;

public class TestBase {
	static ChatModel BASE_MODEL = ChatModelFactory.getInstance().getDefaultChatModel();
}

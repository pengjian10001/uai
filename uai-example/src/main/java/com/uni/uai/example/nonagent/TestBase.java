package com.uni.uai.example.nonagent;

import com.uni.uai.example.llm.ChatModelFactory;

import dev.langchain4j.model.chat.ChatModel;

public class TestBase {
	static ChatModel BASE_MODEL = ChatModelFactory.getInstance().getDefaultChatModel();
}

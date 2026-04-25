package com.uni.uai.mcp.chatmemory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public class ChatMemoryUtil {
	private static ChatMemoryUtil instance = new ChatMemoryUtil();
	public static ChatMemoryUtil getInstance() {
		return instance;
	}
	
	public ChatMemory buildChatMemory(String sessionId, int maxMessage) {
		ChatMemory chatMemory = MessageWindowChatMemory.builder()
		        .maxMessages(maxMessage)
		        .id(sessionId)  //**可以为每个用户生成一个Chat内存
		        .chatMemoryStore(new DbChatMemoryStore(sessionId, maxMessage))
		        .build();
		return chatMemory;
	}

}

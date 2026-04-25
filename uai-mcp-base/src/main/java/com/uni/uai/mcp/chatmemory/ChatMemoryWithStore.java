package com.uni.uai.mcp.chatmemory;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

public class ChatMemoryWithStore {
	private String sessionId;
	private int maxMessage;
	
	private ChatMemory chatMemory;
	private DbChatMemoryStore store;
	
	public ChatMemoryWithStore(String sessionId, int maxMessage) {
		super();
		this.sessionId = sessionId;
		this.maxMessage = maxMessage;
		this.store = new DbChatMemoryStore(sessionId, maxMessage);
		this.chatMemory = MessageWindowChatMemory.builder()
		        .maxMessages(maxMessage)
		        .id(sessionId)  //**可以为每个用户生成一个Chat内存
		        .chatMemoryStore(store)
		        .build();
	}

	public void add(ChatMessage message) {
		this.chatMemory.add(message);
	}

    public List<ChatMessage> messages(){
    	return this.chatMemory.messages();
    }
    
    public void flushToDB() {
    	store.flushToDB();
    }

}

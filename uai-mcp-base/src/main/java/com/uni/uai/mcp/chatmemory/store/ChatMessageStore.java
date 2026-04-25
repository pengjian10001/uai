package com.uni.uai.mcp.chatmemory.store;

import java.util.List;

import com.uni.uai.mcp.model.ChatMessagePO;

public interface ChatMessageStore {
	
	List<ChatMessagePO> get(String key);
	
	void add(String key, ChatMessagePO value);
	
	void remove(String key);

}

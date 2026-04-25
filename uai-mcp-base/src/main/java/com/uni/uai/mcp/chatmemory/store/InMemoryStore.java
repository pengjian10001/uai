package com.uni.uai.mcp.chatmemory.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uni.uai.mcp.model.ChatMessagePO;

public class InMemoryStore implements ChatMessageStore {
	private Map<String,List<ChatMessagePO>> store = new HashMap<String,List<ChatMessagePO>>();

	@Override
	public List<ChatMessagePO> get(String key) {
		return store.get(key);
	}

	@Override
	public void add(String key, ChatMessagePO value) {
		List<ChatMessagePO> list = store.get(key);
		if(list == null) {
			list = new ArrayList<ChatMessagePO>();
			//查看
			store.put(key, list);
		}
		list.add(value);
	}

	@Override
	public void remove(String key) {
		store.remove(key);

	}

}

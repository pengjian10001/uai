package com.uni.uai.mcp.chatmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONReader;
//import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.json.JsonSanitizer;
import com.uni.uai.mcp.model.ChatMessagePO;
import com.uni.uai.mcp.chatmemory.store.*;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.data.DataSourceUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.*;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messageFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messageToJson;

public class DbChatMemoryStore implements ChatMemoryStore{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private List<ChatMessage> messages;
	private Object memoryId;
	private int maxMessage;
	private boolean alreadyFromDb = false;
	private List<ChatMessage> dbList;
	
	public DbChatMemoryStore(final Object memoryId, int parammaxMessage) {
        this.memoryId = memoryId;
        this.maxMessage = parammaxMessage * 3;  //将容量扩大3杯，是避免初始化获取指定条数的历史上下文后，之后，当前请求添加的chatmessage，会把历史的挤出去。
        this.messages = new BoundedLinkedList<ChatMessage>(maxMessage);
        this.dbList = new BoundedLinkedList<ChatMessage>(maxMessage);
    }

	public List<ChatMessage> getMessages() {
		return messages;
	}

	@Override
	public List<ChatMessage> getMessages(Object memoryId) {
		checkMemoryId(memoryId);
		if(alreadyFromDb) {
			return messages;
		}else {
			//将数据库中的内容，替换当前内容
			messages = new BoundedLinkedList<ChatMessage>(maxMessage);
			List<ChatMessagePO> listPO = DataSourceUtil.getInstance().getChatMessagesFromDB(memoryId.toString(), maxMessage);
			Long poId = null;
			String poSingleId = null;
			String poContent = null;
			for(ChatMessagePO po: listPO) {
				try {
					poId = po.getId();
					poSingleId = po.getSingleId();
					poContent = po.getContent();
					//为避免反序列化时，报错，处理json字符串中的特殊字符
					String content = poContent;//this.sanitizeJson2(poContent);
					ChatMessage chatMessage = messageFromJson(content);//JSON.parseObject(content, ChatMessage.class, JSONReader.Feature.AllowUnQuotedFieldNames);;
					dbList.add(chatMessage);
					messages.add(chatMessage);
				} catch (Exception e) {
					//将序列化失败的id设置为失效，避免下次获取
					DataSourceUtil.getInstance().updateChatMessagesState(poId);
					String msg = String.format("DbChatMemoryStore messageFromJson error. id=%s, singleId=%S", poId, poSingleId);
					logger.warn(msg);
					UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), msg , po.getContent(), e, false, 0L, ExceptionUtil.toStackTrace(e));
				}
			}
			alreadyFromDb = true;
			return messages;
		}
	}

	@Override
	public void updateMessages(Object memoryId, List<ChatMessage> messages) {
		checkMemoryId(memoryId);
        this.messages = messages;
	}
	
	@Override
	public void deleteMessages(Object memoryId) {
		checkMemoryId(memoryId);
        this.messages = new BoundedLinkedList<ChatMessage>(maxMessage);
	}
	
	/**
	 * 将数据刷入数据库
	 */
	public void flushToDB() {
		if(messages != null && messages.size() > 0) {
			List<ChatMessagePO> listPO = new ArrayList<ChatMessagePO>();
			for(int i = 0; i < messages.size(); i++) {
				ChatMessage chatMessage = messages.get(i);
				//ChatMessage都重写了equal和hashcode方法，如果是历史的，则不存
				//对于用户消息，一定保存
				if(dbList.contains(chatMessage) 
						//&& !chatMessage.getClass().getSimpleName().equals("UserMessage")
						) {
					//logger.info("----已存在 " + i);
					continue;
				}
				//logger.info("----不存在 " + i);
				String content = messageToJson(chatMessage);//JSON.toJSONString(chatMessage, JSONWriter.Feature.FieldBased,JSONWriter.Feature.IgnoreNonFieldGetter);
				if(content != null) {
					ChatMessagePO po = new ChatMessagePO();
					po.setSessionId(memoryId.toString());
					po.setType(chatMessage.getClass().getSimpleName());
					po.setContent(content);
					po.setSingleId(UbagConf.getlogId());
					listPO.add(po);
				}
			}
			Object result = DataSourceUtil.getInstance().insertChatMessageToDB(listPO);
			logger.info("DbChatMemoryStore flushToDB: " + result);
		}
	}
	
	private void checkMemoryId(Object memoryId) {
        if (!this.memoryId.equals(memoryId)) {
            throw new IllegalStateException("此chat memory的id是: " + this.memoryId +
                    " 但是请求的chat memory id为: " + memoryId);
        }
    }
	
	public String sanitizeJson2(String json) {
		if (json == null) return null;
        try {
            // 清洗并返回规范JSON，若无法修复会抛出异常
            return JsonSanitizer.sanitize(json);
        } catch (IllegalArgumentException e) {
            // 处理无法修复的情况
            return null;
        }
	}
	
	public String sanitizeJson(String json) {
	    if (json == null || json.trim().isEmpty()) {
	        return json;
	    }
	    
	    // 1. 移除前导和尾随的空白字符
	    String trimmed = json.trim();
	    
	    // 2. 确保JSON以 '{' 开头并以 '}' 结尾
	    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
	        int startIdx = trimmed.indexOf("{");
	        int endIdx = trimmed.lastIndexOf("}");
	        
	        if (startIdx >= 0 && endIdx > startIdx) {
	            trimmed = trimmed.substring(startIdx, endIdx + 1);
	        } else {
	            throw new IllegalArgumentException("无效的JSON格式: " + json);
	        }
	    }
	    
	    // 3. 修复常见的JSON格式问题
	    StringBuilder fixedJson = new StringBuilder(trimmed);
	    
	    // 处理未闭合的引号和多余逗号
	    boolean inQuotes = false;
	    boolean inEscape = false;
	    for (int i = 0; i < fixedJson.length(); i++) {
	        char c = fixedJson.charAt(i);
	        
	        // 处理转义字符
	        if (c == '\\') {
	            inEscape = !inEscape;
	        } else if (!inEscape) {
	            // 处理引号
	            if (c == '"') {
	                inQuotes = !inQuotes;
	            }
	            
	            // 处理对象内部或末尾的多余逗号
	            if (!inQuotes) {
	                // 检查是否是对象内部的多余逗号
	                if (c == ',' && i + 1 < fixedJson.length()) {
	                    char nextChar = fixedJson.charAt(i + 1);
	                    if (nextChar == '}' || nextChar == ']') {
	                        fixedJson.deleteCharAt(i);
	                        i--; // 调整索引，因为删除了一个字符
	                    }
	                }
	            }
	        } else {
	            inEscape = false; // 重置转义状态
	        }
	    }
	    
	    // 4. 转义未转义的换行符和制表符
	    String escapedJson = fixedJson.toString()
	        .replaceAll("(?<!\\\\)\n", "\\\\n")
	        .replaceAll("(?<!\\\\)\t", "\\\\t");
	    
	    // 5. 处理未转义的反斜杠
	    escapedJson = escapedJson.replaceAll("\\\\(?![btnfr\"\\\\/])", "\\\\\\\\");
	    
	    return escapedJson;
	}

	private int findPreviousObjectEnd(StringBuilder json, int fromIndex) {
	    for (int i = fromIndex; i >= 0; i--) {
	        if (json.charAt(i) == '}') {
	            return i;
	        }
	    }
	    return -1;
	}
	
	//一个有界的List，先进先出
		private static class BoundedLinkedList<E> extends LinkedList<E> {
		    private static final long serialVersionUID = 1L;
			private final int maxSize;

		    public BoundedLinkedList(int maxSize) {
		        this.maxSize = maxSize;
		    }

		    @Override
		    public boolean add(E element) {
		        if (size() >= maxSize) {
		            removeFirst();  // 移除最早的元素
		        }
		        return super.add(element);
		    }

			@Override
			public boolean addAll(Collection<? extends E> c) {
				if(c == null || c.size() == 0) {
					return false;
				}
				for(E element: c) {
					this.add(element);
				}
				return true;
			}
		    
		    
		}
	
	public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
		/**ToolExecutionResultMessage m = new ToolExecutionResultMessage("1", "2", "3");
		String content = JSONUtil.toJSONString(m);
		logger.info(content);
		ToolExecutionResultMessage m2 = JSONUtil.parseObject(content, ToolExecutionResultMessage.class);
		logger.info(m2);
		**/
		
		
		String content = """
				{"toolExecutionRequests":[{"id":"call_uo65g2KUJx6KF5BO2WU2NDvi","name":"_mcpServerInfo","arguments":{"queryText":"有什么工具"}}],"type":"AI"}
				""";
		//content = escapeJson(content);
		content = new DbChatMemoryStore(1,10).sanitizeJson2(content);
		System.out.println(content);
		ChatMessage chatMessage = messageFromJson(content);//JSON.parseObject(content, ChatMessage.class, JSONReader.Feature.AllowUnQuotedFieldNames);
		System.out.println(chatMessage);
	}
	

}

package dev.langchain4j.data.message;

/**
 * Represents a chat message.
 * Used together with {@link ChatModel} and {@link StreamingChatModel}.
 *
 * @see SystemMessage
 * @see UserMessage
 * @see AiMessage
 * @see ToolExecutionResultMessage
 * @see CustomMessage
 */
public interface ChatMessage {
    /**
     * The type of the message.
     *
     * @return the type of the message
     */
    ChatMessageType type();
    //加上这个方法，以解决在fastjson2反序列化从db中获取到的chatmemory时，获取type。否则会报错
    public default ChatMessageType getType() {
    	return type();
    }

}

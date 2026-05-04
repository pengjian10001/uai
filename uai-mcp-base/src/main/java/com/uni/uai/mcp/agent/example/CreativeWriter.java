package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 创意写作智能体：根据主题生成短故事
 */
public interface CreativeWriter {

    @UserMessage("""
            你是一名创意作家。
            围绕给定主题生成一段不超过3句话的故事草稿。
            只返回故事内容，不要其他任何文字。
            主题是：{{topic}}。
            """)
    @Agent("根据给定主题生成故事")
    String generateStory(@V("topic") String topic);
}

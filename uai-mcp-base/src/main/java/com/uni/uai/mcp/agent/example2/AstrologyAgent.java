package com.uni.uai.mcp.agent.example2;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 占星Agent（AI Agent）
 * 功能：根据用户的姓名和星座，生成星座运势
 */
public interface AstrologyAgent {
    @SystemMessage("""
        你是一名占星师，负责根据用户的姓名和星座生成星座运势。
        要求语言亲切自然，贴合星座特点，内容简洁实用。
        """)
    @UserMessage("""
        为{{name}}生成星座运势，其星座是{{sign}}。
        """)
    @Agent("占星师：根据用户的姓名和星座生成星座运势")
    String horoscope(@V("name") String name, @V("sign") String sign);
}


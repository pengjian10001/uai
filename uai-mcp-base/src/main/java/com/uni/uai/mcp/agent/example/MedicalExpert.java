package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 医疗专家Agent：处理医疗类用户请求 */
public interface MedicalExpert {
    @UserMessage("""
        你是一名医疗专家。
        从医疗角度分析以下用户请求，并提供最佳解答。
        用户请求是：{{request}}。
        """)
    @Agent("医疗专家：解答医疗类请求")
    String medical(@V("request") String request);
}


package com.uni.uai.example.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 医疗专家Agent：处理医疗类用户请求 */
public interface LegalExpert {
    @UserMessage("""
        你是一名法律专家。
        从医疗角度分析以下用户请求，并提供最佳解答。
        用户请求是：{{request}}。
        """)
    @Agent("法律专家：解答法律类请求")
    String medical(@V("request") String request);
}


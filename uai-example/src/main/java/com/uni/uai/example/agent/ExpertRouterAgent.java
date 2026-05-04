package com.uni.uai.example.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**专家路由Agent：根据请求分类，调用对应的专家Agent */
public interface ExpertRouterAgent {
    @Agent
    String ask(@V("request") String request);
}


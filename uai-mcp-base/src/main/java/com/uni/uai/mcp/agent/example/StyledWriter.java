package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**风格化写作Agent：生成故事并通过循环优化风格 */
public interface StyledWriter {
    @Agent
    String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}


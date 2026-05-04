package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 受众编辑Agent：将故事编辑为更适合特定受众的版本 */
public interface AudienceEditor {
    @UserMessage("""
        你是一名专业编辑。
        分析并改写以下故事，使其更贴合目标受众{{audience}}。
        只返回改写后的故事，不要其他任何文字。
        故事内容：{{story}}。
        """)
    @Agent("将故事编辑为更适合特定受众的版本")
    String editStory(@V("story") String story, @V("audience") String audience);
}

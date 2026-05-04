package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**风格编辑Agent：将故事编辑为更贴合特定风格的版本 */
public interface StyleEditor {
    @UserMessage("""
        你是一名专业编辑。
        分析并改写以下故事，使其更贴合、更符合{{style}}风格。
        只返回改写后的故事，不要其他任何文字。
        故事内容：{{story}}。
        """)
    @Agent("将故事编辑为更贴合特定风格的版本")
    String editStory(@V("story") String story, @V("style") String style);
}


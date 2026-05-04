package com.uni.uai.example.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 风格评分Agent：根据故事与指定风格的契合度给出评分 */
public interface StyleScorer {
    @UserMessage("""
            你是一名专业评论员。
            根据以下故事与{{style}}风格的契合度，给出0.0到1.0之间的评分。
            只返回评分数字，不要其他任何文字。
            
            故事内容：{{story}}
            """)
    @Agent("根据指定风格为故事打分")
    double scoreStyle(@V("story") String story, @V("style") String style);
}


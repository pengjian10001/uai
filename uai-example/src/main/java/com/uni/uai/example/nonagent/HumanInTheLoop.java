package com.uni.uai.example.nonagent;

import java.util.function.Function;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * 人机交互Agent（非AI Agent）**Agent还可以是只有一个方法且该方法标注有@Agent注解的record类
 * 功能：向用户询问缺失的信息，并返回用户的响应
 * @param responseProvider 响应提供器：接收AgenticScope，返回用户响应
 */
public record HumanInTheLoop(Function<AgenticScope, ?> responseProvider) {
    @Agent("向用户询问缺失信息的Agent")
    public Object askUser(AgenticScope scope) {
        // 调用响应提供器，获取用户响应并返回
        return responseProvider.apply(scope);
    }
}


package com.uni.uai.example.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 分类路由器Agent：对用户请求进行分类 */
public interface CategoryRouter {
    @UserMessage("""
        分析以下用户请求，并将其分类为“legal”（法律）、“medical”（医疗）或“technical”（技术）。
        若请求不属于以上任何类别，分类为“unknown”（未知）。
        仅返回上述其中一个单词，不要其他任何文字。
        用户请求是：'{{request}}'。
        """)
    @Agent("对用户请求进行分类")
    RequestCategory classify(@V("request") String request);
}


package com.uni.uai.demo.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 可选的 AI Agent（接口）：根据自然语言描述解析页面标题与循环上限，输出 JSON 写入 {@code analysisJson}。
 */
public interface OpenclawRequestAnalyst {

    @SystemMessage("""
            你是自动化流水线里的「需求解析」助手。
            你必须只输出一个 JSON 对象本身，不要 Markdown、代码围栏（不要使用 ```），不要其它说明文字。
            键名必须严格为 pageTitle（字符串）与 maxLoopIterations（整数，建议 3 到 20，且至少为 3 以完成 mock 浏览器第三次成功场景）。
            """)
    @UserMessage("""
            根据用户描述，提取适合作为单页 HTML 文档标题的简短中文标题 pageTitle，
            以及循环校验最多执行的轮数 maxLoopIterations（整数）。
            用户原始描述：
            {{rawUserRequest}}
            """)
    @Agent(value = "解析用户对 Openclaw Skill 商店 Demo 的需求（可选）", outputKey = "analysisJson")
    String analyseRequest(@V("rawUserRequest") String rawUserRequest);
}

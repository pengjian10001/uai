package com.uni.uai.demo.openclaw;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 分析 Skill 对应的后端逻辑：OpenClaw 只打到本入口；趋势/摘要/对比等细分在此路由。
 */
public final class OpenclawAnalysisSkillDemo {

    private final ChatModel chatModel;
    private final boolean useLlm;

    public OpenclawAnalysisSkillDemo(ChatModel chatModel, boolean useLlm) {
        this.chatModel = chatModel;
        this.useLlm = useLlm;
    }

    public Map<String, Object> handle(String query) {
        AnalysisSubIntent intent = resolveIntent(query);
        String detail = switch (intent) {
            case TREND -> "【趋势】Demo：时间序列与同比环比占位；可接数仓或 BI。";
            case SUMMARY -> "【摘要】Demo：长文本压缩与要点列表占位；可接 RAG。";
            case COMPARISON -> "【对比】Demo：多版本/多实验组差异占位；可接指标服务。";
            case UNKNOWN -> "【分析-未分类】Demo：请补充分析词（趋势/摘要/对比）或开启 LLM 分类。";
        };
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("skill", "analysis");
        body.put("subIntent", intent.name());
        body.put("routing", useLlm ? "langchain4j-llm" : "langchain4j-heuristic");
        body.put("message", detail);
        body.put("echoQuery", query);
        return body;
    }

    private AnalysisSubIntent resolveIntent(String query) {
        if (!useLlm || chatModel == null) {
            return OpenclawSkillIntentFallback.analysis(query);
        }
        try {
            AnalysisIntentClassifier classifier = AiServices.builder(AnalysisIntentClassifier.class)
                .chatModel(chatModel)
                .build();
            AnalysisSubIntent r = classifier.classify(query);
            return r != null ? r : AnalysisSubIntent.UNKNOWN;
        } catch (Exception e) {
            return OpenclawSkillIntentFallback.analysis(query);
        }
    }
}

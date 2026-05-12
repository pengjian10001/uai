package com.uni.uai.demo.openclaw;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 财务 Skill 对应的后端逻辑：OpenClaw 只打到本入口；报销/对账/报表等细分在此路由。
 */
public final class OpenclawFinanceSkillDemo {

    private final ChatModel chatModel;
    private final boolean useLlm;

    public OpenclawFinanceSkillDemo(ChatModel chatModel, boolean useLlm) {
        this.chatModel = chatModel;
        this.useLlm = useLlm;
    }

    public Map<String, Object> handle(String query) {
        FinanceSubIntent intent = resolveIntent(query);
        String detail = switch (intent) {
            case REIMBURSEMENT -> "【报销】Demo：校验发票抬头与税号占位；可接真实费控 API。";
            case RECONCILIATION -> "【对账】Demo：匹配银行流水与总账分录占位；可接银企或 ERP。";
            case REPORT -> "【报表】Demo：生成科目余额/合并抵消规则占位；可接报表引擎。";
            case UNKNOWN -> "【财务-未分类】Demo：请补充业务词（报销/对账/报表）或开启 LLM 分类。";
        };
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("skill", "finance");
        body.put("subIntent", intent.name());
        body.put("routing", useLlm ? "langchain4j-llm" : "langchain4j-heuristic");
        body.put("message", detail);
        body.put("echoQuery", query);
        return body;
    }

    private FinanceSubIntent resolveIntent(String query) {
        if (!useLlm || chatModel == null) {
            return OpenclawSkillIntentFallback.finance(query);
        }
        try {
            FinanceIntentClassifier classifier = AiServices.builder(FinanceIntentClassifier.class)
                .chatModel(chatModel)
                .build();
            FinanceSubIntent r = classifier.classify(query);
            return r != null ? r : FinanceSubIntent.UNKNOWN;
        } catch (Exception e) {
            return OpenclawSkillIntentFallback.finance(query);
        }
    }
}

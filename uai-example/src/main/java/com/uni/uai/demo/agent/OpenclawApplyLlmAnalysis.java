package com.uni.uai.demo.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * 非 AI Agent：把 {@code analysisJson} 应用到共享状态：覆盖 {@code request}（网页标题），并写入 {@code maxLoopIterations}。
 * 当前置可选 Analyst 未执行时，{@code analysisJson} 为空，本步骤保留入参中的默认值。
 */
public class OpenclawApplyLlmAnalysis {

    private static final Pattern TITLE_PAT = Pattern.compile(
            "\"pageTitle\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MAX_PAT = Pattern.compile(
            "\"maxLoopIterations\"\\s*:\\s*(\\d+)");

    @Agent(
            value = "将 LLM 解析出的 JSON 写入 request 与 maxLoopIterations",
            outputKey = "request"
    )
    public String apply(AgenticScope scope) {
        String analysisJson = String.valueOf(scope.readState("analysisJson", ""));
        String fallbackRequest = String.valueOf(scope.readState("request", ""));
        System.out.println("[OpenclawApplyLlmAnalysis] 开始合并解析结果，analysisJson 是否为空="
                + (analysisJson == null || analysisJson.isBlank()));
        if (analysisJson == null || analysisJson.isBlank()) {
            System.out.println("[OpenclawApplyLlmAnalysis] 无 LLM 解析结果，保持 request 与 maxLoopIterations 入参默认");
            return fallbackRequest == null ? "" : fallbackRequest;
        }
        String title = extractTitle(analysisJson, fallbackRequest);
        int max = extractMaxIterations(analysisJson, readMax(scope));
        max = Math.max(3, Math.min(50, max));
        scope.writeState("maxLoopIterations", max);
        System.out.println("[OpenclawApplyLlmAnalysis] 已应用：pageTitle=" + title + ", maxLoopIterations=" + max);
        return title;
    }

    private static int readMax(AgenticScope scope) {
        Object v = scope.readState("maxLoopIterations");
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 10;
    }

    private static String extractTitle(String json, String fallback) {
        Matcher m = TITLE_PAT.matcher(json);
        if (m.find()) {
            return unescapeJsonString(m.group(1));
        }
        return fallback == null ? "" : fallback;
    }

    private static int extractMaxIterations(String json, int fallback) {
        Matcher m = MAX_PAT.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String unescapeJsonString(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}

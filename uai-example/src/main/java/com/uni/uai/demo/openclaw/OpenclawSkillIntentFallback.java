package com.uni.uai.demo.openclaw;

/**
 * 不调用模型时的确定性路由，便于本地反复调试 OpenClaw 与 HTTP 契约而无需稳定 LLM。
 */
final class OpenclawSkillIntentFallback {

    private OpenclawSkillIntentFallback() {}

    static FinanceSubIntent finance(String query) {
        if (query == null || query.isBlank()) {
            return FinanceSubIntent.UNKNOWN;
        }
        String s = query.toLowerCase();
        if (containsAny(query, s, "报销", "发票", "费用", "差旅", "reimburse")) {
            return FinanceSubIntent.REIMBURSEMENT;
        }
        if (containsAny(query, s, "对账", "银行", "流水", "调节", "差异", "reconcil")) {
            return FinanceSubIntent.RECONCILIATION;
        }
        if (containsAny(query, s, "报表", "合并", "披露", "科目", "report")) {
            return FinanceSubIntent.REPORT;
        }
        return FinanceSubIntent.UNKNOWN;
    }

    static AnalysisSubIntent analysis(String query) {
        if (query == null || query.isBlank()) {
            return AnalysisSubIntent.UNKNOWN;
        }
        String s = query.toLowerCase();
        if (containsAny(query, s, "趋势", "同比", "环比", "时间序列", "trend", "yoy", "mom")) {
            return AnalysisSubIntent.TREND;
        }
        if (containsAny(query, s, "摘要", "总结", "要点", "summary", "提炼")) {
            return AnalysisSubIntent.SUMMARY;
        }
        if (containsAny(query, s, "对比", "差异", "比较", "compare", "versus")) {
            return AnalysisSubIntent.COMPARISON;
        }
        if (s.contains(" vs ") || s.contains(" vs.")) {
            return AnalysisSubIntent.COMPARISON;
        }
        return AnalysisSubIntent.UNKNOWN;
    }

    private static boolean containsAny(String raw, String lower, String... needles) {
        for (String n : needles) {
            if (n == null || n.isEmpty()) {
                continue;
            }
            if (raw.contains(n)) {
                return true;
            }
            if (isAllAscii(n) && lower.contains(n.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllAscii(String n) {
        return n.chars().allMatch(c -> c < 128);
    }
}

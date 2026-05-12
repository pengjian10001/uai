package com.uni.uai.demo.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * 非 AI Agent：根据浏览器检查结果（key=check）调整 HTML 代码（key=code）。
 */
public class OpenclawCodeAdjuster {

    @Agent(
            value = "根据浏览器检查结果调整 HTML 代码",
            outputKey = "code"
    )
    public String adjustCode(AgenticScope scope) {
        String code = String.valueOf(scope.readState("code", ""));
        String check = String.valueOf(scope.readState("check", ""));
        int attempt = OpenclawBrowserMockAgent.currentInvocationCount();
        if (code == null || code.isBlank()) {
            System.out.println("[OpenclawCodeAdjuster] key=code 为空，跳过调整");
            return "";
        }
        if (check == null || check.isBlank() || "success".equalsIgnoreCase(check) || "user_declined".equalsIgnoreCase(check)) {
            System.out.println("[OpenclawCodeAdjuster] 当前 check=" + check + "，无需调整代码");
            return code;
        }

        // mock 调整：把错误信息和轮次写入 HTML 注释，模拟修复过程可追溯。
        String marker = "<!-- auto-adjust attempt " + attempt + ": " + sanitizeForComment(check) + " -->";
        String adjusted;
        if (code.contains("</body>")) {
            adjusted = code.replace("</body>", "  " + marker + "\n</body>");
        } else {
            adjusted = code + "\n" + marker + "\n";
        }
        System.out.println("[OpenclawCodeAdjuster] 已根据 check 调整代码，attempt=" + attempt);
        return adjusted;
    }

    private static String sanitizeForComment(String text) {
        return text.replace("--", "- -");
    }
}


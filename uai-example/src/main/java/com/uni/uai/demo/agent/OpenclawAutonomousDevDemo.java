package com.uni.uai.demo.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.uni.uai.example.llm.ChatModelFactory;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.observability.MonitoredExecution;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;

/**
 * AI 自主开发系统 Demo：以 Non-AI Agent + HumanInTheLoop + 循环 + 监控为主；
 * 可在入口用 {@code analyserequest=true} 触发可选的 LLM 需求解析，为标题与循环轮数提供默认值。
 */
public class OpenclawAutonomousDevDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("======== OpenclawAutonomousDevDemo 启动 ========");
        OpenclawBrowserMockAgent.resetInvocationCounterForDemo();

        ArgBundle argBundle = parseArgs(args);
        System.out.println("[OpenclawAutonomousDevDemo] analyserequest=" + argBundle.analyseRequest
                + ", targetDir=" + argBundle.targetDir
                + ", 用户描述/标题=" + argBundle.userText);

        AgentMonitor monitor = new AgentMonitor();
        ChatModel baseModel = ChatModelFactory.getInstance().getDefaultChatModel();

        OpenclawRequestAnalyst requestAnalyst = AgenticServices.agentBuilder(OpenclawRequestAnalyst.class)
                .name("openclawRequestAnalyst")
                .chatModel(baseModel)
                .optional(true)
                .outputKey("analysisJson")
                .listener(consoleSteps())
                .build();

        HumanInTheLoop allowHuman = AgenticServices.humanInTheLoopBuilder()
                .description("征得工程师同意：是否将生成的 HTML 写入指定目录")
                .outputKey("allow")
                .listener(consoleSteps())
                .responseProvider(scope -> promptAllowIfNeeded(scope))
                .build();

        UntypedAgent writeAndVerifyLoop = AgenticServices.loopBuilder()
                .name("openclawWriteVerifyLoop")
                .listener(consoleSteps())
                .testExitAtLoopEnd(true)
                .subAgents(
                        new OpenclawBrowserMockAgent(),
                        new OpenclawCodeAdjuster(),
                        new OpenclawStoreFileWriter())
                .maxIterations(100)
                .exitCondition((scope, loopCounter) -> {
                    String c = scope.readState("check", "");
                    if ("success".equalsIgnoreCase(c) || "user_declined".equalsIgnoreCase(c)) {
                        System.out.println("[OpenclawWriteVerifyLoop] 退出条件：check=" + c);
                        return true;
                    }
                    int cap = ((Number) scope.readState("maxLoopIterations", 10)).intValue();
                    if (loopCounter >= cap) {
                        System.out.println("[OpenclawWriteVerifyLoop] 退出条件：已达 maxLoopIterations=" + cap
                                + " (loopCounter=" + loopCounter + ")");
                        return true;
                    }
                    System.out.println("[OpenclawWriteVerifyLoop] 继续循环 check=" + c
                            + ", loopCounter=" + loopCounter + "/" + cap);
                    return false;
                })
                .build();

        UntypedAgent pipeline = AgenticServices.sequenceBuilder()
                .name("openclawAutonomousDevPipeline")
                .listener(monitor)
                .listener(consoleSteps())
                .subAgents(
                        requestAnalyst,
                        new OpenclawApplyLlmAnalysis(),
                        new OpenclawStoreHtmlGenerator(),
                        allowHuman,
                        writeAndVerifyLoop)
                .outputKey("check")
                .build();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("targetDir", argBundle.targetDir);
        input.put("request", argBundle.userText);
        input.put("maxLoopIterations", 10);
        if (argBundle.analyseRequest) {
            input.put("rawUserRequest", argBundle.userText);
            System.out.println("[OpenclawAutonomousDevDemo] 已注入 rawUserRequest，可选 Analyst 将调用 LLM");
        } else {
            System.out.println("[OpenclawAutonomousDevDemo] 未注入 rawUserRequest，可选 Analyst 跳过（参照 TestOptional）");
        }

        Object result = pipeline.invoke(input);
        System.out.println("======== 流水线最终结果 (key=check) ======== " + result);

        if (!monitor.successfulExecutions().isEmpty()) {
            MonitoredExecution execution = monitor.successfulExecutions().get(0);
            System.out.println("-------- AgentMonitor 执行摘要 --------");
            System.out.println(execution);
            Path report = Path.of("openclaw-autodev-report.html");
            HtmlReportGenerator.generateReport(monitor, report);
            System.out.println("[OpenclawAutonomousDevDemo] HTML 报告已生成: " + report.toAbsolutePath());
        } else {
            System.out.println("[OpenclawAutonomousDevDemo] 无 successfulExecutions 记录，跳过 HTML 报告");
        }
        System.out.println("======== OpenclawAutonomousDevDemo 结束 ========");
    }

    private record ArgBundle(boolean analyseRequest, String targetDir, String userText) {}

    /**
     * 若首参数为 true/false，则按新约定解析；否则与旧版兼容：首参数为 targetDir，其余为标题。
     */
    private static ArgBundle parseArgs(String[] args) {
        String defDir = "target/openclaw-demo-store";
        String defTitle = "请为 Openclaw Skill 线上商店生成极客风格首页标题，并建议最多循环校验 6 轮";
        if (args == null || args.length == 0) {
            return new ArgBundle(true, defDir, defTitle);
        }
        if ("true".equalsIgnoreCase(args[0]) || "false".equalsIgnoreCase(args[0])) {
            boolean ar = Boolean.parseBoolean(args[0]);
            String dir = args.length > 1 && !args[1].isBlank() ? args[1] : defDir;
            String text = args.length > 2
                    ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                    : defTitle;
            return new ArgBundle(ar, dir, text);
        }
        String dir = args[0];
        String text = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : defTitle;
        return new ArgBundle(true, dir, text);
    }

    private static AgentListener consoleSteps() {
        return new AgentListener() {
            @Override
            public void beforeAgentInvocation(AgentRequest request) {
                System.out.println("[AgentStep] -> " + request.agentName() + " 开始, inputs=" + summarizeInputs(request.inputs()));
            }

            @Override
            public void afterAgentInvocation(AgentResponse response) {
                System.out.println("[AgentStep] <- " + response.agentName() + " 结束, output=" + summarize(response.output()));
            }
        };
    }

    private static String summarize(Object output) {
        if (output == null) {
            return "null";
        }
        String s = String.valueOf(output);
        if (s.length() > 200) {
            return s.substring(0, 200) + "...(" + s.length() + " chars)";
        }
        return s;
    }

    private static Map<String, Object> summarizeInputs(Map<String, ?> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : inputs.entrySet()) {
            out.put(e.getKey(), summarize(e.getValue()));
        }
        return out;
    }

    private static Object promptAllowIfNeeded(dev.langchain4j.agentic.scope.AgenticScope scope) {
        Object existing = scope.readState("allow");
        if (existing != null && !String.valueOf(existing).isBlank()) {
            System.out.println("[AllowHuman] allow 已存在，跳过询问: " + existing);
            return existing;
        }
        String dir = String.valueOf(scope.readState("targetDir", "target/openclaw-demo-store"));
        System.out.println("[AllowHuman] 需要工程师确认：是否将页面写入目录?");
        System.out.println("[AllowHuman] 目标目录: " + Path.of(dir).toAbsolutePath());
        System.out.print("[AllowHuman] 请输入 yes 或 no: ");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine();
            System.out.println("[AllowHuman] 收到答复: " + line);
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            throw new RuntimeException("读取用户输入失败", e);
        }
    }
}

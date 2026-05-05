package com.uni.uai.demo.agent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI Agent：mock「浏览器加载」——读取已写入的 HTML 文件；第 1、2 次调用返回错误，第 3 次返回 success。
 */
public class OpenclawBrowserMockAgent {

    private static final AtomicInteger BROWSER_INVOCATIONS = new AtomicInteger(0);

    static void resetInvocationCounterForDemo() {
        int was = BROWSER_INVOCATIONS.getAndSet(0);
        System.out.println("[OpenclawBrowserMockAgent] 重置浏览器 mock 调用计数 (原值=" + was + ")");
    }

    static int currentInvocationCount() {
        return BROWSER_INVOCATIONS.get();
    }

    @Agent(
            value = "Mock 浏览器：加载本地 HTML 并校验（前两次失败，第三次成功）",
            outputKey = "check"
    )
    public String mockBrowserLoad(@V("allow") String allow, @V("file") String filePath) {
        System.out.println("[OpenclawBrowserMockAgent] 开始 mock 浏览器校验，allow=" + allow + ", file=" + filePath);
        if (!isYes(allow)) {
            System.out.println("[OpenclawBrowserMockAgent] 用户未同意写入，设置 check=user_declined 并结束");
            return "user_declined";
        }
        if (filePath == null || filePath.isBlank()) {
            System.out.println("[OpenclawBrowserMockAgent] 无有效文件路径，返回错误");
            return "error: file path empty";
        }
        int n = BROWSER_INVOCATIONS.incrementAndGet();
        System.out.println("[OpenclawBrowserMockAgent] 第 " + n + " 次 mock 调用（读取文件校验）");
        try {
            String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            int len = content.length();
            System.out.println("[OpenclawBrowserMockAgent] 已读取文件，长度=" + len);
            if (n < 3) {
                String err = "error: mock browser warmup failed (attempt " + n + ", need 3 for success)";
                System.out.println("[OpenclawBrowserMockAgent] " + err);
                return err;
            }
            System.out.println("[OpenclawBrowserMockAgent] 第三次调用，mock 校验通过 -> success");
            return "success";
        } catch (Exception e) {
            String err = "error: read failed — " + e.getMessage();
            System.out.println("[OpenclawBrowserMockAgent] " + err);
            return err;
        }
    }

    private static boolean isYes(String allow) {
        if (allow == null) {
            return false;
        }
        String t = allow.trim();
        return t.equalsIgnoreCase("yes") || t.equalsIgnoreCase("y");
    }
}

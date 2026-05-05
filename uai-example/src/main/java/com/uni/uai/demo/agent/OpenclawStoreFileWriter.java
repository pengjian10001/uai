package com.uni.uai.demo.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 非 AI Agent：从 key=code 读取 HTML，依据 key=allow 决定是否写入磁盘；同意时写入并设置 key=file。
 */
public class OpenclawStoreFileWriter {

    @Agent(
            value = "将生成的商店 HTML 写入本地目录（需 allow=yes）",
            outputKey = "file"
    )
    public String writeHtmlToDisk(
            @V("allow") String allow,
            @V("code") String code,
            @V("targetDir") String targetDir
    ) throws IOException {
        System.out.println("[OpenclawStoreFileWriter] 评估写入条件，allow=" + allow);
        String decision = allow == null ? "" : allow.trim();
        if (decision.isEmpty()) {
            System.out.println("[OpenclawStoreFileWriter] allow 为空，跳过写入");
            return "";
        }
        if (!decision.equalsIgnoreCase("yes") && !decision.equalsIgnoreCase("y")) {
            System.out.println("[OpenclawStoreFileWriter] 用户未同意写入 (allow=" + decision + ")，退出写入流程");
            return "";
        }
        if (code == null || code.isBlank()) {
            System.out.println("[OpenclawStoreFileWriter] key=code 为空，无法写入");
            return "";
        }
        String dir = targetDir == null || targetDir.isBlank() ? "target/openclaw-demo-store" : targetDir.trim();
        Path outDir = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("openclaw-skill-store-generated.html");
        System.out.println("[OpenclawStoreFileWriter] 写入文件: " + outFile);
        Files.writeString(outFile, code, StandardCharsets.UTF_8);
        String pathStr = outFile.toString();
        System.out.println("[OpenclawStoreFileWriter] 写入成功，key=file -> " + pathStr);
        return pathStr;
    }
}

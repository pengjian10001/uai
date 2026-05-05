package com.uni.uai.demo.agent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Openclaw Skill 线上商店单页 Demo：将 classpath 中的静态 HTML 复制到 {@code target/} 并打印本地访问方式。
 * <p>不依赖大模型；用于与 {@link OpenclawAutonomousDevDemo} 配套的「可部署」静态页展示。</p>
 */
public class OpenclawSkillStoreHtmlDemo {

    private static final String RESOURCE = "/demo/openclaw-skill-store-demo.html";

    public static void main(String[] args) throws Exception {
        System.out.println("======== OpenclawSkillStoreHtmlDemo 启动 ========");
        Path outDir = Path.of(args.length > 0 ? args[0] : "target");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("openclaw-skill-store-demo.html");
        try (InputStream in = OpenclawSkillStoreHtmlDemo.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("找不到资源: " + RESOURCE);
            }
            Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
        }
        Path abs = outFile.toAbsolutePath().normalize();
        System.out.println("[OpenclawSkillStoreHtmlDemo] 已导出静态页: " + abs);
        System.out.println("[OpenclawSkillStoreHtmlDemo] 可用浏览器打开 file://" + abs);
        System.out.println("[OpenclawSkillStoreHtmlDemo] 或部署到任意静态托管（Nginx、OSS、GitHub Pages 等）");
        System.out.println("======== OpenclawSkillStoreHtmlDemo 结束 ========");
    }
}

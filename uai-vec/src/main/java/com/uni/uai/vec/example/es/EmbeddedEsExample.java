package com.uni.uai.vec.example.es;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmbeddedEsExample {

    public static void main(String[] args) throws IOException, NodeValidationException, InterruptedException {
        // 1. 设置工作目录（建议指向一个空文件夹，ES 会自动创建 data 和 config 子目录）
        // 注意：路径中不要包含中文或空格
        Path esHomeDir = Paths.get("target/embedded-es-data"); 

        // 2. 构建 Settings
        Settings settings = Settings.builder()
                .put("node.name", "embedded-node-fix")
                .put("path.home", esHomeDir.toAbsolutePath().toString())
                // 显式指定数据路径和日志路径（可选，不指定会自动在 path.home 下生成）
                .put("path.data", esHomeDir.resolve("data").toAbsolutePath().toString())
                .put("path.logs", esHomeDir.resolve("logs").toAbsolutePath().toString())
                
                // 【关键配置】禁用安全模块（X-Pack），避免复杂的 SSL 证书配置和权限检查
                // 在嵌入式开发中，通常不需要安全认证
                .put("xpack.security.enabled", "false")
                
                // 绑定本地回环地址
                .put("network.host", "127.0.0.1")
                .put("http.port", 9200)
                .build();

        // 3. 创建 Environment
        // 注意：这里传入的 configDir 可以为 null，或者指向一个包含 elasticsearch.yml 的目录
        // 如果为 null，ES 会使用默认设置
        Environment environment = new Environment(settings, null);

        System.out.println("正在启动嵌入式 Elasticsearch 节点...");
        
        // 4. 启动节点
        Node node = new Node(environment).start();

        System.out.println("✅ Elasticsearch 节点已启动！");
        
        // 保持主线程存活，否则程序会退出导致 ES 停止
        // 实际应用中可以添加 shutdown hook
        Thread.sleep(Long.MAX_VALUE);
        
        // 4. 现在你可以使用 'client' 对象执行各种操作了
        // ... 你的业务代码 ...

        // 5. 应用关闭时，记得关闭客户端和节点
        node.close();

    }
}


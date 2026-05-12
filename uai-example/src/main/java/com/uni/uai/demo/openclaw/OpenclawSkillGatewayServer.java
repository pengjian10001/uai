package com.uni.uai.demo.openclaw;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.model.chat.ChatModel;

import com.uni.uai.example.llm.ChatModelFactory;

/**
 * 本地 HTTP 网关：供 OpenClaw 将「财务 / 分析」两个 Skill 分别映射到两个 URL，避免在 OpenClaw 侧配置大量细分工具；
 * 细分意图在 {@link OpenclawFinanceSkillDemo}、{@link OpenclawAnalysisSkillDemo} 中用 LangChain4j 路由。
 * <p>
 * 设计对照 {@code com.uni.uai.mcp.skill.example}：OpenClaw 侧像配置「工具 / Skill 入口」一样配置 HTTP；
 * Java 侧迭代路由逻辑时只需重启本进程（或热部署到 Spring），无需反复改 OpenClaw 全局配置。
 */
public final class OpenclawSkillGatewayServer {

    public static final String PATH_FINANCE = "/api/openclaw/finance";
    public static final String PATH_ANALYSIS = "/api/openclaw/analysis";

    private static final ObjectMapper JSON = new ObjectMapper();

    private OpenclawSkillGatewayServer() {}

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        boolean useLlm = resolveUseLlm();
        HttpServer server = startGateway(new InetSocketAddress("127.0.0.1", port), useLlm);
        System.out.println("[OpenclawSkillGateway] listening on http://127.0.0.1:" + port);
        System.out.println("[OpenclawSkillGateway] POST " + PATH_FINANCE + " | POST " + PATH_ANALYSIS);
        System.out.println("[OpenclawSkillGateway] useLlm=" + useLlm + " (set UAI_OPENCLAW_SKILL_USE_LLM=false for offline heuristic)");
        CountDownLatch hang = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            hang.countDown();
        }));
        try {
            hang.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.stop(0);
        }
    }

    /**
     * 启动网关（单 JVM 内可多次用于测试；生产入口一般用 {@link #main}）。
     */
    public static HttpServer startGateway(InetSocketAddress bind, boolean useLlm) throws IOException {
        ChatModel model = ChatModelFactory.getInstance().getDefaultChatModel();
        OpenclawFinanceSkillDemo finance = new OpenclawFinanceSkillDemo(model, useLlm);
        OpenclawAnalysisSkillDemo analysis = new OpenclawAnalysisSkillDemo(model, useLlm);

        HttpServer server = HttpServer.create(bind, 0);
        server.createContext(PATH_FINANCE, new SkillHttpHandler(finance::handle));
        server.createContext(PATH_ANALYSIS, new SkillHttpHandler(analysis::handle));
        server.createContext("/health", ex -> {
            byte[] ok = "{\"status\":\"up\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(200, ok.length);
            ex.getResponseBody().write(ok);
            ex.close();
        });
        server.setExecutor(null);
        server.start();
        return server;
    }

    static int resolvePort(String[] args) {
        if (args != null && args.length >= 1 && !args[0].isBlank()) {
            return Integer.parseInt(args[0].trim());
        }
        String env = System.getenv("UAI_OPENCLAW_GATEWAY_PORT");
        if (env != null && !env.isBlank()) {
            return Integer.parseInt(env.trim());
        }
        String prop = System.getProperty("uai.openclaw.gateway.port");
        if (prop != null && !prop.isBlank()) {
            return Integer.parseInt(prop.trim());
        }
        return 19090;
    }

    static boolean resolveUseLlm() {
        String env = System.getenv("UAI_OPENCLAW_SKILL_USE_LLM");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        String prop = System.getProperty("uai.openclaw.skill.use.llm");
        if (prop != null && !prop.isBlank()) {
            return Boolean.parseBoolean(prop.trim());
        }
        return true;
    }

    @FunctionalInterface
    interface SkillInvoker {
        Map<String, Object> invoke(String query);
    }

    static final class SkillHttpHandler implements HttpHandler {

        private final SkillInvoker invoker;

        SkillHttpHandler(SkillInvoker invoker) {
            this.invoker = invoker;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!"POST".equalsIgnoreCase(method)) {
                send(exchange, 405, errorBody("only POST supported"));
                return;
            }
            String raw = readBody(exchange.getRequestBody());
            String query;
            try {
                query = parseQuery(raw, exchange.getRequestURI().getQuery());
            } catch (IllegalArgumentException e) {
                send(exchange, 400, errorBody(e.getMessage()));
                return;
            }
            Map<String, Object> result = invoker.invoke(query);
            byte[] bytes = JSON.writeValueAsBytes(result);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    static String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    static String parseQuery(String jsonBody, String uriQuery) {
        if (jsonBody != null && !jsonBody.isBlank()) {
            JsonNode root;
            try {
                root = JSON.readTree(jsonBody);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("invalid JSON: " + e.getOriginalMessage());
            }
            JsonNode q = root.get("query");
            if (q != null && q.isTextual()) {
                return q.asText();
            }
            throw new IllegalArgumentException("JSON body must contain string field \"query\"");
        }
        if (uriQuery != null) {
            for (String pair : uriQuery.split("&")) {
                int i = pair.indexOf('=');
                if (i > 0 && "query".equals(pair.substring(0, i))) {
                    return java.net.URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalArgumentException("empty body; send {\"query\":\"...\"}");
    }

    static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    static byte[] errorBody(String message) throws IOException {
        return JSON.createObjectNode().put("error", message).toString().getBytes(StandardCharsets.UTF_8);
    }
}

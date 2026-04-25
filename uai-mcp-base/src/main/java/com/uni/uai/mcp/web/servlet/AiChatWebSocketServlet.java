package com.uni.uai.mcp.web.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket AI 对话服务端实现
 * 所有逻辑都封装在这个 Servlet 中，处理连接、消息和会话管理，包含定时问候功能
 * 
 * 
	代码说明
	这个简化版的 WebSocket 服务端实现了以下核心功能：
	
	会话管理：使用 sessionMap 存储所有活跃连接，messageHistoryMap 存储每个会话的消息历史
	连接处理：通过 @OnOpen、@OnClose、@OnError 注解处理连接的建立、关闭和错误
	消息处理：通过 @OnMessage 注解接收客户端消息，并根据消息类型进行处理
	消息格式：定义了 Message 和 Payload 内部类，标准化消息格式
	AI 响应：包含简单的 generateAiResponse 方法生成响应，实际应用中可替换为真实 AI 服务调用

	部署说明
	将项目打包为 WAR 文件
	部署到支持 Servlet 5.0 和 WebSocket 2.0 的容器（如 Tomcat 10+）
	服务端 WebSocket 端点为：ws://服务器地址:端口/项目上下文路径/ws/ai-chat
	与之前提供的前端页面配合使用时，需确保前端的 WS_URL 与服务端端点匹配
	
	
	定时任务实现：
	添加了ScheduledExecutorService作为定时任务执行器
	在静态初始化块中启动定时任务，每 10 秒执行一次问候发送
	实现sendGreetingsToAll()方法，向所有符合条件的客户端发送问候
	
	问候控制机制：
	添加greetingEnabledMap存储每个会话的问候启用状态
	新连接默认启用问候功能
	客户端发送 "停止"（不区分大小写）指令时，关闭该会话的问候功能
	新增 "开启问候" 指令支持，可重新启用定时问候
	
	消息处理增强：
	新增SYSTEM_GREETING消息类型，专门用于定时问候
	连接确认消息中添加了关于定时问候的说明
	在会话恢复时保持原有的问候设置
	
	用户体验优化：
	准备了多条问候语，每次随机发送一条，避免重复
	发送问候时在控制台输出日志，便于调试
	停止问候后会发送确认消息给客户端
	
	使用说明
	客户端连接后，会每 10 秒收到一条系统问候消息
	发送 "停止" 指令可取消定时问候
	发送 "开启问候" 可重新启用定时问候功能
	服务端会记录每个客户端的问候状态，会话保持期间有效
	
	何时初始化
	在非spring项目中，web容器会自动扫描包下的所有@ServerEndpoint的类，并完成初始化。
	而在springboot中，通过在WebSocketConfig中手动通过@bean，将其实例化为spring管理的bean。
	
	
	负载均衡长链接保持问题
	域名通常通过负载均衡（如 Nginx）分发请求到集群中的不同服务器。
	WebSocket 是长连接，若客户端首次连接到服务器 A，后续重连时被负载均衡分配到服务器 B，而服务器 B 没有客户端的会话信息（sessionMap 等状态存储在单服务器内存中），会导致：
	重连失败（服务器 B 无法识别客户端的会话 ID）。
	消息发送失败（客户端状态仅保存在服务器 A，服务器 B 无对应数据）。
	解决方案
	（1）确保负载均衡支持 WebSocket 长连接
	Nginx 配置示例（关键配置）：
		upstream websocket_servers {
		    server 192.168.1.101:8080;
		    server 192.168.1.102:8080;
		    # 启用会话亲和性（同一客户端固定到同一服务器）
		    ip_hash; 
		}
		
		server {
		    listen 80;
		    server_name your-domain.com;
		
		    location /ws/ {
		        # 转发 WebSocket 协议
		        proxy_pass http://websocket_servers;
		        proxy_http_version 1.1;
		        proxy_set_header Upgrade $http_upgrade;
		        proxy_set_header Connection "upgrade";
		        proxy_set_header Host $host;
		    }
		}
		关键：ip_hash 确保同一客户端（通过 IP 识别）始终连接到同一服务器，避免跨服务器会话问题；Upgrade 和 Connection 头配置支持 WebSocket 协议升级。
		
	
 */
@ServerEndpoint("/ws/ai-chat")
public class AiChatWebSocketServlet {

    // 静态变量，用于记录当前在线连接数
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    // 存储所有活跃的会话，key 为会话ID，value 为会话对象
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    // 存储每个会话的消息历史，用于上下文理解
    private static final Map<String, List<Message>> messageHistoryMap = new ConcurrentHashMap<>();

    // 存储每个会话是否启用定时问候
    private static final Map<String, Boolean> greetingEnabledMap = new ConcurrentHashMap<>();

    // 定时任务执行器
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // JSON 处理工具
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 当前会话对象
    private Session session;

    // 当前会话ID
    private String sessionId;

    // 问候语列表
    private static final List<String> GREETINGS = Arrays.asList(
        "你好呀！有什么可以帮助你的吗？",
        "正在想你需要什么帮助呢~",
        "有任何问题都可以问我哦！",
        "今天过得怎么样？需要什么帮助吗？",
        "我在这里随时为你服务！"
    );

    /**
     * 静态初始化块，启动定时任务
     */
    static {
        // 启动定时任务，每10秒执行一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendGreetingsToAll();
            } catch (Exception e) {
                System.err.println("定时问候任务执行失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 连接建立成功调用的方法
     * @param session 会话对象，包含客户端连接的所有信息
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        
        // 将新会话加入会话映射
        sessionMap.put(sessionId, session);
        
        // 为新会话初始化消息历史
        messageHistoryMap.put(sessionId, new ArrayList<>());
        
        // 新会话默认启用定时问候
        greetingEnabledMap.put(sessionId, true);
        
        // 在线人数加1
        int count = onlineCount.incrementAndGet();
        System.out.println("新连接加入，会话ID: " + sessionId + "，当前在线人数: " + count);
        
        try {
            // 发送连接确认消息给客户端
            sendMessageToClient(Message.builder()
                    .type("CONNECTION_ACK")
                    .payload(Payload.builder()
                            .sessionId(sessionId)
                            .message("连接已建立，每10秒会收到一条问候。回复'停止'可取消问候")
                            .timestamp(System.currentTimeMillis())
                            .build())
                    .build());
        } catch (IOException e) {
            System.err.println("发送连接确认消息失败: " + e.getMessage());
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 从会话映射中移除当前会话
        sessionMap.remove(sessionId);
        
        // 移除会话的消息历史
        messageHistoryMap.remove(sessionId);
        
        // 移除定时问候设置
        greetingEnabledMap.remove(sessionId);
        
        // 在线人数减1
        int count = onlineCount.decrementAndGet();
        System.out.println("连接关闭，会话ID: " + sessionId + "，当前在线人数: " + count);
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 会话对象
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到会话 " + sessionId + " 的消息: " + message);
        
        try {
            // 解析客户端发送的JSON消息
            Message clientMessage = objectMapper.readValue(message, Message.class);
            
            // 检查是否是"停止"指令（不区分大小写）
            if (clientMessage.getType().equals("USER_MESSAGE") && 
                clientMessage.getPayload() != null && 
                clientMessage.getPayload().getContent() != null &&
                clientMessage.getPayload().getContent().trim().equalsIgnoreCase("停止")) {
                
                // 停止定时问候
                greetingEnabledMap.put(sessionId, false);
                
                // 发送确认消息
                sendMessageToClient(Message.builder()
                        .type("SYSTEM_NOTICE")
                        .payload(Payload.builder()
                                .sessionId(sessionId)
                                .message("已停止定时问候")
                                .timestamp(System.currentTimeMillis())
                                .build())
                        .build());
                return;
            }
            
            // 处理不同类型的消息
            switch (clientMessage.getType()) {
                case "USER_MESSAGE":
                    handleUserMessage(clientMessage);
                    break;
                case "CONNECTION_ACK":
                    handleConnectionAck(clientMessage);
                    break;
                default:
                    // 处理未知类型消息
                    sendErrorMessage("未知的消息类型: " + clientMessage.getType());
            }
        } catch (Exception e) {
            System.err.println("处理消息时发生错误: " + e.getMessage());
            try {
                sendErrorMessage("处理消息失败: " + e.getMessage());
            } catch (IOException ex) {
                System.err.println("发送错误消息失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 发生错误时调用
     * @param session 会话对象
     * @param error 错误对象
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("会话 " + sessionId + " 发生错误: " + error.getMessage());
        error.printStackTrace();
        try {
            sendErrorMessage("服务器错误: " + error.getMessage());
        } catch (IOException e) {
            System.err.println("发送错误消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户消息
     * @param message 用户消息对象
     */
    private void handleUserMessage(Message message) throws IOException {
        // 保存用户消息到历史记录
        List<Message> history = messageHistoryMap.get(sessionId);
        history.add(message);
        
        // 限制历史记录长度，防止内存溢出
        if (history.size() > 20) {
            history.remove(0);
        }
        
        // 获取用户输入内容
        String userInput = message.getPayload().getContent();
        
        // 生成AI响应（实际应用中应调用真实的AI服务）
        String aiResponse = generateAiResponse(userInput, history);
        
        // 构建AI响应消息
        Message aiMessage = Message.builder()
                .type("AI_RESPONSE")
                .payload(Payload.builder()
                        .sessionId(sessionId)
                        .content(aiResponse)
                        .timestamp(System.currentTimeMillis())
                        .build())
                .build();
        
        // 保存AI响应到历史记录
        history.add(aiMessage);
        
        // 发送AI响应给客户端
        sendMessageToClient(aiMessage);
    }

    /**
     * 处理连接确认消息
     * @param message 连接确认消息
     */
    private void handleConnectionAck(Message message) throws IOException {
        // 如果客户端提供了会话ID，尝试恢复会话
        if (message.getPayload() != null && message.getPayload().getSessionId() != null) {
            String clientSessionId = message.getPayload().getSessionId();
            
            // 如果客户端提供的会话ID存在，使用该ID
            if (sessionMap.containsKey(clientSessionId)) {
                // 移除旧的会话ID映射
                sessionMap.remove(sessionId);
                
                // 更新会话ID
                this.sessionId = clientSessionId;
                sessionMap.put(sessionId, session);
                
                // 恢复定时问候设置（默认启用）
                if (!greetingEnabledMap.containsKey(sessionId)) {
                    greetingEnabledMap.put(sessionId, true);
                }
                
                System.out.println("会话ID已更新为客户端提供的值: " + sessionId);
                
                // 发送会话恢复确认
                sendMessageToClient(Message.builder()
                        .type("SYSTEM_NOTICE")
                        .payload(Payload.builder()
                                .sessionId(sessionId)
                                .message("会话已恢复，每10秒会收到一条问候。回复'停止'可取消问候")
                                .timestamp(System.currentTimeMillis())
                                .build())
                        .build());
            }
        }
    }

    /**
     * 生成AI响应（简单实现）
     * @param userInput 用户输入
     * @param history 消息历史
     * @return AI响应文本
     */
    private String generateAiResponse(String userInput, List<Message> history) {
        // 简单的响应逻辑，实际应用中应替换为真实的AI服务调用
        if (userInput.contains("你好") || userInput.contains("hello")) {
            return "你好！我是AI助手，有什么可以帮助你的吗？";
        } else if (userInput.contains("时间") || userInput.contains("现在几点")) {
            return "当前时间是: " + new Date().toString();
        } else if (userInput.contains("人数") || userInput.contains("在线")) {
            return "当前在线人数: " + onlineCount.get();
        } else if (userInput.contains("会话ID") || userInput.contains("session")) {
            return "你的会话ID是: " + sessionId;
        } else if (userInput.contains("问候") && userInput.contains("开启")) {
            // 重新开启问候
            greetingEnabledMap.put(sessionId, true);
            return "已重新开启定时问候，每10秒发送一次";
        } else {
            return "你说的是：\"" + userInput + "\"。这是一个简单的AI响应，实际应用中会调用真实的AI服务来生成更智能的回答。";
        }
    }

    /**
     * 向客户端发送消息
     * @param message 要发送的消息对象
     */
    private void sendMessageToClient(Message message) throws IOException {
        if (session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.getBasicRemote().sendText(jsonMessage);
            System.out.println("向会话 " + sessionId + " 发送消息: " + jsonMessage);
        } else {
            System.err.println("无法向会话 " + sessionId + " 发送消息，连接已关闭");
        }
    }

    /**
     * 发送错误消息给客户端
     * @param errorMessage 错误信息
     */
    private void sendErrorMessage(String errorMessage) throws IOException {
        sendMessageToClient(Message.builder()
                .type("ERROR")
                .payload(Payload.builder()
                        .message(errorMessage)
                        .timestamp(System.currentTimeMillis())
                        .build())
                .build());
    }

    /**
     * 向所有启用了问候功能的客户端发送问候消息
     */
    private static void sendGreetingsToAll() {
        // 随机选择一条问候语
        String greeting = GREETINGS.get(new Random().nextInt(GREETINGS.size()));
        
        // 遍历所有会话，发送问候
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            String sessionId = entry.getKey();
            Session session = entry.getValue();
            
            // 只向启用了问候功能且连接打开的会话发送
            if (greetingEnabledMap.getOrDefault(sessionId, true) && session.isOpen()) {
                try {
                    Message greetingMessage = Message.builder()
                            .type("SYSTEM_GREETING")
                            .payload(Payload.builder()
                                    .sessionId(sessionId)
                                    .message(greeting)
                                    .timestamp(System.currentTimeMillis())
                                    .build())
                            .build();
                    
                    String jsonMessage = objectMapper.writeValueAsString(greetingMessage);
                    session.getBasicRemote().sendText(jsonMessage);
                    System.out.println("向会话 " + sessionId + " 发送问候: " + greeting);
                } catch (IOException e) {
                    System.err.println("向会话 " + sessionId + " 发送问候失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 获取当前在线人数
     * @return 在线人数
     */
    public static int getOnlineCount() {
        return onlineCount.get();
    }

    /**
     * 消息封装类
     */
    public static class Message {
        private String type;
        private Payload payload;

        // 构建者模式，方便创建消息对象
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String type;
            private Payload payload;

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder payload(Payload payload) {
                this.payload = payload;
                return this;
            }

            public Message build() {
                Message message = new Message();
                message.type = this.type;
                message.payload = this.payload;
                return message;
            }
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Payload getPayload() { return payload; }
        public void setPayload(Payload payload) { this.payload = payload; }
    }

    /**
     * 消息负载类
     */
    public static class Payload {
        private String sessionId;
        private String clientName;
        private String ucid;
        private String content;
        private String message;
        private long timestamp;

        // 构建者模式，方便创建负载对象
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String sessionId;
            private String clientName;
            private String ucid;
            private String content;
            private String message;
            private long timestamp;

            public Builder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }
            public Builder clientName(String clientName) {
                this.clientName = clientName;
                return this;
            }
            public Builder ucid(String ucid) {
                this.ucid = ucid;
                return this;
            }

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder timestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Payload build() {
                Payload payload = new Payload();
                payload.sessionId = this.sessionId;
                payload.clientName = this.clientName;
                payload.ucid = this.ucid;
                payload.content = this.content;
                payload.message = this.message;
                payload.timestamp = this.timestamp;
                return payload;
            }
        }

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientName() {
			return clientName;
		}
		public void setClientName(String clientName) {
			this.clientName = clientName;
		}
		public String getUcid() {
			return ucid;
		}
		public void setUcid(String ucid) {
			this.ucid = ucid;
		}
		public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
    

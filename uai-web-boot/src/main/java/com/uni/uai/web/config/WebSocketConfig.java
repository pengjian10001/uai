package com.uni.uai.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.uni.uai.mcp.web.servlet.AiChatWebSocketServlet;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 注册ServerEndpointExporter以支持@ServerEndpoint注解
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
    //不在AiChatWebSocketServlet类上添加spring的@Component，而是通过手动注册websocket的servlet为spring的bean
    @Bean
    public AiChatWebSocketServlet aiChatWebSocketServlet() {
        return new AiChatWebSocketServlet();
    }
    
    // 配置WebSocket容器，设置消息大小限制等
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 无需额外注册，因为使用@ServerEndpoint注解
    }
}

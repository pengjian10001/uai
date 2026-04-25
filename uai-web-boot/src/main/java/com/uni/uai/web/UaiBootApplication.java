package com.uni.uai.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;

import com.uni.uai.mcp.server.McpUpdateTimer;
import com.uni.ubag.log.timer.UbagLogTimer;

@SpringBootApplication
@ServletComponentScan(basePackages = {"com.uni.uai.web.listener","com.uni.uai.web.config", "com.lianjia.infrastructure.graceful.reload", "com.uni.uai.mcp.web.filter"}) // 指定监听器所在的包
@PropertySource(value = {"classpath:ubag-conf.properties"})
public class UaiBootApplication {
	//启动ubag日志
    UbagLogTimer timer = new UbagLogTimer();
    //启动定时更新Mcp资源
    McpUpdateTimer mcpUpdateTimer = new McpUpdateTimer();

	public static void main(String[] args) {
		SpringApplication.run(UaiBootApplication.class, args);
	}

}

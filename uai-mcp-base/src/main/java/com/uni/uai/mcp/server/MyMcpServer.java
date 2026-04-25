package com.uni.uai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.IdCreator;
import com.uni.ubag.common.util.IpUtils;
import com.uni.ubag.log.util.UbagLogUtil;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

public class MyMcpServer {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static MyHttpServletSseServerTransportProvider transportProvider = new MyHttpServletSseServerTransportProvider(McpExtendUtil.objectMapper, McpExtendUtil.messageEndpoint);
    public static MyHttpServletSseServerTransportProvider getTransportProvider() {
    	return transportProvider;
    }
    
    private static McpSyncServer syncServer = null;
	static {
		String logId = "MyMcpServer_" + IdCreator.getInstance("ubaglog").next();
    	UbagConf.setlogId(logId);
    	
		syncServer = McpServer.sync(transportProvider)
			    .serverInfo("uai-mcp-server", "1.0.0")
			    .capabilities(ServerCapabilities.builder()
			    	.resources(true, true)
			    	.prompts(true)
			        .tools(true)         // Enable tool support
			        .logging()           // Enable logging support
			        .build())
			    .build();
		try {
			//添加根据用户问题识别prompt的tools
			//ServerUtil.getInstance().addPromptIdentifyTool(syncServer);
			//添加tools
			ServerUtil.getInstance().addToolsFromDB(syncServer);
			//添加系统自带的一些mcp工具，例如 根据用户问题回答mcp库相关信息的工具等。
			ServerUtil.getInstance().addSystemTool(syncServer);
			//添加prompts
			ServerUtil.getInstance().addPromptsFromDB(syncServer);
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = String.format("MyMcpServer启动异常，e=%s", e.getClass() + "-" + e.getMessage());
			System.err.println(msg);
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "MyMcpServer启动异常", msg, e, false, 0L, ExceptionUtil.toStackTrace(e));

		}
        
	}
	
	//包可见性，用于McpUpdateTimer
	static McpSyncServer getMcpSyncServer() {
		return syncServer;
	}
	



}

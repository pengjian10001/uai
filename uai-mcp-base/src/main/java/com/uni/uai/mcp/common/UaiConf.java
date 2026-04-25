package com.uni.uai.mcp.common;

public class UaiConf {
	
	//chatmessage memory最大消息条数
	public static final int CHAT_MEMORY_MAX_MESSAGE = 15;
	//mcp client工具调用时，requestconf备份的参数名
	public static final String TOOL_QEQUEST_CONF_BACKUP_NAME = "_request_conf";
	//mcp client工具调用时，arguments备份的参数名
	public static final String TOOL_ARGUMENTS_BACKUP_NAME = "_arguments";
	//mcp服务本身的url
	public static final String MCP_SERVER_URL = "_mcp_server_url";
	
	//上传文件请求，文件信息保存在threadlocal的名称
	public static final String FILE_INFO_LIST_NAME = "fileInfoList";
	
	//sse的printwriter
	//public static final String SSE_WRITER = "_sse_writer";
	//sse的AsyncContext
	public static final String SSE_CONTEXT = "_sse_context";
	
	


}

package com.uni.uai.mcp.server;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.common.util.MapUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.data.model.DataSourceDesc;
import com.uni.ubag.log.util.UbagLogUtil;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import reactor.core.publisher.Mono;

/**
 * MCP扩展辅助工具类
 */
public class McpExtendUtil {
	private static Logger logger = LoggerFactory.getLogger(McpExtendUtil.class);
	public static final String messageEndpoint = "/mcp/message";
	
	public static final ObjectMapper objectMapper = new ObjectMapper();
	
	public static void setLogId(String sessionId) {
		UbagConf.setlogId("sid_"+sessionId);
	}

	/**
	 * 为在mcp的client和server端传递logId，在tools定义时，需要将logId等额外的参数放到header中，
	 * 所以，对于某个tools/call，其协议传递的消息类似
	 * 此方法与getHttpDataSourceDesc(String httpDataSourceDesc, Map<String, Object> arguments)配套。
	{
		"id": 2,
		"jsonrpc": "2.0",
		"method": "tools/call",
		"params": {
			"name": "chatbi",
			"arguments": {
				"params": {
					"source": {
						"messageType": "query",
						"groupId": "123",
						"type": "指标查询"
					},
					"message": "北京最近3个月门店联网量"
				},
				"headers": {
					"code": "user_code",
					"logId": "log_id",
					"pid": "pid",
					"type": "source_type",
					"ucid": "ucid"
				}
			}
		}
	}

	 * @param message
	 * @return
	 */
	public static void setLogpId(McpSchema.JSONRPCMessage message){
		if(message instanceof McpSchema.JSONRPCRequest request) {
			try {
				//加上日志
				String method = request.method();
				//只对tools/call获取logId。 TODO 其他方法待补充
				if(McpSchema.METHOD_TOOLS_CALL.equals(method)) {
					JSONObject params = JSONUtil.toJsonObject(request.params());
					JSONObject arguments = JSONUtil.getJSONObject(params, "arguments");
					setLogpId(arguments);
				}
			}catch (Exception e) {
				//e.printStackTrace();
				logError("setLogpId by request error", e);
			}
		}
	}
	
	public static void setLogpId(Map<String, Object> arguments){
		Map<String, Object> param = MapUtil.getMap(arguments, "params", null);
		Map<String, Object> header = MapUtil.getMap(arguments, "headers", null);
		String logId = null;
		//如果arguments传递了logId，则使用，否则从headers中获取
		logId = getLogIdFromMap(arguments);
		if(logId==null) {
			logId = getLogIdFromMap(param);
		}
		if(logId==null) {
			logId = getLogIdFromMap(header);
		}
		//由于使用mcp tool时，如果参数定义了logid，但是用户问答中却没有传递时，会默认生成一个，例如，对于logid和ucid会给出：
		//{logId=log_id_placeholder, ucid=user_id_placeholder}
		//或{"logId":"unique-log-id","ucid":"user-ucid"}
		//或{"logId":"random-log-id","ucid":"user-ucid"}
		if(logId!=null
				&& !logId.contains("placeholder")
				&& !logId.contains("unique-log")
				&& !logId.contains("random-log")
				&& logId.matches(".*[0-9]{7,}.*")) { //包含7个以上连续数字
			
			//这些logid的特点是没有数字
			UbagConf.setlogId(logId);
			String msg = "server端尝试从argument或arguments.params或arguments.header中获取到logId,logId="+logId;
			logger.info(msg);
			TimeTrace.markCall(msg, 0L, 0L);
		}
	}
	
	//提取用户文本中的logId，用于页面调试，例如用户提问： logid=12342312312，今天天气如何
	public static void setLogIdFromQuery(String queryText){
		List<String> list = RegexUtil.findMatchs(queryText, "log[iI]d[ ]*=[ ]*([a-zA-Z0-9_]+)", 1);
		if(list != null && list.size() > 0) {
			String logId = list.get(0);
			UbagConf.setlogId(logId);
			String msg = "从用户queryText中获取到logId,logId="+logId;
			logger.info(msg);
			TimeTrace.markCall(msg, 0L, 0L);
		}
	}
	
	private static String getLogIdFromMap(Map<String,Object> map) {
		String logId = null;
		if(map == null) {
			return null;
		}
		logId = MapUtil.getString(map, "log_id", null);
		if(logId==null) {
			logId = MapUtil.getString(map, "logId", null);
		}
		if(logId==null) {
			logId = MapUtil.getString(map, "logid", null);
		}
		return logId;
	}
	
	/**
	 * 参照MyMcpClient中appendResquestConfToArauments()方法，将Client设置的上下文重新设置到Server的上下文中
	 * @param arguments
	 */
	public static void setRequestConf(Map<String, Object> arguments){
		try {
			if(arguments.containsKey(UaiConf.TOOL_QEQUEST_CONF_BACKUP_NAME)) {
				Map<String, Object> requestConf = MapUtil.getMap(arguments, UaiConf.TOOL_QEQUEST_CONF_BACKUP_NAME, null);
				UbagConf.setAllRequestConf(requestConf);
				String msg = "server端从arguments获取requestconf,其中logId="+UbagConf.getlogId();
				logger.info(msg);
				TimeTrace.markCall(msg, 0L, 0L);
			}
		} catch (Exception e) {
			String msg = "server端从arguments获取上下文异常。e="+e.getClass()+":"+e.getMessage();
			logger.warn(msg, e);
			TimeTrace.markCall(msg, 0L, 1L);
		}
	}
	
	/**
	 * 注意，此方法与setLogpId(McpSchema.JSONRPCMessage message)配套，约定client和server端传递logId的格式。
	 * 所以，此处从arguments中分别取params作为真正的工具参数，而headers中传递logId。
	 * 如果要变动，这两个方法都需要修改
	 * @param httpDataSourceDesc
	 * @param arguments
	 * @return
	 */
	public static DataSourceDesc.Http getHttpDataSourceDesc(String httpDataSourceDesc, Map<String, Object> param, Map<String, Object> header) {
		JSONObject json = JSONUtil.toJsonObject(httpDataSourceDesc);
		//将参数添加到desc中
		json.put("param", param);
		json.put("header", header);
		DataSourceDesc.Http dataSourceDesc = new DataSourceDesc.Http().buildWithJsonString(json.toString());
		return dataSourceDesc;
	}
	
	public static void log(String key, Object value) {
		UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), key, JSONUtil.toJSONString(value), null, true, 0L, "");
	}
	
	public static void logError(String key, Exception e) {
		UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), key, e.getClass() + "-" + e.getMessage(), null, false, 0L, ExceptionUtil.toStackTrace(e));
	}
	
	/**
	public static void modifySession(McpServerSession session) {
		try {
			Field field = session.getClass().getDeclaredField("requestHandlers");
			field.setAccessible(true);
			Object value = field.get(session);
			if(value instanceof Map) {
				Map valueMap = (Map)value;
				McpServerSession.RequestHandler<McpSchema.ListToolsResult> handler = toolsListRequestHandler();
				valueMap.put(McpSchema.METHOD_TOOLS_LIST, handler);
				// 4. 反射替换原字段的值
	            field.set(session, valueMap);
				System.out.println("modifySession success");
			}
		} catch (Exception e) {
			System.err.println("modifySession error");
			e.printStackTrace();
		}
	}
	
	public static CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> getToolsValue()throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException{
		McpAsyncServer mcpAsyncServer = MyMcpServer.getMcpSyncServer().getAsyncServer();
		//Field field = mcpAsyncServer.getClass().getDeclaredField("tools");
		//field.setAccessible(true);
		CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> toolsValue = (CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification>)field.get(mcpAsyncServer);
		// 创建当前列表的完整备份（快照），后续其他线程修改原列表不会影响此备份
		CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> toolsBackup = new CopyOnWriteArrayList<>(toolsValue);
		return toolsBackup;
	}
	
	private static McpServerSession.RequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> toolsValue = getToolsValue();
		return (exchange, params) -> {
			//改为非stream的方式
			List<Tool> tools = new ArrayList<>();
			for (int i = 0; i < toolsValue.size(); i++) {
			    // 获取每个 AsyncToolSpecification 对象中提取 tool 属性
			    McpServerFeatures.AsyncToolSpecification toolSpecification = toolsValue.get(i);
			    Tool tool = toolSpecification.tool();
			    // 添加到结果列表
			    tools.add(tool);
			}
			
			long start = System.currentTimeMillis();
			String clientName = exchange.getClientInfo().name();
			// 默认返回所有，TODO 需改为默认返回空
			// 分两种情况，如果clientName是_mcp，则默认返回所有以_开头的所有，否则默认返回所有非_开头的所有，这样分开，是避免系统工具的描述，会影响业务Label
			List<Tool> newtools = new ArrayList<Tool>();
			if(clientName.equals(ServerUtil.mcpServerLabelName)) {
				for(Tool tool : tools) {
					if(tool.name().startsWith("_")) {
						newtools.add(tool);
					}
				}
			}else {
				for(Tool tool : tools) {
					if(!tool.name().startsWith("_")) {
						newtools.add(tool);
					}
				}
			}
			
			Map<String, Set<String>> labelToolMap = ServerUtil.getInstance().getLabelToolMap();
			//如果有限制，则新生成一个List返回
			if(labelToolMap.containsKey(clientName)) {
				newtools = new ArrayList<Tool>();
				Set<String> toolNameList = labelToolMap.get(clientName);
				for(Tool tool : tools) {
					String toolName = tool.name();
					if(toolNameList.contains(toolName)) {
						newtools.add(tool);
					}
				}
			}
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), "tool_list_"+clientName, JSONUtil.toJSONString(newtools), null, true, System.currentTimeMillis()-start, "labelToolMap:" + JSONUtil.toJSONString(labelToolMap));
			
			return Mono.just(new McpSchema.ListToolsResult(newtools, null));
		};
	}
	**/
	
	public static void main(String[] args) {
		String logId = "abc123456";
		System.out.println(logId.matches(".*[0-9]{5,}.*"));
		
		String queryText = "@testabc logId = 1eqwe123123你好";
		List<String> list = RegexUtil.findMatchs(queryText, "log[iI]d[ ]*=[ ]*([a-zA-Z0-9_]+)", 1);
		if(list != null && list.size() > 0) {
			logId = list.get(0);
			System.out.println(logId);
		}
	}
}

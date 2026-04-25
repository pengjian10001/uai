package com.uni.uai.mcp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.uni.uai.mcp.common.FileInfo;
import com.uni.uai.mcp.data.DataSourceUtil;
import com.uni.uai.mcp.jsonschema.JsonSchemaUtil;
import com.uni.uai.mcp.model.LabelPO;
import com.uni.uai.mcp.model.LabelToolPO;
import com.uni.uai.mcp.model.PromptPO;
import com.uni.uai.mcp.model.ToolExtPO;
import com.uni.uai.mcp.model.ToolPO;
import com.uni.uai.mcp.server.tools.DemoTool;
import com.uni.uai.mcp.server.tools.PromptTool;
import com.uni.uai.mcp.server.tools.RagTool;
import com.uni.uai.mcp.utils.FreeMarkerTemplate;
import com.uni.uai.mcp.utils.complier.CompilerUtil;
import com.uni.uai.mcp.utils.context.TemplateUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.common.util.MapUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.common.util.TimeUtil;
import com.uni.ubag.log.proxy.ProxyAction;
import com.uni.ubag.log.util.UbagLogUtil;

import dev.langchain4j.mcp.client.MyMcpClientUtil;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;

public class ServerUtil {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static ServerUtil instance = new ServerUtil();
	public static ServerUtil getInstance() {
		return instance;
	}

	//private Map<String, PromptPO> promptMapSucc = new HashMap<String, PromptPO>();
	//private Map<String, PromptPO> promptMapFail = new HashMap<String, PromptPO>();
	//private Map<String, ToolPO> toolMapSucc = new HashMap<String, ToolPO>();
	//private Map<String, ToolPO> toolMapFail = new HashMap<String, ToolPO>();
	
	//存储所有tool，额外存储工具返回值编译后的Class
	private Map<String, ToolExtPO> toolExtMap = new HashMap<String, ToolExtPO>();
	
	private Map<String, LabelPO> labelMap = new HashMap<String, LabelPO>();
	
	//mcp服务代理的label名。此名不存于数据库，用于归类mcp服务自身的工具
	public static final String mcpServerLabelName = "_mcp";
	//mcpServerLabelName对应的tools集合名
	private static Set<String> mcpServerToolSet = new HashSet<String>();


	//--------------------------prompt------------------------------
	public void addPrompt(McpSyncServer syncServer, PromptPO po, String name, String description, List<PromptArgument> arguments, BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> call) {
		UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "addPrompt1", po.toString(), new ProxyAction<Object>() {
			public Object exec() throws Throwable{
				Prompt prompt = new Prompt(name, description, arguments);
				//方法名必需匹配[a-zA-Z0-9_-]+$，否则执行时会报错
				if(!name.matches("[a-zA-Z0-9_-]+$")) {
					throw new RuntimeException(String.format("addPromptsFromDB异常，prompt名必需匹配[a-zA-Z0-9_-]+$， %s", name));
				}
				// Sync tool specification
				var syncPromptSpecification = new McpServerFeatures.SyncPromptSpecification(
						prompt, call
				);
				// Register tools, resources, and prompts
				syncServer.addPrompt(syncPromptSpecification);
				return null;
			}
		});
	}

	public void addPromptsFromDB(McpSyncServer syncServer) {
		List<PromptPO> list = DataSourceUtil.getInstance().getPromptsFromDB();
		if(list != null && list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				PromptPO po = list.get(i);
				this.addPrompt(syncServer, po);
			}
		}
	}

	/**
	 * 更新Prompt
	 * @param syncServer
	 */
	public void updatePromptsFromDB(McpSyncServer syncServer) {
		if(syncServer == null) {
			return;
		}
		List<PromptPO> list = DataSourceUtil.getInstance().getRecentlyPromptsFromDB();
		UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "updatePromptsFromDB", list.toString(), new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				if(list != null && list.size() > 0) {
					for(int i = 0; i < list.size(); i++) {
						PromptPO po = list.get(i);
						String name = po.getName();
						
						for(int j = 0; j < 5; j++) {
							//由于删除可能失败，此处尝试3次
							Boolean issuccess = false;
							try {
								//先尝试删除旧的
								issuccess = removePrompt(syncServer, name);
								//如果mtime时间不等，则以新的为主
								if(po.getState()==0) {
									issuccess = addPrompt(syncServer, po);
								}
							} catch (Exception e) {
								UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), String.format("updatePromptsFromDB 更新%s 第%s次异常", name,j), ExceptionUtil.toStackTrace(e), e, issuccess, 0L, "");
							}
							TimeTrace.markCall(String.format("updatePromptsFromDB 更新%s 第%s次", name,j), 0L, 0L);
							if(issuccess!=null && issuccess) {
								break;
							}
						}
					}
				}
				return true;
			}
		});
	}


	/**
	 * 以PromptPO对象的方式添加一个Prompt
	 * @param syncServer
	 * @param po
	 */
	private boolean addPrompt(McpSyncServer syncServer, PromptPO po) {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "addPrompt2", po.toString(), new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				String name = po.getName();
				String description = po.getDescription();
				//TODO 验证schema的合法性
				/**
				 * schema的格式如下：
				 [
				    {
				      "name": "code",
				      "description": "要解释的代码",
				      "required": true
				    },
				    {
				      "name": "language",
				      "description": "程序语言",
				      "required": false
				    }
				]
				 */
				String schema = po.getParamSchema();
				List<PromptArgument> listPromptArgument = JSONUtil.parseArray(schema, PromptArgument.class);
				//做一个简单的验证
				for(int i= 0; i< listPromptArgument.size(); i++) {
					//如果参数schema传递的是一个{}，而不是数组类型，得到的listPromptArgument是一个包含空对象的数组[{}]，需要检测到这种情况
					if(listPromptArgument.get(i) == null 
							|| !(listPromptArgument.get(i) instanceof PromptArgument)) {
						throw new RuntimeException("addPrompt. arguments的元素为空，或不能转换为PromptArgument类型");
					}
					PromptArgument pa = listPromptArgument.get(i);
					if(pa == null || pa.name() == null || "".equals(pa.name())) {
						throw new RuntimeException("addPrompt. arguments必需为JSON数组，且name属性不能为空");
					}
				}

				/**
				 prompt模版，是可以带${var}的文本，例如：
				  """
			    			解释${language!'java'}代码是如何工作的：\n\n
			    			${code}
			      """
				 */
				String promptTemplate = po.getPromptTemplate();
				/**
				 promptConfig格式为：
				 {
				 	"role" : "user"
				 }
				 */
				PromptPO.PromptConfig promptConfig = JSONUtil.parseObject(po.getPromptConfig(), PromptPO.PromptConfig.class);

				addPrompt(syncServer, po , name, description, listPromptArgument, (exchange, arguments) -> {
					McpSchema.GetPromptResult callResult = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, String.format("server端执行[%s]prompt", name), arguments==null?"":arguments.toString(), new ProxyAction<McpSchema.GetPromptResult>() {
						public McpSchema.GetPromptResult exec() throws Throwable{
							//logger.info("arguments: " + arguments);
							String resolved = FreeMarkerTemplate.parse(arguments.arguments(), promptTemplate);
							Role role = Role.USER;
							if("ASSISTANT".equalsIgnoreCase(promptConfig.getRole())) {
								role = Role.ASSISTANT;
							}
							PromptMessage promptMessage = new PromptMessage(role, new McpSchema.TextContent(resolved));
							List<PromptMessage> messages = List.of(
									promptMessage
					        		);
					        // Prompt实现
					        return new McpSchema.GetPromptResult(description, messages);
						}
						
						@Override
						public McpSchema.GetPromptResult nullHandler(Throwable e) {
							McpSchema.GetPromptResult result = null;
							if(e!=null) {
								//工具调用异常，抛出
								throw new RuntimeException(e);
							}
							return result;
						}
					});
					return callResult;
			    });
				return true;
			}
		});
		return result;
	}
	
	private Boolean removePrompt(McpSyncServer syncServer, String name) {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "removePrompt", name, new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				syncServer.removePrompt(name);
				return true;
			}
		});
		return result;
	}



	//--------------------------tools------------------------------
	/**
	 * 添加工具
	 * @param syncServer
	 * @param name
	 * @param description
	 * @param schema
	 * @param call
	 */
	public Boolean addTool(McpSyncServer syncServer, ToolPO po, String name, String description, String schema, BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "addTool", name, new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				//使用langchain4j mcp客户端的方法验证schema
				MyMcpClientUtil.checkSchema(schema);
				//方法名必需匹配[a-zA-Z0-9_-]+$，否则执行时会报错
				if(!name.matches("[a-zA-Z0-9_-]+$")) {
					throw new RuntimeException(String.format("addToolsFromDB异常，方法名必需匹配[a-zA-Z0-9_-]+$， %s", name));
				}
				//处理工具扩展
				toolExt(po, name);
				//如果工具的返回值类型不为空，则生成对应的返回值约束，添加到工具的描述中
				Tool tool = new Tool(name, description, schema);
				// Sync tool specification
				var syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
				    tool, call
				);
				//为了避免添加时并发读，使用同步
				//Object lockObject = McpExtendUtil.getToolsValue();
				// Register tools, resources, and prompts
				synchronized (ServerUtil.class) {
					syncServer.addTool(syncToolSpecification);
				}
				//toolMapSucc.put(name, po);
				return true;
			}
		});
		return result;
	}

	public void addToolsFromDB(McpSyncServer syncServer) {
		List<ToolPO> list = DataSourceUtil.getInstance().getToolsFromDB();
		if(list != null && list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				ToolPO tool = list.get(i);
				this.addTool(syncServer, tool);
			}
		}
	}

	/**
	 * 更新Tools
	 * @param syncServer
	 */
	public Boolean updateToolsFromDB(McpSyncServer syncServer) {
		if(syncServer == null) {
			return false;
		}
		List<ToolPO> list = DataSourceUtil.getInstance().getRecentlyToolsFromDB();
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "updateToolsFromDB", list.toString(), new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				if(list != null && list.size() > 0) {
					for(int i = 0; i < list.size(); i++) {
						ToolPO po = list.get(i);
						String name = po.getName();
						for(int j = 0; j < 5; j++) {
							//由于删除可能失败，此处尝试3次
							Boolean issuccess = false;
							try {
								//先尝试删除旧的
								issuccess = removeTool(syncServer, name);
								//如果mtime时间不等，则以新的为主
								if(po.getState()==0) {
									issuccess = addTool(syncServer, po);
								}
							} catch (Exception e) {
								UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), String.format("updateToolsFromDB 更新%s 第%s次异常", name,j), ExceptionUtil.toStackTrace(e), e, issuccess, 0L, "");
							}
							TimeTrace.markCall(String.format("updateToolsFromDB 更新%s 第%s次", name,j), 0L, 0L);
							if(issuccess!=null && issuccess) {
								break;
							}
						}
					}
					//不需要通知，因为mcp的removeTool和addTool中已经会notify了
					//syncServer.notifyToolsListChanged();
				}
				return true;
			}
		});
		return result;
	}
	
	Map<String, Set<String>> labelToolMap = new ConcurrentHashMap<String,Set<String>>();
	public Map<String, Set<String>> getLabelToolMap(){
		return labelToolMap;
	}
	
	/**
	 * 获取并更新LabelName和ToolName的关系，从数据库中更新，并从内部添加的更新。
	 */
	public void updateLabelToolFromDBAndMcpSelf() {
		//从数据库更新
		List<LabelToolPO> list = DataSourceUtil.getInstance().getLabelToolsFromDB();
		Map<String, Set<String>> map = new ConcurrentHashMap<String,Set<String>>();
		for(int i = 0; i < list.size(); i++) {
			LabelToolPO po = list.get(i);
			String labelName = po.getLabelName();
			String toolName = po.getToolName();
			Set<String> toolNameList = map.get(labelName);
			if(toolNameList == null) {
				toolNameList = new HashSet<String>();
				map.put(labelName, toolNameList);
			}
			toolNameList.add(toolName);
		}
		labelToolMap = map;
		//从内部添加的tools更新
		labelToolMap.put(mcpServerLabelName, mcpServerToolSet);
	}

	/**
	 * 以ToolPO的形式添加Tool
	 * @param syncServer
	 * @param tool
	 */
	private Boolean addTool(McpSyncServer syncServer, ToolPO po) {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "addTool", po.toString(), new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				String name = po.getName();
				String description = po.getDescription();
				//TODO 验证schema的合法性
				String schema = po.getParamSchema();

				addTool(syncServer, po, name, description, schema, (exchange, arguments) -> {
					
					CallToolResult callToolResult = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, String.format("server端执行[%s]工具", name), arguments==null?"":arguments.toString(), new ProxyAction<CallToolResult>() {
						public CallToolResult exec() throws Throwable{
							String resultText = null;
							String clientName = exchange.getClientInfo().name();
							//logger.info("arguments: " + arguments);
							//对http请求，为传递额外的header参数（以传递logId及ucid信息），与虚拟人系统约定将arguments分为params和headers两个部分，真正的http请求参数是在params中。
							//logId的处理，在McpExtendUtil.setLogpId中。
							Map<String, Object> param = null;
							Map<String, Object> header = null;
							if(arguments != null 
									&& arguments.containsKey("params")
									&& arguments.containsKey("headers")) {
								param = MapUtil.getMap(arguments, "params", null);
								header = MapUtil.getMap(arguments, "headers", null);
							}else if(arguments != null 
									&& arguments.containsKey("headers")) {
								//如果arguments不同时包含params和headers，则不是虚拟人系统，可以按照常规的方式解析arguments，arguments整体就是参数
								//但也支持传递headers作为http的请求头
								param = arguments;
								header = MapUtil.getMap(arguments, "headers", null);
							}else {
								//如果arguments不同时包含params和headers，则不是虚拟人系统，可以按照常规的方式解析arguments，arguments整体就是参数
								param = arguments;
							}
							//参照MyMcpClient中appendResquestConfToArauments()方法，将Client设置的上下文重新设置到Server的上下文中，包括logId
							McpExtendUtil.setRequestConf(arguments);
							//下面这些设置logId的方法保留，因为有些client端没有按照配套的协议传递LogId，所以尝试从以下方式获取
							//此处重新根据参数设置logId，是因为mcp会多次经过webfilter，清空Threadlocal
							//为了和调用方logId保持一致，此处显式根据参数设置logId
							McpExtendUtil.setLogpId(arguments);
							//无论如何，header中必需包含logId
							header = appendLogIdToHeader(header);
							Object httpResult = DataSourceUtil.getInstance().exeHttp(po.getDataSourceDesc(), param, header);
							List<Content> listContent = new ArrayList<Content>();
							resultText = httpResult.toString();
							listContent.add(new McpSchema.TextContent(resultText));
					        // Tool implementation
					        return new CallToolResult(listContent, false);
						}
						
						@Override
						public CallToolResult nullHandler(Throwable e) {
							CallToolResult result = null;
							if(e!=null) {
								//工具调用异常，抛出
								throw new RuntimeException(e);
							}
							return result;
						}
					});
					return callToolResult;
			    });
				return true;
			}
		});
		return result;
	}
	
	//无论如何设置logId到header中
	private Map<String, Object> appendLogIdToHeader(Map<String, Object> header){
		if(header == null) {
			//如果没有header，则创建一个，并附加logId
			Map<String, Object> newHeader = new HashMap<String, Object>();
			newHeader.put("logId", UbagConf.getlogId());
			return newHeader;
		}else {
			//如果header中没有logId，则设置
			if(!header.containsKey("logId")) {
				header.put("logId", UbagConf.getlogId());
			}
			return header;
		}
	}
	
	private Boolean removeTool(McpSyncServer syncServer, String name) {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "removeTool", name, new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				//为避免修改时，别的地方读取，导致错误，使用同步
				//为了避免添加时并发读，使用同步
				//Object lockObject = McpExtendUtil.getToolsValue();
				synchronized (syncServer) {
					syncServer.removeTool(name);
				}
				return true;
			}

			
		});
		return result;
	}
	
	/**
	 * 处理工具扩展toolExt。
	 * 如果包含returnclass，则生成class，供后续使用
	 * @param po
	 * @param name
	 * @param description
	 * @return
	 * @throws Exception
	 */
	private Boolean toolExt(ToolPO po, String name) throws Exception {
		Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.CODE, "removeTool", name, new ProxyAction<Boolean>() {
			public Boolean exec() throws Throwable{
				//如果工具配置了returnClass
		 		String returnClass = po.getReturnClass();
				//可以根据返回值，优化description，为了避免returnClass随意插入数据，此处判断长度必需大于10才会编译
				if(returnClass != null && returnClass.trim().length() > 10) {
					ToolExtPO toolExPO = toolExtMap.get(name);
					if(toolExPO == null) {
						toolExPO = ToolExtPO.createToolPO(po.getId(), po.getName(), po.getReturnClass(), null);
						toolExtMap.put(name, toolExPO);
					}
					if(toolExPO.getReturnClassAfterCompiler() == null //如果还没有编译returnClazz
							||!returnClass.equals(toolExPO.getReturnClass())) { //或者returnClazz有变化
						//捕获字符串public class classA中的classA
						String classNameRegex = "public\\s+class\\s([a-zA-Z0-9_-]+)";
						List<String> matches = RegexUtil.findMatchs(returnClass, classNameRegex, 1);
						if(matches == null || matches.size() == 0) {
							throw new RuntimeException(String.format("tool=%s的returnClass中类格式不对，类定义必需包含public class ClassA {......}", name));
						}
						
						String packageName = "com.uai.userpackage";
						//将name附加到原有类名的后面，避免用户定义的类名重复
						String className = matches.get(0)+"_"+name;
						String fullClassName = packageName + "." + className;
						returnClass = returnClass.replaceFirst(classNameRegex, String.format("public class %s ", className));
						//并覆盖原有包名（如果有），避免用户定义类名覆盖了工程中的类名
						
						String packageRegex = "^\\s*package[^;]+;";
						String packageStr = String.format("package " + packageName + ";");
						List<String> matchespackage = RegexUtil.findMatchs(returnClass, packageRegex, 0);
						if(matchespackage != null && matchespackage.size() > 0) {
							//如果包含package，则替换
							returnClass = returnClass.replaceFirst("^\\s*package[^;]+;", packageStr);
						}else {
							//如果没有包，则添加
							returnClass = packageStr + "\n" + returnClass;
						}
						
						Class<?> clazz = CompilerUtil.getInstance().compile(fullClassName, returnClass);
						toolExPO.setReturnClassAfterCompiler(clazz);
						String msg = String.format("编译tool的returnClass，toolname=%s, before-className=%s, after-fullClassName=%s", name, matches.get(0), fullClassName);
						logger.info(msg);
					}
				}
				return true;
			}
		});
		return result;
		
		
	}
	
	private Class<?> getToolReturnClass(String toolName){
		ToolExtPO extPO = toolExtMap.get(toolName);
		if(extPO == null) {
			return null;
		}else {
			return extPO.getReturnClassAfterCompiler();
		}
	}
	
	//获取工具返回值约束的描述
	public String getToolReturnConstraintDescription(String toolName) {
		Class<?> returnClassAfterCompiler = this.getToolReturnClass(toolName);
		if(returnClassAfterCompiler != null) {
			return JsonSchemaUtil.getInstance().classToJsonSchemaString(returnClassAfterCompiler);
		}else {
			return null;
		}
	}

	/**
	 * 添加一个根据用户问题识别对应的Prompt的工具
	 * @param syncServer
	 */
	public void addPromptIdentifyTool(McpSyncServer syncServer) {
		//内部工具，都以_开头，在McpExtendUtil中会区别对待
		var name = "_promptIdentify";
		var description = "根据用户问题识别对应的Prompt";
		//参数。prompt名称可选，如果有，则从数据库中获取指定prompt，否则使用默认的
		var schema = """
		        {
		          "type": "object",
		          "properties": {
		            "queryText": {
		              "type": "string",
		              "description": "用户问题"
		            },
		            "promptName": {
		              "type": "string",
		              "description": "prompt名称"
		            }
		          },
		          "required": ["queryText"]
		        }
	            """;
		ToolPO po = ToolPO.createToolPO(name, description, schema, "", "");
		addTool(syncServer, po, name, description, schema, (exchange, arguments) -> {
			if(arguments.get("promptName") != null) {
				//TODO 可以根据用户传递的模版名称，来识别模版。
				String promptName = (String)arguments.get("promptName");
			}
			String queryText = (String) arguments.get("queryText");
			List<Content> list = new ArrayList<Content>();
			String prompt = PromptTool.getInstance().promptIdentify(queryText);
			list.add(new McpSchema.TextContent(prompt));
	        // Tool implementation
	        return new CallToolResult(list, false);
	    });
		//mcpserver自身的tools名字添加到Set中
		mcpServerToolSet.add(name);
	}
	
	/**
	 * 添加一些系统自带的MCP工具
	 * @param syncServer
	 */
	public void addSystemTool(McpSyncServer syncServer) {
		this.addMcpServerInfoTool(syncServer);
		this.addJsonSqlAnalysisTool(syncServer);
	}
	
	private void addMcpServerInfoTool(McpSyncServer syncServer) {
		//内部工具，都以_开头，在McpExtendUtil中会区别对待
		var name = "_mcpServerInfo";
		var description = "回答mcp server代理自身有什么能力（包括但不限于角色、工具、标签等）";
		//参数。prompt名称可选，如果有，则从数据库中获取指定prompt，否则使用默认的
		var schema = """
		        {
		          "type": "object",
		          "properties": {
		            "queryText": {
		              "type": "string",
		              "description": "用户问题"
		            }
		          },
		          "required": ["queryText"]
		        }
	            """;
		ToolPO po = ToolPO.createToolPO(name, description, schema, "", "");
		addTool(syncServer, po, name, description, schema, (exchange, arguments) -> {
			String queryText = (String) arguments.get("queryText");
			List<Content> list = new ArrayList<Content>();
			String result = RagTool.getInstance().queryMcpServerInfoFromDB(queryText);
			list.add(new McpSchema.TextContent(result));
	        // Tool implementation
	        return new CallToolResult(list, false);
	    });
		//mcpserver自身的tools名字添加到Set中
		mcpServerToolSet.add(name);
	}
	
	//'jsonsql_analysis'工具，在MCPClientUtil中处理用户上传的excel时，明确设置使用这个工具
	//这个工具名称不要以“_”开头，不作为系统工具，否则clientName为非_mcp外，绑定不了这个工具
	public final String jsonSqlAnalysisToolName = "jsonsql_analysis";
	private void addJsonSqlAnalysisTool(McpSyncServer syncServer) {
		//内部工具，都以_开头，在McpExtendUtil中会区别对待
		var name = jsonSqlAnalysisToolName;
		var description = "基于用户上传的excel文件，生成SQLite表信息，回答用户的分析相关问题（包括但不限于查询、统计、趋势、差异、极值、合并等）";
		//参数。prompt名称可选，如果有，则从数据库中获取指定prompt，否则使用默认的
		var schema = """
			{
			  "type": "object",
			  "properties": {
			    "queryText": {
			      "type": "string",
			      "description": "用户提出的问题或需求，用于明确工具需要处理的核心任务"
			    },
			    "uploadFiles": {
			      "type": "array",
			      "description": "用户上传的多个文件列表，每个元素包含单个文件的关键信息",
			      "items": {
			        "type": "object",
			        "properties": {
			          "fileName": {
			            "type": "string",
			            "description": "用户上传文件的原始文件名（含后缀），如「数据报表.xlsx」"
			          },
			          "fileSize": {
			            "type": "integer",
			            "format": "int64",
			            "description": "文件大小，单位为字节（Byte）"
			          },
			          "contentType": {
			            "type": "string",
			            "description": "文件的 MIME 类型，用于标识文件格式，如「application/vnd.openxmlformats-officedocument.spreadsheetml.sheet」（Excel）、「text/csv」（CSV）"
			          },
			          "tempFilePath": {
			            "type": "string",
			            "description": "文件在服务端存储的临时路径（绝对路径或相对路径），用于工具定位并读取文件内容，如「/tmp/upload/123456.xlsx」"
			          }
			        },
			        "required": ["fileName", "tempFilePath"]
			      }
			    }
			  },
			  "required": ["queryText", "uploadFiles"]
			}
	            """;
		ToolPO po = ToolPO.createToolPO(name, description, schema, "", "");
		addTool(syncServer, po, name, description, schema, (exchange, arguments) -> {
			String queryText = (String) arguments.get("queryText");
			Object fileInfoObj = arguments.get("uploadFiles");
			List<FileInfo> fileInfos = JSONUtil.parseArray(JSONUtil.toJSONString(fileInfoObj), FileInfo.class);
			List<Content> list = new ArrayList<Content>();
			String result = RagTool.getInstance().queryAyanlysisFromSqlite(queryText, fileInfos);
			list.add(new McpSchema.TextContent(result));
	        // Tool implementation
	        return new CallToolResult(list, false);
	    });
		//不作为系统工具，可以用在任何clientName中
		//mcpserver自身的tools名字添加到Set中
		//mcpServerToolSet.add(name);
	}
	
	/**
	 * 添加一个测试工具
	 * @param syncServer
	 */
	public void addDemoTool(McpSyncServer syncServer) {
		var name = "beikeTrade";
		var description = "计算贝壳的交易额";
		var schema = """
		        {
		          "type": "object",
		          "properties": {
		            "location": {
		              "type": "string",
		              "description": "城市名称"
		            },
		            "year": {
		              "type": "number",
		              "description": "年份"
		            }
		          },
		          "required": ["location"]
		        }
	            """;
		ToolPO po = ToolPO.createToolPO(name, description, schema, "", "");
		addTool(syncServer, po, name, description, schema, (exchange, arguments) -> {
	    	//McpSchema.ClientCapabilities.Sampling sampling = exchange.getClientCapabilities().sampling();
	    	List<Content> list = DemoTool.getInstance().trade(arguments);
	        // Tool implementation
	        return new CallToolResult(list, false);
	    });
	}
	
	//--------------------------labels------------------------------
	public void updateLabelFromDB() {
		List<LabelPO> list = DataSourceUtil.getInstance().getLabelsFromDB();
		Map<String, LabelPO> map = new HashMap<String, LabelPO>();
		for(int i = 0; i < list.size(); i++) {
			LabelPO po = list.get(i);
			String labelName = po.getName();
			if(labelName != null) {
				map.put(labelName.trim(), po);
			}
		}
		labelMap = map;
	}
	
	public LabelPO getLabelPO(String labelName) {
		if(labelName != null) {
			return labelMap.get(labelName.trim());
		}else {
			return null;
		}
	}
	

	public static class PromptDesc{
		String name;
		String description;
		public PromptDesc(String name, String description) {
			super();
			this.name = name;
			this.description = description;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		@Override
		public String toString() {
			return JSONUtil.toJSONString(this);
		}
	}
	
	public static void main(String[] args) throws Exception {
		for(int i = 0; i < 2; i++) {
			String returnClass = """
		        	package com.example;
		        	import java.util.Optional;
					import javax.validation.constraints.Pattern;
					import com.fasterxml.jackson.annotation.JsonProperty;
					import dev.langchain4j.model.chat.request.json.JsonSchema;
					import dev.langchain4j.model.output.structured.Description;
					import dev.langchain4j.service.output.ServiceOutputParser;
				    public class Person {
					    @Description("姓名，必需字段。")
					    @JsonProperty(required=true)
					    String name;
					    
					    @Description("年龄，必需满足大于0，小于200")
					    int age;
					    
					    @Pattern(regexp="^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
					    @Description("用户邮箱，必须符合邮箱格式")
					    private String email;
					    
					    Address address;
					    
					    @Description("一个地址")
						public static class Address {
						    String street;
						    String city;
						    Contury contury;
						    
							@Description("一个国家")
							public static class Contury {
		        		        @Description("国家")
							    String name;
							}
						    
						}
					}				
						""";
				String name = "testtool";
				String description = "这是一个测试工具";
				ToolPO po = ToolPO.createToolPO(name, description, "", returnClass, "");
				ServerUtil.getInstance().toolExt(po, name);
				Class<?> returnClassAfterCompiler = ServerUtil.getInstance().getToolReturnClass(name);
				String returnDesc = "## 【约束】严格按照如下 JSON Schema 解析此工具的返回值，如果字段和返回值的描述不匹配，则说找不到:\n" + JsonSchemaUtil.getInstance().classToJsonSchemaString(returnClassAfterCompiler);
				System.out.println(returnDesc);
				//returnClass = returnClass + "  ";
		}
		
		
	}
	
}

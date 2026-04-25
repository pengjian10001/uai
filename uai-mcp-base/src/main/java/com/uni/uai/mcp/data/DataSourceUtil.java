package com.uni.uai.mcp.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.model.ChatMessagePO;
import com.uni.uai.mcp.model.LabelPO;
import com.uni.uai.mcp.model.LabelToolPO;
import com.uni.uai.mcp.model.PromptPO;
import com.uni.uai.mcp.model.ToolPO;
import com.uni.uai.mcp.server.McpExtendUtil;
import com.uni.uai.mcp.utils.context.TemplateUtil;
import com.uni.ubag.common.concurrent.ResourceResult;
import com.uni.ubag.common.constant.BaseConstant;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.data.common.CommonDataSourceService;
import com.uni.ubag.data.model.DataResourceRequest;
import com.uni.ubag.data.model.DataSourceConfig;
import com.uni.ubag.data.model.DataSourceDesc;
import com.uni.ubag.data.model.DataSourceResult;

public class DataSourceUtil {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	CommonDataSourceService service = CommonDataSourceService.getInstance();
	private static DataSourceUtil instance = new DataSourceUtil();
	public static DataSourceUtil getInstance() {
		return instance;
	}



	//-----------------------------HTTP-------------------------------

	/**
	 * 执行http
	 * @param httpDataSourceDesc http请求描述，例如：
	 	{
		    "method": "post",
		    "url": "http://odin.ubag.lianjia.com/common/text/tool_ai_tianqi",
		    "dataType":"TEXT"
		}
	 *
	 * @param arguments http的请求参数. 
	 * 		TODO 注意，arguments中包含请求参数param和heder两部分，在McpExtendUtil.getHttpDataSourceDesc方法中会提取出来。
	 * 		这是为了兼容MCP协议只能传递param。
	 * @return
	 */
	public Object exeHttp(String httpDataSourceDesc, Map<String, Object> param, Map<String, Object> header) {
		String key = "httpkey";
		String name = "httpname";
		DataSourceDesc.Http dataSourceDesc = McpExtendUtil.getHttpDataSourceDesc(httpDataSourceDesc, param, header);
		DataSourceConfig.Http dataSourceConfig = new DataSourceConfig.Http();
		dataSourceConfig.setConfigName("httpConfigName1");

		//执行http请求
		DataResourceRequest<DataSourceDesc.Http, DataSourceConfig.Http> dataResourceRequest = DataResourceRequest.Builder.getHttpDataResourceConfig(key, name, dataSourceDesc, dataSourceConfig);
		ResourceResult<DataSourceResult> result = service.getData(dataResourceRequest);
		DataSourceResult dr = result.get();
		Object httpResult = "";
		//logger.info(String.format("desc:%s;    args:%s;   result=%s", httpDataSourceDesc, arguments, JSONUtil.toJSONString(dr)));
		if(dr.isResultTag()) {
			httpResult = dr.getResult();
		}else {
			throw new RuntimeException(String.format("exeHttp resultTag为false。%s", JSONUtil.toJSONString(dr)));
		}
		return httpResult;
	}



	//-----------------------------DB-------------------------------
	/**
	 * 获取到所有Prompt
	 * @return
	 */
	public List<PromptPO> getPromptsFromDB(){
		String where = "t.state = 0";
		List<PromptPO> list = this.getPromptsFromDBByCondition(where);
		return list;
	}

	/**
	 * 获取到近期更新的Prompt
	 * @return
	 */
	public List<PromptPO> getRecentlyPromptsFromDB(){
		//state=0或1的都拉取，失效的需要在后面处理时动态剔除
		String where = "t.mtime > date_sub(now(), interval 10 second)";
		List<PromptPO> list = this.getPromptsFromDBByCondition(where);
		return list;
	}

	private List<PromptPO> getPromptsFromDBByCondition(String where){
		String key = "getPrompts";
		String name = "获取Prompt列表";
		var sql = """
				select
					t.id as id, t.model as model, t.type as type,
					t.name as name, t.description as description, t.param_schema as paramSchema,
					t.prompt_template as promptTemplate, t.prompt_config as promptConfig,
					t.version as version, t.state as state, t.mtime as mtime
				from
					mcp.t_prompt t
				where
					%s
				limit 1000
				""";
		sql = String.format(sql, where);
		List<PromptPO> list = this.getFromDB(key, name, sql, null, PromptPO.class);
		return list;
	}

	/**
	 * 获取到所有Tools
	 * @return
	 */
	public List<ToolPO> getToolsFromDB(){
		String where = "t.state = 0";
		List<ToolPO> list = this.getToolsFromDBByCondition(where);
		return list;
	}

	/**
	 * 获取到近期更新的tools
	 * @return
	 */
	public List<ToolPO> getRecentlyToolsFromDB(){
		//state=0或1的都拉取，失效的需要在后面处理时动态剔除
		String where = "t.mtime > date_sub(now(), interval 10 second)";
		List<ToolPO> list = this.getToolsFromDBByCondition(where);
		return list;
	}

	private List<ToolPO> getToolsFromDBByCondition(String where){
		String key = "getTools";
		String name = "获取工具列表";
		var sql = """
				select
					t.id as id, t.model as model, t.type as type,
					t.name as name, t.description as description, t.param_schema as paramSchema,
					t.return_class as returnClass, t.return_script as returnScript,
					t.datasource_config as dataSourceConfig, t.datasource_desc as dataSourceDesc,
					t.version as version, t.state as state, t.mtime as mtime
				from
					mcp.t_tool t
				where
					%s
				limit 1000
				""";
		sql = String.format(sql, where);
		List<ToolPO> list = this.getFromDB(key, name, sql, null, ToolPO.class);
		return list;
	}
	
	//插入Tool
	public Object insertToolToDB(String model, int type, String form_name, 
			String description, String param_schema, 
			String return_class , String return_script, String datasource_desc, 
			String datasource_config, int state, String version){
		String key = "inserttool";
		String name = "插入Tool";
		var sql = "insert into mcp.t_tool (`model`,`type`,`name`,`description`,`param_schema`,`return_class`,`return_script`,`datasource_desc`,`datasource_config`,`state`,`version`) values (?,?,?,?,?,?,?,?,?,?,?)";
		Object[] params = new Object[11];
		params[0] = model;
		params[1] = type;
		params[2] = form_name;
		params[3] = description;
		params[4] = param_schema;
		params[5] = param_schema;
		params[6] = param_schema;
		params[7] = datasource_desc;
		params[8] = datasource_config;
		params[9] = state;
		params[10] = version;
		Object result = this.exeDB(key, name, sql, params, BaseConstant.DbMethod.insert.toString());		
		return result;
	}
	
	public List<LabelPO> getLabelsFromDB(){
		String key = "getLabels";
		String name = "获取Labels列表";
		var sql = """
				select
					t.id as id, t.type as type,
					t.name as name, t.description as description, t.ext as ext,
					t.parent_id as parentId,
					t.state as state, t.mtime as mtime
				from
					mcp.t_label t
				limit 1000
				""";
		List<LabelPO> list = this.getFromDB(key, name, sql, null, LabelPO.class);
		return list;
	}


	public List<LabelToolPO> getLabelToolsFromDB(){
		String key = "getTools";
		String name = "获取标签和工具对应关系";
		var sql = """
				select
					tll.id as id,
					tlab.id as labelId,
					tlab.name as labelName,
					tt.id as toolId,
					tt.name as toolName,
					tll.state as state,
					tll.mtime  as mtime
				FROM mcp.t_label tlab
				INNER JOIN mcp.t_label_tool tll ON tlab.id = tll.label_id
				INNER JOIN mcp.t_tool tt  ON tll.tool_id  = tt.id
				where tll.state in (0)
				limit 5000;
				""";
		List<LabelToolPO> list = this.getFromDB(key, name, sql, null, LabelToolPO.class);
		return list;
	}
	
	//按时间倒叙取maxMessage条记录，并按照时间正序返回
	public List<ChatMessagePO> getChatMessagesFromDB(String sessionId, int maxMessage){
		String key = "getChatMessages";
		String name = "获取ChatMessage";
		/**如果要排除：type为AiMessage 且 content中不含toolExecutionRequests的记录
		 * 可在where中加入
		 * and t.type != 'ToolExecutionResultMessage'
           and not (t.type = 'AiMessage' and JSON_EXTRACT(t.content, '$.toolExecutionRequests') IS not NULL)
		 */
		var sql = String.format("""
			select * 
			from (
				select
					t.id as id, t.type as type,t.content as content,
					t.session_id as sessionId, t.single_id as singleId,
					t.state as state, t.mtime as mtime
				from
					mcp.t_chat_message t
				where t.session_id = '%s' and t.state = 0
				order by mtime desc
				limit %s
				) as tt
			order by mtime asc
			""", sessionId, maxMessage);
		List<ChatMessagePO> list = this.getFromDB(key, name, sql, null, ChatMessagePO.class);
		return list;
	}
	
	public Object deleteChatMessagesFromDB(String sessionId){
		String key = "deleteChatMessages";
		String name = "删除ChatMessage";
		var sql = """
				delete 
				from
					mcp.t_chat_message t
				where session_id = ?
				""";
		Object[] params = new Object[1];
		params[0] = sessionId;
		Object obj = this.exeDB(key, name, sql, params, BaseConstant.DbMethod.delete.toString());
		return obj;
	}
	
	public Object updateChatMessagesState(Long id){
		String key = "updateChatMessagesState";
		String name = "更新ChatMessage的state为失效";
		var sql = """
				update mcp.t_chat_message
				set state = 1
				where id = ?
				""";
		Object[] params = new Object[1];
		params[0] = id;
		Object obj = this.exeDB(key, name, sql, params, BaseConstant.DbMethod.delete.toString());
		return obj;
	}
	
	//for循环插入
	public Object insertChatMessageToDB(List<ChatMessagePO> value){
		if(value == null || value.size() ==0) {
			return null;
		}
		List<ChatMessagePO> list = new ArrayList<ChatMessagePO>();
		for(int i = 0; i < value.size(); i++) {
			ChatMessagePO po = value.get(i);
			if(po == null) {
				continue;
			}
			//均判断非空，避免数据库插入失败
			if(po.getSessionId() != null && po.getSingleId() != null 
					&& po.getType() != null && po.getContent() != null) {
				list.add(po);
			}else {
				System.out.println("insert chatmessage时，po数据不完成，部分字段为空，导致没有更新到数据库，po：" + JSONUtil.toJSONString(po));
			}
		}
		
		List<Object> result = new ArrayList<>();
		for(int i = 0; i < list.size(); i++) {
			ChatMessagePO po = list.get(i);
			String key = "insertchatmessage";
			String name = "插入ChatMessage";
			var sql = "insert into mcp.t_chat_message (`session_id`,`single_id`,`type`,`content`) values (?,?,?,?)";
			Object[] params = new Object[4];
			params[0] = po.getSessionId();
			params[1] = po.getSingleId();
			params[2] = po.getType();
			params[3] = po.getContent();
			Object obj = this.exeDB(key, name, sql, params, BaseConstant.DbMethod.insert.toString());
			result.add(obj);
		}
		return result;
	}
	
	//批量插入
	public Object batchInsertChatMessageToDB(List<ChatMessagePO> value){
		if(value == null || value.size() ==0) {
			return null;
		}
		List<ChatMessagePO> list = new ArrayList<ChatMessagePO>();
		for(int i = 0; i < value.size(); i++) {
			ChatMessagePO po = value.get(i);
			if(po == null) {
				continue;
			}
			//均判断非空，避免数据库插入失败
			if(po.getSessionId() != null && po.getSingleId() != null 
					&& po.getType() != null && po.getContent() != null) {
				list.add(po);
			}
		}
		String key = "batchinsertchatmessage";
		String name = "批量插入ChatMessage";
		var sql = this.getChatMessageInsertSql(list);
		Object[] params = new Object[0];
		Object obj = this.exeDB(key, name, sql, params, BaseConstant.DbMethod.batchInsert.toString());
		return obj;
	}

	/**
	 * 构建查询的db desc
	 * @param sql
	 * @return
	 */
	private DataSourceDesc.DB buildDataSourceDescDB(String sql, Object[] param, String method){
		DataSourceDesc.DB dataSourceDesc = new DataSourceDesc.DB();
		dataSourceDesc.setSql(sql);
		dataSourceDesc.setParam(param);
		dataSourceDesc.setMethod(method);
		//dataSourceDesc.setParams(1);
		return dataSourceDesc;
	}

	/**
	 * 构建存储mcp配置的db config
	 * @return
	 */
	private DataSourceConfig.DB buildDataSourceConfigDB(){
		DataSourceConfig.DB dataSourceConfig = DataConfig.getInstance().getDefaultDbConfig();
		//logger.info("mysql datasource url: " + dataSourceConfig.getConfigValue());
		return dataSourceConfig;
	}
	
	private <T> List<T> getFromDB(String key, String name, String sql, Object[] param, Class<T> clazz){
		DataSourceDesc.DB dataSourceDesc = this.buildDataSourceDescDB(sql, param, BaseConstant.DbMethod.query.toString());
		DataSourceConfig.DB dataSourceConfig = this.buildDataSourceConfigDB();
		DataResourceRequest<DataSourceDesc.DB, DataSourceConfig.DB> dataResourceRequest = DataResourceRequest.Builder.getDBDataResourceConfig(key, name, dataSourceDesc, dataSourceConfig);
		ResourceResult<DataSourceResult> result = service.getData(dataResourceRequest);
		DataSourceResult dr = result.get();
		List<T> list = new ArrayList<T>();
		if(dr.isResultTag()) {
			JSONArray jsonarray = JSONUtil.toJsonArray(dr.getResult());
			if(jsonarray != null && jsonarray.size() > 0) {
				for(int i = 0; i < jsonarray.size(); i++) {
					JSONObject obj = jsonarray.getJSONObject(i);
					T t = JSONUtil.parseObject(obj.toString(), clazz);
					list.add(t);
				}
			}
		}else {
			throw new RuntimeException(String.format("getFromDB resultTag为false。key=%s, name=%s, result=%s", key, name, JSONUtil.toJSONString(dr)));
		}
		return list;
	}
	
	/**
	 * 在默认的数据库上执行db操作
	 * @param key
	 * @param name
	 * @param sql
	 * @param method
	 * @return
	 */
	public Object exeDB(String key, String name, String sql, Object queryparam, String method){
		DataSourceDesc.DB dataSourceDesc = this.buildDataSourceDescDB(sql, this.getObjects(queryparam), method);
		DataSourceConfig.DB dataSourceConfig = this.buildDataSourceConfigDB();
		DataResourceRequest<DataSourceDesc.DB, DataSourceConfig.DB> dataResourceRequest = DataResourceRequest.Builder.getDBDataResourceConfig(key, name, dataSourceDesc, dataSourceConfig);
		ResourceResult<DataSourceResult> result = service.getData(dataResourceRequest);
		DataSourceResult dr = result.get();
		if(dr.isResultTag()) {
			return dr.getResult();
		}else {
			throw new RuntimeException(String.format("exeDB resultTag为false。key=%s, name=%s, result=%s", key, name, JSONUtil.toJSONString(dr)));
		}
	}
	
	public String getChatMessageInsertSql(List<ChatMessagePO> array){
		if(array.size()==0){
			throw new RuntimeException("getChatMessageInsertSql时，获取array 为空");
		}
		StringBuffer sql = new StringBuffer("insert into mcp.t_chat_message (`session_id`,`single_id`,`type`,`content`) values ");
		for(int i = 0; i<array.size(); i++){
			ChatMessagePO po = (ChatMessagePO)(array.get(i));
			String s = String.format("('%s','%s','%s','%s')", 
					this.escapeSql(po.getSessionId()),
					this.escapeSql(po.getSingleId()),
					po.getType(),
					this.escapeSql(po.getContent()));
			sql.append(s);
			if(i!=array.size()-1){
				sql.append(",");
			}
		}
		return sql.toString();
	}
	
	public String escapeSql(String str){
		String s = TemplateUtil.instance.escapeSql(str);
		/**
			由于insert语句，使用''包裹，所以，如果字符串中，\或'成对出现时，没有问题，例如下面insert中给`key`传递值的部分：''\\'' or like''，没有问题。
			insert into merlin.common_log (`project`,`type`,`key`,`value`,`ex`,`tag`,`time`,`result`,`server`, `otime`, `logId`) 
			values 
			(7,18,'''\\'' or like''','','',0,1015,'','10.200.20.48',1660183771031,'63132996780685')
			但如果key的值为下列3种情况，都会导致sql异常
				1）''\\\'' or like''  或 ''\'' or like''（由于\是单数，转移了后面一个'，导致'为单数）  
				2）''\\''' or like''（'为单数），
				3）末尾只有一个\或'，即，如果末尾两个字符是\\或''时，没有问题，即'******\\'或'******'''，但是如果是单个\或'，则会导致非法sql，例如'******\'，'******''，
		*/
		//TODO 对于1）和2），由于TemplateUtil.instance.escapeSql(str)已经将当个'替换为两个'，所以，s中'的数量肯定为双数，所以需要处理\的逻辑
		
		//对于情况3），处理方案是，判断末尾字符，把末尾的\或'都删除
		int end = s.length();
		for(int i = end-1; i >0; i--){
			if(s.charAt(i)=='\\' 
					|| s.charAt(i)=='\''){
				end--;
				continue;
			}else{
				break;
			}
		}
		if(end == s.length()){
			return s;
		}else{
			return s.substring(0, end);
		}
	}
	
	private Object[] getObjects(Object param) {
		if(param == null) {
			return null;
		}
        try {
            JSONArray array = JSONUtil.toJsonArray(param);
            Object[] objs = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                objs[i] = array.get(i);
            }
            return objs;
        } catch (Exception e) {
            throw new RuntimeException(String.format("%s解析为Object[]时异常", param),e);
        }
    }
}

package com.uni.uai.mcp.utils;

import java.util.HashMap;
import java.util.Map;

import com.uni.uai.mcp.client.McpClientUtil;
import com.uni.uai.mcp.client.McpStreamClientUtil;
import com.uni.uai.mcp.data.DataConfig;
import com.uni.uai.mcp.data.DataSourceUtil;
import com.uni.uai.mcp.server.McpExtendUtil;
import com.uni.uai.mcp.utils.context.TemplateExtendUtil;
import com.uni.uai.mcp.utils.context.TemplateUtil;
import com.uni.uai.rag.embedding.EmbeddingUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.JSONPathUtil;
import com.uni.ubag.data.db.sqlite.JsonSqlUtil;
import com.uni.ubag.data.db.sqlite.JsonSqlUtil2;
import com.uni.ubag.util.template.TemplateCollectionUtil;

public class ConfigTemplateUtils {
	private static Logger logger = LoggerFactory.getLogger(ConfigTemplateUtils.class);
    /**
     *
     * @param systemContext 系统上下文
     * @param contextMap 解析上下文
     * @param text
     * @return
     */
    public static String parse(Map<String, Object> contextMap, String text) {
    	Map<String, Object> context = new HashMap<String, Object>();
        Map<String, Object> params = UbagConf.getRequestConf();
        if (params != null) {
            context.putAll(params);
        }
        //从方法传入的上下文优先于threadlocal的param
        if (contextMap != null) {
            context.putAll(contextMap);
        }
        addDefaultSystemContext(context);

        String result = FreeMarkerTemplate.parse(context, text);
        return result;
    }

    private static JsonSqlUtil jsonSqlUtil = new JsonSqlUtil();
    private static void addDefaultSystemContext(Map<String, Object> context){
        //=====json相关
        //jsonpath工具
        context.put("jsonpath", JSONPathUtil.getInstance());
        context.put("json", JSONUtil.getInstance());
        //不能用JsonSqlUtil.getInstance()，否则报错找不到代理类
        context.put("sql", jsonSqlUtil);
        context.put("sql2", JsonSqlUtil2.getInstance());
        
        context.put("c", TemplateCollectionUtil.getInstance());
        context.put("t", TemplateUtil.getInstance());
        context.put("te", TemplateExtendUtil.getInstance());
        context.put("mcpcli", McpClientUtil.getInstance());
        context.put("mcpstmcli", McpStreamClientUtil.getInstance());
        
        context.put("ds", DataSourceUtil.getInstance());
        
        context.put("embed", EmbeddingUtil.getInstance());
    }

}

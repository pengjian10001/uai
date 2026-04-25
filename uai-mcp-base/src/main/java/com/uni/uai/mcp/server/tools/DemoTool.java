package com.uni.uai.mcp.server.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Content;

public class DemoTool {
	private static DemoTool instance = new DemoTool();
	public static DemoTool getInstance() {
		return instance;
	}
	/**
	 * 一个测试工具
	 * @param arguments
	 * @return
	 */
	public List<Content> trade(Map<String, Object> arguments){
		List<Content> list = new ArrayList<Content>();
		String location = (String) arguments.get("location");
		Integer tradeMoney = null;
		if("北京".equals(location)) {
			tradeMoney = 11202;
		}else {
			tradeMoney = 8900;
		}
		list.add(new McpSchema.TextContent(location + "交易额为 " + tradeMoney + " 亿元"));
    	return list;
	}
	
}

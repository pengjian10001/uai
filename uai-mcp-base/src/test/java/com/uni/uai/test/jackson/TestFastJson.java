package com.uni.uai.test.jackson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

public class TestFastJson {

	public static void main(String[] args) {
		String json = """
				{
					"toolExecutionRequests": [{
						"id": "call_GzuqMd4ugOn8cUfu9Ex2MF5g",
						"name": "_mcpServerInfo",
						"arguments": '{
							"queryText": "工具能力"
						}'
					}],
					"type": "AI"
				}
				""";
		
		System.out.println(json);
		AiMessage obj = null;
		//JSON.parseObject(json, AiMessage.class, JSONReader.Feature.AllowUnQuotedFieldNames);
		//System.out.println(obj);
		
		json = JSON.toJSONString(obj, JSONWriter.Feature.FieldBased);
		System.out.println(json);

	}

}

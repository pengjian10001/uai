package dev.langchain4j.mcp.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.uni.uai.mcp.server.McpExtendUtil;

import dev.langchain4j.model.chat.request.json.JsonSchemaElement;

public class MyMcpClientUtil {
	/**
	 * 此方法是langchain4j调用mcp server的tools/list时，会将响应转换为其内部结构，如果结构不对，则会报错
	 * 所以，在mcp server端在添加tool时，可以调用此方法，验证schema，如果报错，则不添加
	 * @param array
	 * @return
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 */
	public static JsonSchemaElement checkSchema(String schema) throws JsonMappingException, JsonProcessingException {
		try {
			JsonNode node = McpExtendUtil.objectMapper.readTree(schema);
			JsonSchemaElement ele = ToolSpecificationHelper.jsonNodeToJsonSchemaElement(node);
			return ele;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(String.format("MyMcpClientUtil.checkSchema异常, schema=%s, e=%s", schema, e.getClass() + "-" + e.getMessage()));
		}
	}
}

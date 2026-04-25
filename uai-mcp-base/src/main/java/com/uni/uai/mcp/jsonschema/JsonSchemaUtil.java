package com.uni.uai.mcp.jsonschema;

import java.util.Optional;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.service.output.ServiceOutputParser;

public class JsonSchemaUtil {
	public ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
	private static JsonSchemaUtil instance = new JsonSchemaUtil();
	
	public static JsonSchemaUtil getInstance() {
		return instance;
	}
	
	public JsonSchema classToJsonSchema(Class<?> clazz){
		Optional<JsonSchema> option = serviceOutputParser.jsonSchema(clazz);
		if(option.isPresent()) {
			return option.get();
		}else {
			return null;
		}
	}
	
	public String classToJsonSchemaString(Class<?> clazz){
		String str = serviceOutputParser.outputFormatInstructions(clazz);
		/**
		 此处str返回的jsonschema前面包含一段文本：
			You must answer strictly in the following JSON format: {
			"name": (姓名，必需字段。; type: string),
			"age": (年龄，必需满足大于0，小于200; type: integer),
			"email": (用户邮箱，必须符合邮箱格式，即必需符合正则表达式'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$'.; type: string),
			"address": (一个地址字段; type: com.uni.uai.test.jsonschema.JsonSchemaUtilTest$Person$Address: {
			"street": (type: string),
			"city": (type: string)
			})
			}
			
		  将这个文本去掉，即去掉"{"前面的所有字符
		 */
		if(str!=null) {
			str = str.replaceAll("^[^{]*", "");
		}
		return str;
	}
	
	
	public static void main(String[] args) {
		
	}
}

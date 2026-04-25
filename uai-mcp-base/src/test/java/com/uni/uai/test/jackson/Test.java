package com.uni.uai.test.jackson;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.uni.ubag.common.util.JSONUtil;


public class Test {

	// 自定义反序列化器：将 JSON 对象转为字符串
	public static class JsonObjectToStringDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
		@Override
	    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
	        ObjectMapper mapper = (ObjectMapper) p.getCodec();
	        TreeNode node = mapper.readTree(p);
	        return mapper.writeValueAsString(node); // 将 JSON 对象转为字符串
	    }
	}
	
    // 自定义序列化器：将字符串当作 JSON 对象处理
    public static class StringToJsonObjectSerializer extends com.fasterxml.jackson.databind.JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(value, Object.class);
            gen.writeObject(json);
        }
    }
    
    private static abstract class AiMessageMixin{
    	@JsonCreator
    	public AiMessageMixin(
    	@JsonProperty("type")String type,
    	@JsonProperty("toolExecutionRequests")List<ToolExecutionRequest> toolExecutionRequests
    	){
    	}

    }
    
    private static abstract class ToolExecutionRequestMixin{
    	@JsonCreator
    	public ToolExecutionRequestMixin(
    	@JsonProperty("id")String id,
    	@JsonProperty("name")String name,
    	@JsonProperty("arguments")
    	@JsonDeserialize(using = JsonObjectToStringDeserializer.class) 
        @JsonSerialize(using = StringToJsonObjectSerializer.class)
    	String arguments
    	){
    	}

    }

	
	public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
		String json = """
				{
					"toolExecutionRequests": [{
						"id": "call_GzuqMd4ugOn8cUfu9Ex2MF5g",
						"name": "_mcpServerInfo",
						"arguments": {
							"queryText": "工具能力"
						}
					}],
					"type": "AI"
				}
				""";
		// 注册 Mixin 到 ObjectMapper
		ObjectMapper mapper = JsonMapper.builder()
	            .visibility(FIELD, ANY)
	            .addMixIn(AiMessage.class, AiMessageMixin.class)
	            .addMixIn(ToolExecutionRequest.class, ToolExecutionRequestMixin.class)
	            .build();
		
		
		System.out.println(json);
		AiMessage obj = mapper.readValue(json, AiMessage.class);
		json = mapper.writeValueAsString(obj);
		System.out.println(json);
		
		obj = mapper.readValue(json, AiMessage.class);
		json = mapper.writeValueAsString(obj);
		System.out.println(json);
		 

	}

}

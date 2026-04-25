package com.uni.uai.test.jackson;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;

import java.io.IOException;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;


public class Test2 {
	public static class EscapedStringSerializer extends JsonSerializer<String> {
	    @Override
	    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
	        if (value == null) {
	            gen.writeNull();
	            return;
	        }
	        // 使用 escapeJson 确保字符串可安全放入 JSON 双引号中
	        String escapedValue = StringEscapeUtils.escapeJson(value);
	        gen.writeString(escapedValue);
	    }
	}
    
	public static class EscapedStringDeserializer extends JsonDeserializer<String> {
	    @Override
	    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
	        String value = p.getValueAsString();
	        if (value == null) {
	            return null;
	        }
	        // 使用 unescapeJson 还原转义字符
	        return StringEscapeUtils.unescapeJson(value);
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
    	@JsonDeserialize(using = EscapedStringDeserializer.class) 
        @JsonSerialize(using = EscapedStringSerializer.class)
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
                        "arguments": "{\"queryText\": \"工具能力\"}"
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

package com.uni.uai.test.jsonschema;

import java.util.Optional;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uni.uai.mcp.jsonschema.JsonSchemaUtil;

import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.output.ServiceOutputParser;

public class JsonSchemaUtilTest {
	public static ServiceOutputParser serviceOutputParser = new ServiceOutputParser();

	@Description("一个人。")
	public static class Person {
	    @Description("姓名，必需字段。") // 加上必需的约束
	    @JsonProperty(required=true)
	    String name;
	    @Description("年龄，必需满足大于0，小于200") //加上大小的约束
	    int age;
	    @Pattern(regexp="")
	    @Description("用户邮箱，必须符合邮箱格式，即必需符合正则表达式'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$'.") //加上格式的约束
	    private String email;
	    @Description("一个地址字段")
	    Address address;
	    
	    @Description("一个地址对象") // 你可以添加一个可选的描述，以帮助LLM更好地理解
		public static class Address {
		    String street;
		    String city;
		}

	}
	
	
	
	public static void main(String[] args) {
		JsonSchema jsonSchema = JsonSchemaUtil.getInstance().classToJsonSchema(Person.class);
		System.out.println(jsonSchema);
		
		String str = JsonSchemaUtil.getInstance().classToJsonSchemaString(Person.class);
		System.out.println(str);
		
	}
}

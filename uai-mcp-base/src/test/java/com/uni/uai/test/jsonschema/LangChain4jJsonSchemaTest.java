package com.uni.uai.test.jsonschema;

import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.service.output.ServiceOutputParser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.uni.uai.mcp.utils.complier.SafeDynamicCompiler;

public class LangChain4jJsonSchemaTest {
	public static ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
    public static void main(String[] args) throws Exception {
    	// 1. 创建动态编译器实例
    	SafeDynamicCompiler compiler = new SafeDynamicCompiler();
        
        // 生成类名和源代码
        String className = "com.example.Person";
        String sourceCode = """
        	package com.example;
        	import java.util.Optional;
			import javax.validation.constraints.Pattern;
			import com.fasterxml.jackson.annotation.JsonProperty;
			import dev.langchain4j.model.chat.request.json.JsonSchema;
			import dev.langchain4j.model.output.structured.Description;
			import dev.langchain4j.service.output.ServiceOutputParser;
		    public class Person {
			    @Description("姓名，必需字段。") // 加上必需的约束
			    @JsonProperty(required=true)
			    String name;
			    @Description("年龄，必需满足大于0，小于200") //加上大小的约束
			    int age;
			    @Pattern(regexp="")
			    @Description("用户邮箱，必须符合邮箱格式，即必需符合正则表达式'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$'.") //加上格式的约束
			    private String email;
			    Address address;
			    
			    @Description("一个地址") // 你可以添加一个可选的描述，以帮助LLM更好地理解
				public static class Address {
				    String street;
				    String city;
				}
		
			}	
        		""";
        
        // 编译并加载类
        Class<?> customerClass = compiler.compileAndLoad(className, sourceCode);
        
        System.out.println(customerClass);
        
        Optional<JsonSchema> jsonSchema = serviceOutputParser.jsonSchema(customerClass);
        System.out.println(jsonSchema);
		
		String str = serviceOutputParser.outputFormatInstructions(customerClass);
		System.out.println(str);
    }
}

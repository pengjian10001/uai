package com.uni.uai.test.jsonschema;

import dev.langchain4j.model.output.structured.Description;

@Description("用户信息")
public class TestUcResultBean {
	@Description("接口成功或失败，1为成功，其他为失败")
	int code;
	@Description("数据对象")
	Data data;
    
    @Description("数据对象") // 你可以添加一个可选的描述，以帮助LLM更好地理解
	public static class Data {
    	@Description("结果对象")
	    R r;
	    
	    public static class R {
	    	@Description("用户code")
		    String usercode;
		    String city;
		}
	}

}

package com.uni.uai.test.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.uni.uai.test.jackson.Test.JsonObjectToStringDeserializer;
import com.uni.uai.test.jackson.Test.StringToJsonObjectSerializer;

public class ToolExecutionRequest {
	private final String id;
    private final String name;
    private final String arguments;
	public ToolExecutionRequest(
			String id, 
			String name, 
			String arguments) {
		this.id = id;
		this.name = name;
		this.arguments = arguments;
	}
}

package com.uni.uai.test.jackson;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AiMessage {
    private final String type;
    private final List<ToolExecutionRequest> toolExecutionRequests;
    public AiMessage(
    		String type, 
    		List<ToolExecutionRequest> toolExecutionRequests) {
        this.type = type;
        this.toolExecutionRequests = toolExecutionRequests; 
    }
}

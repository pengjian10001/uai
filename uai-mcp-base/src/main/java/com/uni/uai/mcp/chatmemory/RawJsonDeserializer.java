package com.uni.uai.mcp.chatmemory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class RawJsonDeserializer extends StdDeserializer<String> {
    public RawJsonDeserializer() {
        super(String.class);
    }
    
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return new ObjectMapper().writeValueAsString(node);
    }
}

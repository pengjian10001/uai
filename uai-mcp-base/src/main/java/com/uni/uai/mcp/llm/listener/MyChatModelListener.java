package com.uni.uai.mcp.llm.listener;

import org.json.JSONObject;

import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.log.util.UbagLogUtil;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;

public class MyChatModelListener implements ChatModelListener{
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatResponse chatResponse = responseContext.chatResponse();
        
        String markKey = "listener onResponse";
    	JSONObject result = new JSONObject();

        ChatResponseMetadata metadata = chatResponse.metadata();
        result.put("modelName", metadata.modelName());
        result.put("finishReason", metadata.finishReason());
        
        TokenUsage tokenUsage = metadata.tokenUsage();
        result.put("inputTokenCount", tokenUsage.inputTokenCount());
        result.put("outputTokenCount", tokenUsage.outputTokenCount());
        result.put("totalTokenCount", tokenUsage.totalTokenCount());
        
        result.put("aiMessage", chatResponse.aiMessage());

        ChatRequest chatRequest = responseContext.chatRequest();
        JSONObject value = new JSONObject();
        value.put("chatRequest", chatRequest);
        
        TimeTrace.markCall(markKey, 0L, 0L);
        UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), markKey, value.toString(), null, true, 0L, result.toString());
        System.out.println(UbagConf.getlogId());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
    	String markKey = "listener onError";
        Throwable error = errorContext.error();

        ChatRequest chatRequest = errorContext.chatRequest();
        JSONObject result = new JSONObject();
        result.put("chatRequest", chatRequest);
        
        TimeTrace.markCall(markKey, 0L, 1L);
        UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), markKey, ExceptionUtil.toStackTrace(error), null, false, 0L, result.toString());
        System.out.println(UbagConf.getlogId());
    }

}

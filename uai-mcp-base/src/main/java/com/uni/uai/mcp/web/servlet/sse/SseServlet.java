package com.uni.uai.mcp.web.servlet.sse;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.ubag.common.util.FileUtil;

public class SseServlet extends SseBaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void handle(HttpServletRequest request) throws Exception{
		//业务处理
    	//TemplateSseUtil.getInstance().test(lastEventId);
    	//TemplateSseUtil.getInstance().testChat("预测下月1日的天气与公交拥堵情况的关系，并显示推理过程");
    	
    	String requestURI = request.getRequestURI();
    	String fileContent = FileUtil.getInstance().getResourceContent(requestURI);
		//WebFilter中，会将请求参数放入到_param中。
		Map<String, Object> context = HttpServletUtil.getInstance().handleParam(request);
		Map<String, Object> contextPostJson = HttpServletUtil.getInstance().handlePostJsonParam(request);
		context.putAll(contextPostJson);
		//sse不需要返回值，在业务中通过out输出
		ConfigTemplateUtils.parse(context, fileContent);
		
	}
}

package com.uni.uai.mcp.web.servlet.sse;

import java.io.*;
import java.util.*;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.FileUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UploadSseServlet extends SseBaseServlet {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
    // 临时文件存储路径，实际项目中建议使用配置文件配置
    public static final String UPLOAD_TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "ai_chat_uploads";
    public static final long maxFileSize = 30 * 1024 * 1024; // 单个文件最大30MB
    public static final long maxRequestSize = 100 * 1024 * 1024; // 请求总大小最大100MB
    public static final  int fileSizeThreshold = 1 * 1024 * 1024; // 1MB缓冲区（文件大小超过此值则写入临时文件）

    @Override
    public void init() throws ServletException {
        super.init();
        // 初始化上传目录
        File tempDir = new File(UPLOAD_TEMP_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = resp.getWriter();
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("message", "不支持GET请求，请使用POST");
        writer.write(result.toString());
    }

	@Override
	public void handle(HttpServletRequest request) throws Exception {
		String requestURI = request.getRequestURI();
		String fileContent = FileUtil.getInstance().getResourceContent(requestURI);
        
        // 1. 处理普通表单参数
		//WebFilter中，会将请求参数放入到_param中。
		Map<String, Object> context = HttpServletUtil.getInstance().handleParam(request);
		Map<String, Object> contextPostJson = HttpServletUtil.getInstance().handlePostJsonParam(request);
		context.putAll(contextPostJson);
		
        // 2. 处理文件上传
        Map<String, Object> contextPostFile = HttpServletUtil.getInstance().handlePostFileParam(request, UPLOAD_TEMP_DIR);
        context.putAll(contextPostFile);
        logger.info(String.format("收到AI对话请求: context=%s", context));
        
        // 3. 调用AI服务处理消息（此处为示例，实际需替换为真实AI服务调用）
        ConfigTemplateUtils.parse(context, fileContent);
		
	}
    

}

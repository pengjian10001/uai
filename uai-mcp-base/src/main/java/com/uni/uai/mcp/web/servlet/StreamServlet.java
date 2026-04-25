package com.uni.uai.mcp.web.servlet;

import java.io.*;
import java.util.*;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.uai.mcp.utils.JSONUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.FileUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.common.util.TimeUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StreamServlet extends HttpServlet {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long serialVersionUID = 1L;
    public static final String UTF_8 = "UTF-8";

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        String markKey = requestURI;
        boolean isMark = false;
        long start = System.currentTimeMillis();
        Throwable ee = null;
        Boolean issuccess = true;
        String result = "";

        try {
            // 记录调用堆栈
            isMark = TimeTrace.markStart(markKey);
            resp.setCharacterEncoding(UTF_8);

            String fileContent = FileUtil.getInstance().getResourceContent(requestURI);
            
            // 1. 处理普通表单参数
			//WebFilter中，会将请求参数放入到_param中。
			Map<String, Object> context = HttpServletUtil.getInstance().handleParam(req);
			Map<String, Object> contextPostJson = HttpServletUtil.getInstance().handlePostJsonParam(req);
			context.putAll(contextPostJson);
			
            // 2. 处理文件上传
            Map<String, Object> contextPostFile = HttpServletUtil.getInstance().handlePostFileParam(req, UPLOAD_TEMP_DIR);
            context.putAll(contextPostFile);
            logger.info(String.format("收到AI对话请求: context=%s", context));
            
            // 3. 调用AI服务处理消息（此处为示例，实际需替换为真实AI服务调用）
            result = ConfigTemplateUtils.parse(context, fileContent);

            // 4. 设置响应
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = resp.getWriter();
            writer.write(result);
            writer.flush();
            result = "处理成功";
        } catch (Exception e) {
        	Long time = TimeUtil.currentTimeMillis() - start;
			issuccess = false;
			ee = e;
			String msg = String.format("%s异常, e=%s", requestURI, ee.getClass()+ "-" + ee.getMessage());
			logger.warn(msg, e);
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), ee.getClass().getName(), ExceptionUtil.toStackTrace(ee), ee, issuccess, time, msg);
        } finally {
        	long time = System.currentTimeMillis()-start;
			if(isMark) {
				TimeTrace.markEnd(markKey,time, issuccess?0L:1L);
			}
			Map<String, Object> value = UbagConf.getRequestMap(UbagConf.WebConf.request_user_param, new HashMap<>());
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CONTROLLER.getCode(), requestURI, JSONUtil.getInstance().toJSONString(value), ee, issuccess, time, result);
        }
    }


}

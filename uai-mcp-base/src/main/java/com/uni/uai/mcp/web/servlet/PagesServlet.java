package com.uni.uai.mcp.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

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

public class PagesServlet extends HttpServlet {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final long serialVersionUID = 1L;
	public static final String UTF_8 = "UTF-8";
	//url前缀
	public static final String URL_PREFIX = "/pages";
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.service(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.service(req, resp);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestURI = req.getRequestURI();
		String markKey = requestURI;
		boolean isMark = false;
		long start = System.currentTimeMillis();
		Throwable ee = null;
    	Boolean issuccess = true;
    	String result = "";
		try {
			//记录调用堆栈
			isMark = TimeTrace.markStart(markKey);
			if (requestURI.startsWith(URL_PREFIX)) {
				resp.setContentType("text/html");
				resp.setCharacterEncoding(UTF_8);
				String fileContent = FileUtil.getInstance().getResourceContent(requestURI);
				//WebFilter中，会将请求参数放入到_param中。
				Map<String, Object> context = HttpServletUtil.getInstance().handleParam(req);
				Map<String, Object> contextPostJson = HttpServletUtil.getInstance().handlePostJsonParam(req);
				context.putAll(contextPostJson);
				result = ConfigTemplateUtils.parse(context, fileContent);
				PrintWriter writer = resp.getWriter();
				writer.write(result);
				writer.flush();
			}else {
				// TODO Auto-generated method stub
				super.service(req, resp);
			}
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

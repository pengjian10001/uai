package com.uni.uai.mcp.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.FileUtil;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 静态页，没有freemarker解析
 */
public class StaticServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String UTF_8 = "UTF-8";
	//url前缀
	public static final String URL_PREFIX = "/static";
	

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
		if (requestURI.startsWith(URL_PREFIX)) {
			resp.setContentType("text/html");
			resp.setCharacterEncoding(UTF_8);
			String fileContent = FileUtil.getInstance().getResourceContent(requestURI);
			//WebFilter中，会将请求参数放入到_request中。
			//Map<String, Object> context = HttpServletUtil.getInstance().handleParam(req);
			//Map<String, Object> contextPostJson = HttpServletUtil.getInstance().handlePostJsonParam(req);
			//context.putAll(contextPostJson);
			//String result = ConfigTemplateUtils.parse(context, fileContent);
			PrintWriter writer = resp.getWriter();
			writer.write(fileContent);
			writer.flush();
		}else {
			// TODO Auto-generated method stub
			super.service(req, resp);
		}
	}
	

}

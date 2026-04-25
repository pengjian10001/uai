package com.uni.uai.mcp.web.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.server.ServerUtil;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.uai.mcp.utils.ServletUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.thread.UbagThreadUtils;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.common.util.TimeUtil;
import com.uni.ubag.data.util.concurrent.UbagDataThreadUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 加上webfilter注解及@Component，用于springboot工程通过注解引入
 * 普通spring mvc通过web.xml配置
 * @author pengjian
 *
 */
@Component
@jakarta.servlet.annotation.WebFilter(urlPatterns = "/*", filterName = "uaiLogWebFilter", asyncSupported = true)
public class WebFilter implements jakarta.servlet.Filter{
	final private Logger logger = LoggerFactory.getLogger(WebFilter.class);
	private String host = null;
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
			jakarta.servlet.FilterChain arg2) throws IOException, jakarta.servlet.ServletException {
		//首先清除ThreadLocal
		//this.init();
		
		String contextPath = request.getServletContext().getRealPath("/");
		UbagConf.setRequestConf(UbagConf.WebConf.request_webapp_path, contextPath);
		long startTime = TimeUtil.currentTimeMillis();
		Throwable ex = null;
		Boolean issuccess = true;
		StringBuffer url = new StringBuffer();
		Map<String, Object> param = new JSONObject();
		boolean isEntrance = false;
		//对于贝壳，keboot项目，都会接健康检查，这部分url不打印日志
		boolean isHealthUrl = false;
		String markKey = null;
		boolean isMark = false;
		try {
			isEntrance = UbagConf.isEntrance();
			markKey = "webFilter";
			isMark = TimeTrace.markStart(markKey);
			HttpServletRequest httpServletRequest = (HttpServletRequest)request;
			url = url.append(httpServletRequest.getRequestURL().toString());
			String exclude_key_regex = UbagConf.getString(UbagConf.LogConf.log_logid_verify_exclude_key_regex, ".*/actuator/.*");
			//health日志url为/actuator/......
			if(RegexUtil.isMatch(url.toString(), exclude_key_regex)) {
				isHealthUrl = true;
			}
			this.handleLogId(httpServletRequest, url.toString());
			param = HttpServletUtil.getInstance().handleParam(httpServletRequest);
			this.setHeader(httpServletRequest);
			this.setCookie(httpServletRequest);
			//动态获取服务的url
			UbagConf.setConf(UaiConf.MCP_SERVER_URL, ServletUtil.getInstance().getServerUrl(httpServletRequest));
			UbagConf.setRequestConf(UbagConf.WebConf.request_full_url, url.toString());
			UbagConf.setRequestConf(UbagConf.WebConf.request_query_string, (httpServletRequest).getQueryString());
			String ip = this.getIPAddress(httpServletRequest);
			UbagConf.setRequestConf(UbagConf.WebConf.request_ip, ip);
			
			arg2.doFilter(request, response);
		} catch (Throwable e){
			logger.warn("webfilter. "+e.getClass()+"-"+e.getMessage(),e);
			issuccess=false;
			ex = e;
			String msg = String.format("webFilter捕获异常, methon=%s, e=%s", url, ex.getClass()+ "-" + ex.getMessage());
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "webFilter", ExceptionUtil.toStackTrace(ex), ex, false, 0L, msg);
			throw e;
		} finally{
			long time = TimeUtil.currentTimeMillis() - startTime;
			if(isMark) {
            	TimeTrace.markEnd(markKey, time, issuccess?0L:1L);
            }
			//logger.info(String.format("request %s,%s", url, this.handleParam((HttpServletRequest)request)));
			//logger.info(String.format("time %s,%s", url, time));
			//处理ubaglog
			if(!isHealthUrl) {
				UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.WEBFILTER.getCode(), url.toString(), param, ex, issuccess, time, "");
				UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.TRACE.getCode(), url.toString(), TimeTrace.getMarkTrace(), ex, issuccess, time, "");
			}
			this.init();
		}
		
	}

	private void handleLogId(HttpServletRequest request, String url){
		String logId = null;
		//默认尝试从以下几种logId获取logId的值
		//第一个是在ubag配置文件中配置的名称
		//其他是几个兼容的名称
		String[] logIdNameArr = new String[] {UbagConf.getlogIdName(), "logId","logid","log_id"};
		for(String logIdName : logIdNameArr) {
			logId = request.getParameter(logIdName);
			//TODO 对于postjson请求，此处获取不到
			if(logId == null){
				//经测试request.getHeader获取header时，是不区分大小写的，即传递logid和logId都是一样的
				logId = request.getHeader(logIdName);
			}
			if(logId != null){
				break;
			}
		}
		if(logId == null){
			logId = UbagLogUtil.getInstance().tryGetLogId(url, "web");
		}
		//if(logId == null){
		//	logId = ""+IdCreator.getInstance(UbagConf.getlogIdName()).next();
		//}
		MDC.put(UbagConf.getlogIdName(), logId);
		//TODO 对于postjson请求，此处获取不到，所以，如果获取不到，则在controller intercept中获取。而不要在此处创建logId
		UbagConf.setlogId(logId);		
	}
	
	/**
	 * 从请求中获取header
	 * @param request
	 */
	public void setHeader(HttpServletRequest request){
		JSONObject headerObj = new JSONObject();
		Enumeration<String> names = request.getHeaderNames();
		if(names!=null){
			while(names.hasMoreElements()){
				String name = names.nextElement();
				Object value = request.getHeader(name);
				if(value!=null){
					headerObj.put(name, value);
				}
			}
			UbagConf.setRequestConf(UbagConf.WebConf.request_header, headerObj.toString());
		}
	}
	/**
	 * 从请求中获取cookie
	 * @param request
	 */
	public void setCookie(HttpServletRequest request){
		JSONObject cookieObj = new JSONObject();
		Cookie[] cookies = request.getCookies();
		if(cookies!=null && cookies.length>0){
			for(Cookie c : cookies){
				String name = c.getName();
				cookieObj.put(name, this.cookie2Map(c));
			}
			UbagConf.setRequestConf(UbagConf.WebConf.request_cookie, cookieObj.toString());
		}
	}
	
	private Map<String,Object> cookie2Map(Cookie c){
		Map<String, Object> map =new HashMap<String, Object>();
		if(c!=null){
			map.put("name", c.getName());
			map.put("value", c.getValue());
			map.put("comment", c.getComment());
			map.put("domain", c.getDomain());
			map.put("maxAge", c.getMaxAge());
			map.put("path", c.getPath());
			map.put("version", c.getVersion());
			map.put("secure", c.getSecure());
		}
		return map;
	}
	
	private void init() {
		UbagThreadUtils.cleanAll();
		UbagDataThreadUtil.cleanAll();
	}
	
	//获取客户端请求真实ip地址
	public String getIPAddress(HttpServletRequest request) {
	    String ip = null;    //X-Forwarded-For：Squid 服务代理
	    String ipAddresses = request.getHeader("X-Forwarded-For");if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {        //Proxy-Client-IP：apache 服务代理
	        ipAddresses = request.getHeader("Proxy-Client-IP");
	    }if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {        //WL-Proxy-Client-IP：weblogic 服务代理
	        ipAddresses = request.getHeader("WL-Proxy-Client-IP");
	    }if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {        //HTTP_CLIENT_IP：有些代理服务器
	        ipAddresses = request.getHeader("HTTP_CLIENT_IP");
	    }if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {        //X-Real-IP：nginx服务代理
	        ipAddresses = request.getHeader("X-Real-IP");
	    }    //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
	    if (ipAddresses != null && ipAddresses.length() != 0) {
	        ip = ipAddresses.split(",")[0];
	    }    //还是不能获取到，最后再通过request.getRemoteAddr();获取
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
	        ip = request.getRemoteAddr();
	    }
	    return ip;
	}
	
}

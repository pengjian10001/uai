package com.uni.uai.mcp.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.structured.Description;
import io.modelcontextprotocol.server.McpAsyncServer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

public class ServletUtil {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static ServletUtil instance = new ServletUtil();
	//private static String servletContextKey = "ubag.uai.servlet.context";
	
	private Set<Class<?>> classSet = new HashSet<>();
	private Set<String> classpath = new HashSet<>();
	private String jarpath = null;
	
	public static ServletUtil getInstance() {
		return instance;
	}
	
	public Set<String> getClasspath() {
		/**
		 * 为了避免动态编译ai tool的returnclass时，找不到类@Description
		 /com/uai/userpackage/TestUcResultBean_test_uc.java:17: 错误: 找不到符号
			    	@Description("职位code")
			    	 ^
		  符号:   类 Description
		  位置: 类 R
		 */
		//添加Description所在jar包的classpath路径
		//如果还缺失其他类，可以在此处再增加jar包的路径
		return this.getClasspath(Description.class);
	}
	
	public Set<String> getClasspath(Class<?>... classes) {
		if(classes != null && classes.length > 0) {
			for(Class<?> clazz : classes) {
				if(!classSet.contains(clazz)) {
					String path = this.getWarPath(clazz);
					if(path!=null) {
						classpath.add(path);
						classSet.add(clazz);
					}
				}
			}
		}
		return classpath;
	}

	public String getJarpath() {
		return jarpath;
	}

	public void setServletPath(ServletContext servletContext) {
	    //jarpath = servletContext.getRealPath("/WEB-INF/lib");
	    
		jarpath = servletContext.getRealPath("/");
		if(jarpath != null) {
			jarpath = jarpath + "/WEB-INF/lib";
		}
	    
	}
	
	/**
     * 获取当前类所在的 WAR 包路径
     * @param clazz 当前类的 Class 对象
     * @return WAR 包的绝对路径，若无法获取则返回 null
     */
    private String getWarPath(Class<?> clazz) {
        try {
            // 获取类的资源 URL（格式可能为：jar:file:/path/to/your/app.war!/WEB-INF/classes/...）
            String className = clazz.getName().replace('.', '/') + ".class";
            java.net.URL url = clazz.getClassLoader().getResource(className);
            if (url == null) {
                return null;
            }
            
            String urlPath = url.toString();
            // 处理 WAR 包内资源的 URL 格式（jar:file:...!/{classPath}）
            if (urlPath.startsWith("jar:file:")) {
                // 截取 WAR 包路径（从 "file:" 后到 "!/" 前）
                int endIndex = urlPath.indexOf("!/");
                if (endIndex != -1) {
                    String warPath = urlPath.substring("jar:file:".length(), endIndex);
                    // 解码 URL 编码（如空格可能被编码为 %20）
                    return java.net.URLDecoder.decode(warPath, "UTF-8");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 通过 Servlet 容器提供的 API 动态获取服务对外开放的 IP 地址 和 端口号，无需硬编码。
     * 当需要在服务中，通过http[s]的方式访问自身服务的时，可以使用这个方法获取到自身服务的ip和端口号。
     * 例如，mcp客户端和服务端在一台机器上时，客户端调用mcp服务端（http://server:port/sse），此方法可以获取到http://server:port
     * 
     * 
     * 适用于 Tomcat、Jetty 等主流容器
     * 当有请求进入时，可通过当前请求对象 HttpServletRequest 获取服务实际对外提供服务的 IP 和端口。
     * 这种方式能准确反映客户端实际访问的地址（尤其适用于多网卡、多端口部署的场景）。
     * 
     * request.getLocalPort()：返回服务器接收当前请求的端口（最准确，无论是否配置多端口）。
		request.getLocalAddr()：返回服务器接收请求的 IP 地址（可能是内网 IP，如 192.168.x.x）。
		getPublicIp() 辅助方法：通过主机名解析所有 IP，过滤掉回环地址（127.0.0.1），优先返回局域网 / 公网 IP。
     * @param request
     * @throws IOException
     */
    public String getServerUrl(HttpServletRequest request) throws IOException {
        // 1. 获取服务对外的端口号（客户端实际连接的端口）
        int serverPort = request.getLocalPort();

        // 2. 获取服务对外的 IP 地址（客户端看到的服务器 IP）
        String publicIp = getPublicIp(); // 解析主机名获取实际对外 IP

        // 3. 准确判断协议类型
        String protocol = request.isSecure() ? "https" : "http"; // isSecure() 为 true 表示 HTTPS

        // 4. 拼接自身访问地址（使用实际协议）
        String selfUrl = protocol + "://" + publicIp + ":" + serverPort;
        return selfUrl;
    }
    
    // 解析主机名获取对外 IP（非 127.0.0.1）
    private String getPublicIp() {
        try {
            // 获取主机名对应的所有 IP，过滤掉回环地址
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress addr : addresses) {
                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                    return addr.getHostAddress(); // 返回局域网 IP
                }
            }
            // 若没有局域网 IP，返回本地主机名解析的 IP（可能是公网）
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1"; // 异常时默认回环地址
        }
    }
    
    
    
    
    
	
    public static void main(String[] args) {
    	Set<String> classpath = ServletUtil.getInstance().getClasspath();
    	for(String s : classpath) {
    		System.out.println("-" + s);
    	}
    	
    	classpath = ServletUtil.getInstance().getClasspath(Description.class, FinishReason.class, McpAsyncServer.class);
    	for(String s : classpath) {
    		System.out.println("--" + s);
    	}
    	System.out.println();
	}

}

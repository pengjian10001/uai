package com.uni.uai.web.listener;

import java.io.IOException;
import java.io.PrintWriter;

import com.uni.uai.mcp.server.MyMcpServer;
import com.uni.uai.mcp.utils.ServletUtil;
import com.uni.uai.mcp.web.servlet.PagesServlet;
import com.uni.uai.mcp.web.servlet.sse.SseServlet;
import com.uni.uai.mcp.web.servlet.sse.UploadSseServlet;
import com.uni.uai.mcp.web.servlet.StaticServlet;
import com.uni.uai.mcp.web.servlet.StreamServlet;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebListener
public class DynamicServletListener implements ServletContextListener {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        //获取类加载路径，以便后续使用
        ServletUtil.getInstance().setServletPath(context);
        context.addServlet("dynamicServlet", new DynamicServlet()).addMapping("/dynamic");
        
        ServletRegistration.Dynamic dynamic = context.addServlet("mcpServer", MyMcpServer.getTransportProvider());
        dynamic.setAsyncSupported(true);
        dynamic.addMapping("/*");
        
        //添加mcp管理的servlet
        context.addServlet("mcpPages", new PagesServlet()).addMapping("/pages/*");
        //静态页面，例如html
        context.addServlet("mcpStatic", new StaticServlet()).addMapping("/static/*");
        
        //文件上传接口对应的servelt
        // 1. 动态注册Servlet并获取Dynamic对象
        ServletRegistration.Dynamic streamServlet = context.addServlet("mcpStream", new StreamServlet());
        // 2. 手动配置multipart参数，对于上传文件的servlet，必须配置此config
        streamServlet.setMultipartConfig(
            new MultipartConfigElement(
            	StreamServlet.UPLOAD_TEMP_DIR,  // 临时文件目录（与StreamServlet中一致）
            	StreamServlet.maxFileSize,  // 单个文件最大30MB
            	StreamServlet.maxRequestSize, // 请求总大小最大100MB
            	StreamServlet.fileSizeThreshold    // 1MB缓冲区（文件大小超过此值则写入临时文件）
            )
        );
        // 3. 配置映射路径
        streamServlet.addMapping("/stream/*");
        
        //通用的sse servlet
        ServletRegistration.Dynamic sseServlet = context.addServlet("commonsse", new SseServlet());
        //启动异步支持
        sseServlet.setAsyncSupported(true);
        sseServlet.addMapping("/commonsse/*");
        
        //文件上传接口且为sse流式输出对应的servelt
        // 1. 动态注册Servlet并获取Dynamic对象
        ServletRegistration.Dynamic uploadSseServlet = context.addServlet("uploadsse", new UploadSseServlet());
        // 2. 手动配置multipart参数，对于上传文件的servlet，必须配置此config
        uploadSseServlet.setMultipartConfig(
            new MultipartConfigElement(
            	UploadSseServlet.UPLOAD_TEMP_DIR,  // 临时文件目录（与UploadSseServlet中一致）
            	UploadSseServlet.maxFileSize,  // 单个文件最大30MB
            	UploadSseServlet.maxRequestSize, // 请求总大小最大100MB
            	UploadSseServlet.fileSizeThreshold    // 1MB缓冲区（文件大小超过此值则写入临时文件）
            )
        );
        //启动异步支持
        uploadSseServlet.setAsyncSupported(true);
        // 3. 配置映射路径
        uploadSseServlet.addMapping("/uploadsse/*");
        
        logger.info("动态添加servlet完成");
    }
    
    // 定义一个简单的 Servlet
    class DynamicServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h1>Dynamic Servlet</h1>");
            out.println("</body></html>");
        }
    }
    
}

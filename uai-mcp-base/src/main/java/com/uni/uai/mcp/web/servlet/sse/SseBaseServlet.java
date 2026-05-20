package com.uni.uai.mcp.web.servlet.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.HttpServletUtil;
import com.uni.uai.mcp.utils.context.TemplateSseUtil;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.thread.UbagThreadUtils;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.FileUtil;
import com.uni.ubag.data.util.concurrent.UbagDataThreadUtil;
import com.uni.ubag.log.util.UbagLogUtil;

/**
在 Java Servlet 中实现 Server-Sent Events（SSE），核心是利用 HTTP 长连接 + 文本流传输，遵循 SSE 协议规范（UTF-8 编码、特定响应头、消息格式）。
SSE 是单向通信（服务端→客户端），适合推送实时消息（如通知、日志、实时数据更新），以下是完整实现方案：
一、SSE 协议核心规范
要实现 SSE，必须满足以下协议要求，否则客户端无法正确解析：
1、响应头：
	Content-Type: text/event-stream（标识为 SSE 流）
	Cache-Control: no-cache（禁止缓存，避免客户端复用旧流）
	Connection: keep-alive（维持长连接）
	X-Accel-Buffering: no（禁用 Nginx 等反向代理的缓冲，否则消息会被批量推送）
2、消息格式（每行必须以 \n 结尾，空行分隔消息）：
	基础格式：data: 消息内容\n\n（最核心字段，客户端通过 event.data 获取）
	可选字段：
		id: 消息ID\n（客户端记录 ID，重连时通过 Last-Event-ID 请求头携带，实现断点续传）
		event: 事件名\n（客户端可通过 onmessage 监听默认事件，或通过 addEventListener(事件名) 监听自定义事件）
		retry: 重连间隔(毫秒)\n（客户端断开连接后，自动重连的时间）
3、编码：所有消息必须使用 UTF-8 编码。

二、Servlet 实现 SSE 完整代码
1. 环境准备
	依赖：无需额外依赖，仅需 Servlet 5.0+ 规范（Tomcat 10+、Jetty 11+、GlassFish 6+ 等支持 Jakarta EE 9+ 的容器。）
	客户端：浏览器原生支持 EventSource API（IE 不支持，需用 polyfill）

 */
public abstract class SseBaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String UTF_8 = "UTF-8";
	//url前缀
	public static final String URL_PREFIX = "/commonsse";
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    // 异步线程池：处理消息推送（避免阻塞 Servlet 主线程）
    protected final ExecutorService executor = Executors.newFixedThreadPool(50);


    @Override
    public void init() throws ServletException {
        super.init();
    }

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
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. 配置 SSE 核心响应头（严格遵循协议规范）
        response.setContentType("text/event-stream;charset=UTF-8"); // 标识为 SSE 流，显式指定 UTF-8
        response.setCharacterEncoding("UTF-8");       // 强制 UTF-8 编码
        response.setHeader("Cache-Control", "no-cache"); // 禁止缓存
        response.setHeader("Connection", "keep-alive"); // 维持长连接
        response.setHeader("X-Accel-Buffering", "no");  // 禁用 Nginx 缓冲（关键）

        // 2. 跨域支持（若客户端与服务端不同域，按需开启）
        response.setHeader("Access-Control-Allow-Origin", "*"); // 生产环境指定具体域名
        response.setHeader("Access-Control-Allow-Methods", "GET,POST"); // 同时支持 GET 和 POST
        response.setHeader("Access-Control-Allow-Headers", "Last-Event-ID");

        // 3. 获取客户端断点续传 ID（首次连接为 null）
        //String lastEventId = request.getHeader("Last-Event-ID");
        //System.out.println("客户端上次接收的消息ID：" + (lastEventId == null ? "无" : lastEventId));

        // 4. 禁用响应缓冲，确保消息实时推送
        response.flushBuffer();

        // 5. 获取输出流（用于写入 SSE 消息）
        PrintWriter out = response.getWriter();

        // 6. 开启异步支持（Jakarta Servlet 异步 API）
        request.setAttribute("jakarta.servlet.async.supported", true);
        AsyncContext asyncContext = request.startAsync();
        //此处超时是一个兜底方案。如果客户端关闭，服务端没有感知到，超时后 AsyncListener.onTimeout 会关闭 out 并调用 asyncContext.complete()，无需额外处理，但需确保超时时间合理（避免过短导致正常连接被断开）。
        asyncContext.setTimeout(3 * 60 * 1000); // 异步超时：3分钟（按需调整）
        
        // 7. 监听异步事件（连接关闭/超时/错误时释放资源）
        asyncContext.addListener(new MyAsyncListener(out));

        // 8. 异步推送消息（核心逻辑）
        executor.submit(() -> {
        	//将sse上下文传递到后面进程中
        	TemplateSseUtil.getInstance().setSseContext(asyncContext, out);
        	long start = System.currentTimeMillis();
            try {
            	//业务处理
            	handle(request);
                
                //out.close(); // 不在此处关闭流，而是在MyAsyncListener中
            } catch (Exception e) {
            	e.printStackTrace();
            	String msg = "SSE 推送异常：" + e.getMessage();
                UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "SSE 推送异常：" + e.getMessage(), ExceptionUtil.toStackTrace(e), e, false, 0L, "");
                System.err.println(msg);
                out.println(msg);
                out.flush();
                //对于没有在MyAsyncListener的onError中捕获到的异常（例如，还没有进入MyAsyncListener，在Freemarker模版中主动抛出的异常），会被此处捕获
                //因此，异常时，关闭流
                out.close();
            } finally {
            	//以上，在这个异步线程的ThreadLocal中设置了asyncContext和请求参数，因此，结束时，也清空上下文
            	UbagThreadUtils.cleanAll();
        		UbagDataThreadUtil.cleanAll();
        		//finally中不关闭流，因为如果正常结束时，流是在MyAsyncListener中关闭的
            }
        });
    }
	
	public abstract void handle(HttpServletRequest request) throws Exception;
    
    // 销毁 Servlet 时关闭线程池（避免内存泄漏）
    @Override
    public void destroy() {
        executor.shutdownNow();
        super.destroy();
    }
    
    private class MyAsyncListener implements AsyncListener{
    	long start = System.currentTimeMillis();
    	PrintWriter out;
    	
    	public MyAsyncListener(PrintWriter out) {
			super();
			this.out = out;
		}

		@Override
    	public void onComplete(jakarta.servlet.AsyncEvent event) throws IOException {
    	    logger.info("SSE 连接完成，释放资源");
    	    out.close(); // 仅关闭当前连接的输出流，不关闭线程池
    	    System.out.println("onComplete:" + (System.currentTimeMillis()-start) + "ms");
    	    //asyncContext.complete();//onComplete 调用了 asyncContext.complete()，会导致循环调用，需删除：
    	}

    	@Override
    	public void onTimeout(jakarta.servlet.AsyncEvent event) throws IOException {
    	    logger.warn("SSE 连接超时，释放资源");
    	    out.close();
    	    //asyncContext.complete();
    	    System.out.println("onTimeout:" + (System.currentTimeMillis()-start) + "ms");
    	}

    	@Override
    	public void onError(jakarta.servlet.AsyncEvent event) throws IOException {
    	    logger.error("SSE 连接错误", event.getThrowable());
    	    out.close();
    	    //asyncContext.complete();
    	    System.out.println("onError:" + (System.currentTimeMillis()-start)/1000 + "ms");
    	}

        @Override
        public void onStartAsync(jakarta.servlet.AsyncEvent event) throws IOException {}
    }


}

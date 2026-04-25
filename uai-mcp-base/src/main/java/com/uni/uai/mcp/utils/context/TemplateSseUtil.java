package com.uni.uai.mcp.utils.context;

import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import jakarta.servlet.AsyncContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import com.uni.uai.mcp.client.McpStreamClientUtil;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.utils.context.TemplateSseUtil.SseContext;

/**
 * 用于模版中计算的帮助类
 *
 * @author pengjian
 */
public class TemplateSseUtil {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	public static TemplateSseUtil instance = new TemplateSseUtil();
	
	public static TemplateSseUtil getInstance() {
		return instance;
	}
	
	public void setSseContext(AsyncContext asyncContext, PrintWriter writer){
		SseContext sseContext = new SseContext(asyncContext, writer);
		//System.out.println("set的线程id="+Thread.currentThread().getId());
		UbagConf.setRequestConf(UaiConf.SSE_CONTEXT, sseContext);
		//UbagConf.setRequestConf(UaiConf.SSE_WRITER, writer);
	}
	
	public SseContext getSseContext(){
		//System.out.println("get的线程id="+Thread.currentThread().getId());
		SseContext context = UbagConf.getRequestCast(UaiConf.SSE_CONTEXT, SseContext.class);
		return context;
	}

	public void testChat(String message) throws Exception {
		McpStreamClientUtil.getInstance().queryLLMPure(message, new HashMap<String,Object>());
	}
	
	public void test(String lastEventId) throws InterruptedException {
		SseContext context = TemplateSseUtil.getInstance().getSseContext();
		
		// 初始化消息 ID（优化：处理非整数 Last-Event-ID）
    	int messageId;
    	try {
    	    messageId = lastEventId == null || lastEventId.isEmpty() ? 1 : Integer.parseInt(lastEventId) + 1;
    	} catch (NumberFormatException e) {
    	    logger.warn("无效的 Last-Event-ID：{}，重置消息ID为1", lastEventId);
    	    messageId = 1;
    	}

        // 循环推送消息（实际场景：监听业务事件/定时任务触发）
    	long lastHeartbeatTime = System.currentTimeMillis();
    	// 循环10次后，结束
        for(int i = 0; i < 10; i++) {
        	//1.这里是心跳消息，根据情况可以去掉。
        	//部分防火墙 / 网关会主动断开长时间无数据传输的长连接，可添加心跳消息（空数据消息）避免连接被断开：
        	// 循环推送消息中添加心跳检测（每30秒发送一次心跳）
        	long currentTime = System.currentTimeMillis();
            // 30秒无消息时发送心跳（data: 后面跟空字符串，仅维持连接）
            if (currentTime - lastHeartbeatTime > 30 * 1000) {
            	context.send("data: \n\n");// 心跳消息（严格遵循格式）
                lastHeartbeatTime = currentTime;
            }
        	
        	//2.这里是业务消息
            // 构建 SSE 消息（严格遵循格式：字段+换行，空行结束）
            StringBuilder sseMsg = new StringBuilder();
            sseMsg.append("id: ").append(messageId).append("\n"); // 消息ID（断点续传用）
            sseMsg.append("event: realTimeMsg\n"); // 自定义事件名（客户端需对应监听）
            sseMsg.append("retry: 3000\n"); // 客户端重连间隔：3秒
            // 消息内容（支持 JSON 格式，客户端需解析）
            sseMsg.append("data: {\"msgId\":").append(messageId)
                    .append(",\"content\":\"Jakarta SSE 实时消息\"")
                    .append(",\"timestamp\":").append(System.currentTimeMillis())
                    .append("}\n");
            sseMsg.append("\n"); // 空行分隔消息（必须！）

            // 推送消息并强制刷新（避免缓冲导致延迟）
            context.send(sseMsg.toString());

            // 模拟消息间隔：每2秒推送一条
            Thread.sleep(2000);
            messageId++; // 消息ID自增
        }
	}
	
	public static class SseContext{
		AsyncContext asyncContext;
		PrintWriter writer;
		public SseContext(AsyncContext asyncContext, PrintWriter writer) {
			super();
			this.asyncContext = asyncContext;
			this.writer = writer;
		}
		public AsyncContext getAsyncContext() {
			return asyncContext;
		}
		public void setAsyncContext(AsyncContext asyncContext) {
			this.asyncContext = asyncContext;
		}
		public PrintWriter getWriter() {
			return writer;
		}
		public void setWriter(PrintWriter writer) {
			this.writer = writer;
		}
		public void send(String s){
			//此方法不能try/cathc，而是要抛出异常。否则无法释放out
			writer.write(s);
			writer.flush();
		}
		public void complete(){
			asyncContext.complete();
		}
	}

}

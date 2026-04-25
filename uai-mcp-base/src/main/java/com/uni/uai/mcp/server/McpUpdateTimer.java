package com.uni.uai.mcp.server;

import java.util.Timer;
import java.util.TimerTask;

import com.uni.uai.mcp.chatmemory.store.DbStore;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.thread.UbagThreadUtils;
import com.uni.ubag.common.util.IdCreator;
import com.uni.ubag.common.util.IpUtils;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.common.util.TimeUtil;
import com.uni.ubag.data.util.concurrent.UbagDataThreadUtil;
import com.uni.ubag.log.proxy.ProxyAction;
import com.uni.ubag.log.util.UbagLogUtil;

public class McpUpdateTimer {
	final private static Logger logger = LoggerFactory.getLogger(McpUpdateTimer.class);
	//private static AtomicInteger lock = new AtomicInteger(0);
	/**
	 * 客户端可以在spring中初始化并加载此类。
	 * 如果不是spring工程，则加载此类即可。
	 * 为了避免客户端多次调用而启动多次Timer，使用一个static控制只执行一次
	 */
	static {
		logger.info("初始化 McpUpdateTimer");
		//10s执行一次。由于是在static块中，要修改，必须重启JVM
		long timer_period = 15000L;
        Timer timer = new Timer();
        //全局配置加载任务
        timer.schedule(new McpUpdateTask(), 0, timer_period);
        
        //全局秒级任务
        //Timer timer2 = new Timer();
        //timer2.schedule(new McpSecondTask(), 0, 1000L);
    }

	/**
	 * 定时更新mcp prompt、tool等
	 * @author pengjian
	 *
	 */
	public static class McpUpdateTask extends TimerTask {
        public void run() {
        	long start = System.currentTimeMillis();
        	Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.TIMETASK, "McpUpdateTimer", "", new ProxyAction<Boolean>() {
    			public Boolean exec() throws Throwable{
    				String logId = "mcp_update_timer_" + IpUtils.getCachedIp() + "_" + IdCreator.getInstance("ubaglog").next();
                	UbagConf.setlogId(logId);
                	//定时更新prompt和tool
                	ServerUtil.getInstance().updatePromptsFromDB(MyMcpServer.getMcpSyncServer());
                	ServerUtil.getInstance().updateToolsFromDB(MyMcpServer.getMcpSyncServer());
                	ServerUtil.getInstance().updateLabelFromDB();
                	ServerUtil.getInstance().updateLabelToolFromDBAndMcpSelf();
    				return true;
    			}
    		});
        	UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.TRACE.getCode(), "McpUpdateTimer", TimeTrace.getMarkTrace(), null, result, System.currentTimeMillis()-start, "");
			//7.清除线程变量
            //由于此定时任务，是单线程的，所以每次任务执行后，对一些在doconfig执行中，对线程的一些校验（例如，s.getAndParseConfig方法中，对嵌套的检验），需要清除
        	UbagThreadUtils.cleanAll();
        	//UbagDataThreadUtil.cleanAll();
        }
    }
	
	//Mcp内部的秒级任务，例如，ChatMemory定时更新数据库
	public static class McpSecondTask extends TimerTask {
        public void run() {
        	long start = TimeUtil.currentTimeMillis();
        	Boolean result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.TIMETASK, "McpSecondTask", "", new ProxyAction<Boolean>() {
    			public Boolean exec() throws Throwable{
    				String logId = "mcp_update_timer_" + IpUtils.getCachedIp() + "_" + IdCreator.getInstance("ubaglog").next();
                	UbagConf.setlogId(logId);
                	//定时插入ChatMemory
                	DbStore.getInstance().batchInsert();
    				return true;
    			}
    		});
        	UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.TRACE.getCode(), "McpUpdateTimer", TimeTrace.getMarkTrace(), null, result, System.currentTimeMillis()-start, "");
			//7.清除线程变量
            //由于此定时任务，是单线程的，所以每次任务执行后，对一些在doconfig执行中，对线程的一些校验（例如，s.getAndParseConfig方法中，对嵌套的检验），需要清除
        	UbagThreadUtils.cleanAll();
        	//UbagDataThreadUtil.cleanAll();
        }
    }
}

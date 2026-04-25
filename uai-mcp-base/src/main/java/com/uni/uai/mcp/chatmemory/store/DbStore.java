package com.uni.uai.mcp.chatmemory.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.data.DataSourceUtil;
import com.uni.uai.mcp.model.ChatMessagePO;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.QueueUtil;
import com.uni.ubag.log.util.UbagLogUtil;

public class DbStore implements ChatMessageStore {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private DbStore() {}
	private static DbStore instance = new DbStore();
	public static DbStore getInstance() {
		return instance;
	}
	
	private String localCacheKey = "ubag.uai.chat.memory.dbstroe.key";
	
	//为避免每次添加，都执行数据库insert操作，先缓存到一个队列中，之后通过定时任务异步insert
	private BlockingQueue<ChatMessagePO> queue = new LinkedBlockingDeque<ChatMessagePO>(1000);

	@Override
	public List<ChatMessagePO> get(String key) {
		//为避免每次都数据库中获取，每次请求缓存
		List<ChatMessagePO> list = this.getRequestChatMessagePOList();
		if(list.size() ==0) {
			//如果为空，才会从数据库中获取历史
			List<ChatMessagePO> dbList = DataSourceUtil.getInstance().getChatMessagesFromDB(key,  UaiConf.CHAT_MEMORY_MAX_MESSAGE);
			//由于是从数据库返回固定条数，最前面的可能不完成，所以去掉
			if(dbList!= null && dbList.size() > UaiConf.CHAT_MEMORY_MAX_MESSAGE -1) {
				String singleId = dbList.get(0).getSingleId();
				for(ChatMessagePO po : dbList) {
					if(!singleId.equals(po.getSingleId())) {
						list.add(po);
					}
				}
			}else {
				list.addAll(dbList);
			}
			UbagConf.setRequestConf(localCacheKey, list);
		}
		return list;
	}

	@Override
	public void add(String key, ChatMessagePO value) {
		try {
			queue.add(value);
		} catch (Exception e) {
			//如果队列满时，会抛出此异常，此时不打印堆栈
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "DbStore add error", e.getClass(), e, false, 0L, ExceptionUtil.toStackTrace(e));
		}
		//同时添加到当前请求的缓存中
		List<ChatMessagePO> list = this.getRequestChatMessagePOList();
		list.add(value);
	}

	@Override
	public void remove(String key) {
		DataSourceUtil.getInstance().deleteChatMessagesFromDB(key);
	}
	
	//获取当前请求中的ChatMessagePOList，因此也只包含当前sessionId的ChatMessagePO
	//此list与queue的区别是，queue是保存全局的，包含所有还未插入数据库的、所有sessionId的ChatMessagePO
	private List<ChatMessagePO> getRequestChatMessagePOList(){
		List<ChatMessagePO> list = null;
		Object obj = UbagConf.getRequestConf(localCacheKey);
		if(obj == null) {
			//使用有界队列存储
			list = new BoundedLinkedList<ChatMessagePO>(UaiConf.CHAT_MEMORY_MAX_MESSAGE);
			UbagConf.setRequestConf(localCacheKey, list);
		}else {
			list = (List<ChatMessagePO>) obj;
		}
		return list;
	}
	
	public List<ChatMessagePO> getChatMessagePOList(int size){
		List<ChatMessagePO> list = QueueUtil.poll(queue, size);
		return list;
	}
	
	public void batchInsert() {
		List<ChatMessagePO> list = this.getChatMessagePOList(200);
		Object obj = DataSourceUtil.getInstance().batchInsertChatMessageToDB(list);
		logger.info("++++" + obj);
	}
	
	//一个有界的List，先进先出
	private static class BoundedLinkedList<E> extends LinkedList<E> {
	    private static final long serialVersionUID = 1L;
		private final int maxSize;

	    public BoundedLinkedList(int maxSize) {
	        this.maxSize = maxSize;
	    }

	    @Override
	    public boolean add(E element) {
	        if (size() >= maxSize) {
	            removeFirst();  // 移除最早的元素
	        }
	        return super.add(element);
	    }

		@Override
		public boolean addAll(Collection<? extends E> c) {
			if(c == null || c.size() == 0) {
				return false;
			}
			for(E element: c) {
				this.add(element);
			}
			return true;
		}
	    
	    
	}

}

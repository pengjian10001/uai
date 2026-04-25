package com.uni.uai.mcp.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

public class LRUMap<K,V> extends LinkedHashMap<K,V> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long serialVersionUID = 1L;
	private final int capacity;
    /**
     * 构造一个具有指定容量的LRU缓存
     * @param capacity 缓存的最大容量
     */
    public LRUMap(int capacity) {
        // 调用LinkedHashMap的构造函数
        // 参数1：初始容量
        // 参数2：负载因子
        // 参数3：accessOrder=true表示按照访问顺序排序，最近访问的元素会被移到链表尾部
    	super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    
    /**
     * 获取缓存容量
     */
    public int getCapacity() {
        return capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > capacity;
    }
    
    @Override
	public V put(K key, V value) {
		//logger.info("sessionMap size = " + super.size());
		return super.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return super.remove(key);
	}
    
}

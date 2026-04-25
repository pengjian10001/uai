package com.uni.uai.mcp.utils;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonUtil {
	private static JacksonUtil instance = new JacksonUtil();
	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static JacksonUtil getInstance() {
		return instance;
	}
	
	/**
	 * 将Map转换为对象
	 * @param <T>
	 * @param map
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) throws Exception {
        return objectMapper.convertValue(map, clazz);
    }
}

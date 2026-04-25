package com.uni.uai.mcp.common;

import com.alibaba.fastjson2.JSONObject;

/**
 * 结果封装器
 */
public interface ResultWrapper {
	public String data(String in);
	public String error(String msg);
	public String complete(String in);
	
	public static class Text implements ResultWrapper{

		@Override
		public String data(String in) {
			if(in != null) {
				return in.toString();
			}
			return null;
		}

		@Override
		public String error(String msg) {
			if(msg != null) {
				return msg;
			}
			return null;
		}

		@Override
		public String complete(String in) {
			if(in != null) {
				return in.toString();
			}
			return null;
		}
		
	}
	
	/**
	 * 对于非sse，正常返回、异常，都是作为文本返回
	 * 而对于sse，由于响应内容是一段段给出，前端需要根据输出的不同，有不同的响应，所以，返回内容要有所区分
	 */
	public static class SseJsonString implements ResultWrapper{

		@Override
		public String data(String in) {
			//不判断null，如果为null，json中的content也为null
			JSONObject obj = new JSONObject();
			obj.put("stage", "data");
			obj.put("content", in);
			return obj.toString();
		}

		@Override
		public String error(String msg) {
			JSONObject obj = new JSONObject();
			obj.put("stage", "error");
			obj.put("content", msg);
			return obj.toString();
		}

		@Override
		public String complete(String in) {
			JSONObject obj = new JSONObject();
			obj.put("stage", "complete");
			obj.put("content", in);
			return obj.toString();
		}
		
	}
}

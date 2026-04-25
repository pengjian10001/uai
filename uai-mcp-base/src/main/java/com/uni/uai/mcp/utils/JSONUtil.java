package com.uni.uai.mcp.utils;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.uni.ubag.common.exception.ErrorCode;
import com.uni.ubag.common.exception.SourceVerifyException;


public class JSONUtil {
	private static JSONUtil instance = new JSONUtil();
	public static JSONUtil getInstance() {
		return instance;
	}
	
	public Integer getInteger(JSONObject json, String key, Integer defaultValue){
		if(json == null){
			return defaultValue;
		}
		Integer i = json.getInteger(key);
		if(i==null){
			return defaultValue;
		}else{
			return i;
		}
	}
	
	public Double getDouble(JSONObject json, String key, Double defaultValue){
		if(json == null){
			return defaultValue;
		}
		Double i = json.getDouble(key);
		if(i==null){
			return defaultValue;
		}else{
			return i;
		}
	}
	
	public Boolean getBoolean(JSONObject json, String key, Boolean defaultValue){
		if(json == null){
			return defaultValue;
		}
		Boolean i = json.getBoolean(key);
		if(i==null){
			return defaultValue;
		}else{
			return i;
		}
	}
	
	public String getString(JSONObject json, String key, String defaultValue){
		if(json == null){
			return defaultValue;
		}
		String result = json.getString(key);
		if(result==null){
			return defaultValue;
		}else{
			return result;
		}
	}
	
	public JSONObject getJSONObject(JSONObject json, String key){
		if(json == null){
			return null;
		}
		return json.getJSONObject(key);
	}
	
	public JSONArray getJSONArray(JSONObject json, String key){
		if(json == null){
			return null;
		}
		if(!json.containsKey(key)){
			return null;
		}
		return json.getJSONArray(key);
	}
	
	public JSONArray getJSONArray(JSONObject json, String key, JSONArray defaultValue){
		if(json == null){
			return defaultValue;
		}
		if(!json.containsKey(key)){
			return defaultValue;
		}
		return json.getJSONArray(key);
	}
	
	public JSONObject toJsonObject(Object obj) {
		if(obj==null){
			return null;
		}
		JSONObject json = null;
		boolean result_tag = true;
		try {
			if(obj instanceof JSONObject){
				json = (JSONObject)obj;
			}else if(obj instanceof String){
				json = JSON.parseObject((String)obj);
			}else{
				json = (JSONObject) JSON.toJSON(obj);
			}
		} catch (Exception e) {
			try {
				json = JSON.parseObject(toJSONString(obj));
			} catch (Exception e2) {
				result_tag = false;
				throw new SourceVerifyException(ErrorCode.COMMONCONFIG_TEMPLATE_METHOD_ERROR,String.format("toJsonObject异常，obj=%s", obj),e2);
			}
		} 
		return json;
	}
	
	public JSONArray toJsonArray(Object obj) {
		if(obj==null){
			return null;
		}
		JSONArray json = null;
		try {
			if(obj instanceof JSONArray){
				json = (JSONArray)obj;
			}else if(obj instanceof String){
				json = JSON.parseArray((String)obj);
			}
			/**else if(obj.getClass().getSimpleName().equals("SequenceAdapter")){
				String str = toJSONString(obj);
				json = JSON.parseArray(str);
			}**/
			else{
				json = (JSONArray) JSON.toJSON(obj);
			}
		} catch (Exception e) {
			try {
				json = JSON.parseArray(toJSONString(obj));
			} catch (Exception e2) {
				throw new SourceVerifyException(ErrorCode.COMMONCONFIG_TEMPLATE_METHOD_ERROR,String.format("toJsonArray异常，obj=%s", obj),e2);
			}
		} 
		return json;
	}
	
	public Object toJsonObjectOrArray(Object obj) {
		if(obj==null){
			return null;
		}
		if(obj instanceof JSONObject || obj instanceof JSONArray){
			return obj;
		}else if(obj instanceof String){
			try {
				return toJsonObject(obj);
			} catch (Exception e) {
				return toJsonArray(obj);
			}
		}else{
			return JSON.toJSON(obj);
		}
	}
	
	public String toJson(Object o) {
		return this.toJSONString(o);
	}
	
	public String toJSONString(Object o) {
		if(o==null){
			return null;
		}
		String json = JSON.toJSONString(o);
		return json;
	}
	
	public JSONObject parseObject(String json) {
		JSONObject obj = JSON.parseObject(json);
		return obj;
	}
	
	
	public <T> List<T> parseArray(String json, Class<T> clazz) {
		List<T> list = JSONArray.parseArray(json, clazz);
		return list;
	}
	
	public JSONArray parseArray(String text) {
		JSONArray arr = JSON.parseArray(text);
		return arr;
	}
	
	public Object parse(String text) {
		Object obj = JSON.parse(text);
		return obj;
	}
	
	public boolean isJson(Object obj){
		if(obj==null){
			return false;
		}
		try {
			Object jsonobj = JSON.parse(obj.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public void main(String[] args) {
		
		
	}

}

package com.uni.uai.mcp.utils.context;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.BaseConstant;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.exception.ErrorCode;
import com.uni.ubag.common.exception.SourceVerifyException;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.DateUtils;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.IdCreator;
import com.uni.ubag.common.util.IpUtils;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.common.util.MD5Util;
import com.uni.ubag.common.util.ParseUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.common.util.StringUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.common.util.TimeUtil;
import com.uni.ubag.common.util.TypeUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateModelException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.uni.uai.mcp.utils.ConfigTemplateUtils;

/**
 * 用于模版中计算的帮助类
 *
 * @author pengjian
 */
public class TemplateUtil {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	public static TemplateUtil instance = new TemplateUtil();
	
	public static TemplateUtil getInstance() {
		return instance;
	}

	private static String sqlInjectionParttern = "select|insert|update|delete|=|<|>|\\'|\\/\\*|\\*|\\.\\.\\/|\\.\\/|sleep|like|union|into";

	public static final String  ODIN_ENCRYPT = "odincipher";

	//管理环境变量
	private Map<String, String> env = new ConcurrentHashMap<String,String>();
	static String ip = null;
	public String getCachedIp() throws UnknownHostException{
		if(ip==null){
			ip = IpUtils.getLocalIpAddress();
		}
		return ip;
	}


	public Double checkNumber(Object o, Double defaultValue) {
		if (o == null || "".equals(o) || o.toString().equals("0") || o.toString().equals("0.0")|| o.toString().equals("-")) {
			return defaultValue;
		}
		if (o instanceof BigDecimal) {
			BigDecimal ob = (BigDecimal) o;
			Double od = ob.doubleValue();
			if (od.isNaN()) {
				return defaultValue;
			} else {
				return od;
			}
		}
		if (o instanceof Double) {
			Double od = (Double) o;
			if (od.isNaN()) {
				return defaultValue;
			} else {
				return od;
			}
		}
		Double result = null;
		String str = null;
		try {
			str = o.toString().replaceAll("%", "");
			result = Double.parseDouble(str);
		} catch (Exception e) {
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "t.checkNumber异常", str, e, false, 0L, ExceptionUtil.toStackTrace(e));
			result = defaultValue;
		}
		return result;
	}

	public Long checkLong(Object o, Long defaultValue) {
		if (o == null || "".equals(o) || !isLong(o.toString()) || o.toString().equals("0")) {
			return defaultValue;
		}
		return Long.parseLong(o.toString());
	}

	public Boolean checkLong(Object o, Integer errorCode, String msg) {
		if (o == null || "".equals(o) || !isLong(o.toString()) || o.toString().equals("0")) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return true;
	}

	public Boolean checkLong2(Object o, Integer errorCode, String msg) {
		if (o == null || "".equals(o) || !isLong(o.toString())) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return true;
	}

	public Long checkLong2(Object o, Long defaultValue) {
		if (o == null || "".equals(o) || !isLong(o.toString())) {
			return defaultValue;
		}
		return Long.parseLong(o.toString());
	}

	/**
	 * 是否是整数
	 * @param str
	 * @return
	 */
	public boolean isLong(Object str){
		if(str==null){
			return false;
		}
		String s = str.toString();
		boolean ret = false;
		if(StringUtils.isNotBlank(s) && s.matches("^[-\\+]?[\\d]+$")){
			ret = true;
		}
		return ret;
	}
	
	public boolean isDouble(Object str){
		if(str==null){
			return false;
		}
		String s = str.toString();
		boolean ret = false;
		if(StringUtils.isNotBlank(s) && s.matches("^(-?\\d+)(\\.\\d+)?$")){
			ret = true;
		}
		return ret;
	}

	public String httpGetStr(Object obj) {
		if(obj==null){
			return null;
		}
		try {
			String str = new String(obj.toString().getBytes("iso8859-1"),"UTF-8");
			return str;
		} catch (Exception e) {
			logger.warn(String.format("处理编码转换异常,obj=%s", obj),e);
		}
		return obj.toString();
	}
	
	/**
	 * 在配置中，可以约定一些配置返回结果的特殊处理。
	 * 例如，如果返回结果为
	 * {	
	 * 		"action":"return",
	 * 		"data_type":"json",
	 * 		"result":{
	 * 			"key": "value"
	 * 		}
	 * }
	 * 则表示，parse后，希望提前返回结果，不要继续执行后续的流程。
	 * @param result 对于不同的情况，可以返回不同的内容
	 * @return
	 */
	public Object checkParseResult(Object result) {
		JSONObject obj = this.toJsonObject(result);
		String action = JSONUtil.getString(obj, "action", "");
		if(action.equalsIgnoreCase("return")) {
			String datatype = JSONUtil.getString(obj, "data_type", BaseConstant.DataType.JSON.name());
			Object resultValue = obj.get("result");
			if(resultValue==null) {
				this.error(ErrorCode.PARAM_ERROR, "checkParseResult时异常，当action为return时，必须包含result属性." + obj.toString());
			}
			if(datatype.equalsIgnoreCase(BaseConstant.DataType.JSON.name())) {
				this.normalReturnJson(resultValue);
			}else {
				this.normalReturn(resultValue);
			}
		}
		//其他约定，后续补充
		return 0;
	}
	
	/**
	 * 抛出normal return异常
	 * @param msg
	 * @return
	 */
	public Boolean normalReturnJson(Object msg) {
		ParseUtil.getInstance().setJsonDataType();
		return this.normalReturn(msg);
	}
	
	public Boolean normalReturn(Object msg) {
		UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.NORMALRETURN.getCode(), "抛出normal_return异常", msg, null, true, 0L, "");
		throw new SourceVerifyException(ErrorCode.NORMAL_RETURN, msg.toString());
	}

	public Boolean throwError(Integer errorCode, String msg) {
		throw new SourceVerifyException(errorCode, msg);
	}

	public Boolean error(Integer errorCode,String msg) {
		throw new SourceVerifyException(errorCode, msg);
	}

	//抛出的异常，在返回值中，会展示viewMsg，默认viewMsg=currentStack+msg，参见new SourceVerifyException(errorCode, msg)，此处也可指定viewMsg
	public Boolean error(Integer errorCode, String msg, String viewMsg) {
		throw new SourceVerifyException(errorCode, msg, viewMsg);
	}

	public Object check(Object o, Object defaultValue) {
		if (o == null || "".equals(o)) {
			return defaultValue;
		}
		return o;
	}

	//检查指定名称的参数是否存在，如果不存在，则设置为默认值
	public Object checkOrSet(String paramName, Object defaultValue) {
		Object o = UbagConf.getRequestConf(paramName);
		if (o == null || "".equals(o)) {
			UbagConf.setRequestConf(paramName, defaultValue);
			return defaultValue;
		}
		return o;
	}
	
	/**
	 * 注意：check方法是新起一个ConfigTemplateUtils.parse()，而在此方法中，通过调用ThreadLocalUtils.getParamMap()方法设置parse()的上下文
	 * 所以如果调用t.check("!name??",-1,"name不存在")时，name必须时父环境通过t.setParam()设置的变量，才返回true，
	 * 而name只是在父环境定义的变量，但没有通过t.setParam()设置到上下文，则返回false
	 * @param temp
	 * @param errorCode
	 * @param msg
	 * @return
	 */
	public Boolean check(String temp, Integer errorCode, String msg) {
		temp = "<#if (" + temp + ")>true</#if>";
		String t = ConfigTemplateUtils.parse(new HashMap<>(), temp);
		if ("true".equals(t)) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return true;
	}

	public Object checkNull(Object o, Integer errorCode, String msg) {
		if (o == null) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return o;
	}

	public Object checkNotNull(Object o, Integer errorCode, String msg) {
		if (o != null) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return o;
	}

	public Object checkEmpty(Object o, Integer errorCode, String msg) {
		if (o == null || "".equals(o.toString())) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return o;
	}

	public Object checkBetween(Object o, Double min, Double max, Integer errorCode, String msg) {
		if (o == null) {
			throw new SourceVerifyException(errorCode, msg);
		} else {
			Double tmp_o = this.checkNumber(o, Double.MIN_VALUE);
			if (tmp_o < min || tmp_o > max) {
				throw new SourceVerifyException(errorCode, msg);
			}
		}
		return o;
	}

	public Object checkNotEqual(Object o1, Object o2, Integer errorCode, String msg) {
		try {
			if (!o1.equals(o2)) {
				throw new SourceVerifyException(errorCode, msg);
			}
		} catch (Exception e) {
			//logger.warn(String.format("checkNotEqual error o1=%s, o2=%s", o1, o2), e);
			throw new SourceVerifyException(errorCode, msg,e);
		}
		return o1;
	}

	public Object checkEqual(Object o1, Object o2, Integer errorCode, String msg) {
		try {
			if (o1.equals(o2)) {
				throw new SourceVerifyException(errorCode, msg);
			}
		} catch (Exception e) {
			//logger.warn(String.format("checkNotEqual error o1=%s, o2=%s", o1, o2), e);
			throw new SourceVerifyException(errorCode, msg,e);
		}
		return o1;
	}

	public Object checkIn(Object o1, List<Object> list, Integer errorCode, String msg) {
		try {
			for (Object o2 : list) {
				if (o1.equals(o2)) {
					throw new SourceVerifyException(errorCode, msg);
				}
			}
		} catch (Exception e) {
			//logger.warn(String.format("checkIn error o1=%s, o2=%s", o1, list), e);
			throw new SourceVerifyException(errorCode, msg,e);
		}
		return o1;
	}

	public Object checkNotIn(Object o1, List<Object> list, Integer errorCode, String msg) {
		try {
			for (Object o2 : list) {
				if (o1.equals(o2)) {
					return o1;
				}
			}
			throw new SourceVerifyException(errorCode, msg);
		} catch (Exception e) {
			//logger.warn(String.format("checkNotEqual error o1=%s, o2=%s", o1, list), e);
			throw new SourceVerifyException(errorCode, msg,e);
		}
	}

	public Object checkRegex(String o1, String regex, Integer errorCode, String msg) {
		List<String> list1 = RegexUtil.findMatchs(o1, regex);
		if (list1 != null && list1.size() > 0) {
			throw new SourceVerifyException(errorCode, msg);
		}
		return o1;
	}

	public Boolean checkParamNotNullAndSqlInjection (Object temp){
		if(null == temp || StringUtils.isBlank(String.valueOf(temp))){
			throw new SourceVerifyException(ErrorCode.SQL_NULL_ERROR, "参数不能为空。");
		}
		return checkSqlInjection(temp);
	}

	public Boolean checkSqlInjection (Object temp){
		if(null == temp){
			return true;
		}
		Pattern pattern = Pattern.compile(sqlInjectionParttern, Pattern.CASE_INSENSITIVE);
		String tempString = String.valueOf(temp);
		Matcher m = pattern.matcher(tempString);
		if(m.find()){
			throw new SourceVerifyException(ErrorCode.SQL_INJECTION_ERROR, "参数检查异常。");
		}
		return true;
	}

	public List<String> find(String o1, String regex, int group) {
		List<String> list1 = RegexUtil.findMatchs(o1, regex, group);
		return list1;
	}

	public List<String> find(String o1, String regex) {
		return this.find(o1, regex, 0);
	}

	public String findFirst(String o1, String regex, int group) {
		List<String> list1 = this.find(o1, regex, group);
		if(list1!=null && list1.size()>0){
			return list1.get(0);
		}
		return null;
	}

	public String findFirst(String o1, String regex) {
		return this.findFirst(o1, regex, 0);
	}

	public boolean patternMatch(String str, String regex) {
		if (str == null || regex == null) return false;
		Pattern pattern = RegexUtil.compile(regex);
		return pattern.matcher(str).matches();
	}
	
	public static String replaceAll(String regex, String str, String replacement){
		return RegexUtil.replaceAll(regex, str, replacement);
	}

	//将一个对象，转换为一个json对象的属性，例如，如果参数是一个string，但带有引号等特殊字符，则会转移
	public Object toJsonValue(Object o) {
		if(o==null){
			return null;
		}
		JSONObject json = new JSONObject();
		json.put("key", o);
		return json.get("key");
	}

	/**
	 * 将参数解析为一个JSON字符串，并返回解析后的结果
	 * @param o
	 * @return
	 */
	public String toJson(Object o) {
		if(o==null){
			return null;
		}
		String json = JSONUtil.toJSONString(o);
		return json;
	}

	public String toJsonWithoutEscape(Object o) {
		String json = toJson(o);
		if (json == null) return null;
		json = StringEscapeUtils.unescapeJava(json);
		if (json.startsWith("\"")) json = json.substring(1);
		if (json.endsWith("\"")) json = json.substring(0,json.length()-1);
		json = StringEscapeUtils.unescapeJava(json);
		if (json.startsWith("\"")) json = json.substring(1);
		if (json.endsWith("\"")) json = json.substring(0,json.length()-1);
		return json;
	}

	/**
	 * 将一个多行的（以\n区分多行）、格式化风格（每行前面通过空白字符进行格式化）的字符串，进行格式化转换，例如转换为html的多行数据
	 * @param str
	 * @param replaceBlank
	 * @param replaceNewline
	 * @return
	 */
	public String formatMutiLineString(String str, String replaceBlank, String replaceNewline){
		if(str==null){
			return null;
		}
		//toPettyJson()方法格式化后，以\t为缩进，以\n为换行符，需要替换为html的格式化
		List<String> list = this.readLine(str);
		StringBuffer sb = new StringBuffer();
		int size = list.size();
		for(int i = 0; i < size; i++){
			String s = list.get(i);
			//将前导的所有\t替换为replaceBlank，正则逻辑为，替换第一个空白，并替换紧挨前面空白后面空白
			sb.append(s.replaceAll("^\\s|(?<=^\\s+)\\s", replaceBlank));
			if(i < size - 1){
				sb.append(replaceNewline);
			}
		}
		return sb.toString();		
	}
	
	/**
	 * 按行读取一个str到一个List<String>
	 * @param str
	 * @return
	 */
	public List<String> readLine(String str){
		List<String> list = StringUtil.readLine(str);
		return list;
	}
	
	public String getClass(Object obj) {
		if(obj==null){
			return "null";
		}else{
			return obj.getClass().getName();
		}
	}

	public JSONObject toJsonObject(Object obj) {
		return JSONUtil.toJsonObject(obj);
	}

	//如果转换不成功，忽略异常
	public JSONObject toJsonObject(Object obj, Object defaultValue) {
		if(obj==null){
			return null;
		}
		try {
			JSONObject json = this.toJsonObject(obj);
			return json;
		} catch (Exception e) {
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "toJsonObject异常", obj, e, false, 0L, ExceptionUtil.toStackTrace(e));
			return this.toJsonObject(defaultValue);
		}
	}

	//json 对象中的string数组转换成jsonObject 放回去
	public JSONObject paramToJsonArray(Object obj,String key){
		JSONObject object = this.toJsonObject(obj);
		String paramObj = object.getString(key);
		JSONArray paramArray=this.toJsonArray(paramObj,"[]");
		object.put(key,paramArray);
		return object;
	}

	//将数组中的json 对象中的string数组转换成jsonObject 放回去
	public JSONArray convertParamToJsonArray(Object arrayObj, String param){
		JSONArray array = toJsonArray(arrayObj);
		if(null == array || array.isEmpty()){
			return array;
		}
		JSONArray result = new JSONArray();
		for (int i = 0; i < array.size(); i++){
			JSONObject obj = array.getJSONObject(i);
			String paramObj = obj.getString(param);
			if(StringUtils.isNotBlank(paramObj)){
				JSONArray newObj = JSONArray.parseArray(paramObj);
				obj.put(param, newObj);
			}
			result.add(i,obj);
		}
		return result;
	}

	//将数组中的json 对象中的string数组转换成jsonObject 放回去
	public JSONArray convertParamToJsonObject(Object arrayObj, String param){
		JSONArray array = toJsonArray(arrayObj);
		if(null == array || array.isEmpty()){
			return array;
		}
		JSONArray result = new JSONArray();
		for (int i = 0; i < array.size(); i++){
			JSONObject obj = array.getJSONObject(i);
			String paramObj = obj.getString(param);
			if(StringUtils.isNotBlank(paramObj)){
				JSONObject newObj = JSONObject.parseObject(paramObj);
				obj.put(param, newObj);
			}
			result.add(i,obj);
		}
		return result;
	}

	public JSONArray toJsonArray(Object obj) {
		return JSONUtil.toJsonArray(obj);
	}

	//如果转换不成功，忽略异常
	public JSONArray toJsonArray(Object obj, Object defaultValue) {
		if(obj==null){
			return null;
		}
		try {
			JSONArray json = this.toJsonArray(obj);
			return json;
		} catch (Exception e) {
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "toJsonArray异常", obj, e, false, 0L, ExceptionUtil.toStackTrace(e));
			return this.toJsonArray(defaultValue);
		}
	}

	//obj类型未知，尝试将其转换为jsonObject或josnarray
	public Object toJsonObjectOrArray(Object obj) {
		return JSONUtil.toJsonObjectOrArray(obj);
	}

	/**
	 * 在rootObject对象中，按照jsonPath的路径，添加一个value对象。
	 * 例如，setToJsonObject(rootObject, "$.abc.efg", value)，在rootObject中key为"abc"的JSONObject对象中，设置键为"egf"，值为value的元素
	 * @param rootObject 	一个可以转换为JSONObject的对象，例如，如果是一个JSON字符串，或一个java对象，或JSONObject对象本身
	 * @param path 			一个符合Json Path预发的路径
	 * @param value 		一个要添加的value独享
	 * @return 以JOSNObject的形式，返回变更后的对象
	 */
	public Object setToJsonObject(Object rootObject, String path, Object value){
		JSONObject root = this.toJsonObject(rootObject);
		Object obj = JSONPath.set(root, path, value);
		return obj;
	}

	//比上一个方法更通用，传递的rootObject可以为json对象或json数组
	public Object setToJson(Object rootObject, String path, Object value){
		Object root = this.toJsonObjectOrArray(rootObject);
		Object obj = JSONPath.set(root, path, value);
		return obj;
	}

	public Object removeFromJson(Object rootObject, String path){
		Object root = this.toJsonObjectOrArray(rootObject);
		JSONPath.remove(root, path);
		return root;
	}

	public Object setToJsonAarry(Object rootObject, String path, Object value){
		JSONArray root = this.toJsonArray(rootObject);
		Object obj = JSONPath.set(root, path, value);
		return obj;
	}

	/**
	 * 在rootObject对象中，按照jsonPath的路径，添加一个value对象。
	 * 例如，addToJsonArray(rootObject, "$.abc", value)，在rootObject中key为"abc"的JSONArray数组中，添加一个值为value的元素
	 * @param rootObject 	一个可以转换为JSONObject的对象，例如，如果是一个JSON字符串，或一个java对象，或JSONObject对象本身
	 * @param path 			一个符合Json Path预发的路径
	 * @param values		一个要添加的value独享
	 * @return 以JSONObject的形式，返回变更后的对象
	 */
	public Object addToJsonArray(Object rootObject, String path, Object... values){
		JSONObject root = this.toJsonObject(rootObject);
		//在freemarker中生成的JSON对象，内部的对象可能是不可编辑的，例如，可能是SimpleSenquce，从而抛出UnsupportedOperationException
		//所以先将这些path对应的对象转换为JSONArray，再进行add
		Object pahtObj = JSONPath.eval(root, path);
		JSONArray pathArr = this.toJsonArray(pahtObj);
		for(Object value : values) {
			pathArr.add(value);
		}
		Object result = this.setToJsonObject(rootObject, path, pathArr);
		return result;
	}

	public Object addToJsonArray2(Object rootObject, String path, Object values){
		Object new_root_object = JSONObject.parseObject(JSONObject.toJSONString(rootObject));
		Object new_value = JSONObject.parseObject(JSONObject.toJSONString(values));
		return  this.addToJsonArray(new_root_object,path,new_value);
	}


	public boolean setParam(String key, Object value) {
		UbagConf.setRequestConf(key, value);
		return true;
	}

	public boolean setParamIfNotExist(String key, Object value) {
		Object v = UbagConf.getRequestConf(key);
		if(v==null){
			UbagConf.setRequestConf(key, value);
			return true;
		}else{
			return false;
		}
	}

	public boolean appendParam(String key, Object value) {
		String s = UbagConf.getRequestString(key);
		if (s == null) {
			s = "";
		}
		s += (value==null?"":value.toString());
		UbagConf.setRequestConf(key, s);
		return true;
	}

	public boolean incrParam(String key, Object value) {
		Double num = this.checkNumber(value, 0D);
		Object d = UbagConf.getRequestConf(key);
		if (d == null) {
			d = 0D;
		}
		d = (Double) d + num;
		UbagConf.setRequestConf(key, d);
		return true;
	}

	public boolean putMapParam(String mapKey, String key, Object value) {
		Object o = UbagConf.getRequestConf(mapKey);
		if (o == null) {
			o = new JSONObject();
		}
		((JSONObject) o).put(key, value);
		UbagConf.setRequestConf(mapKey, o);
		return true;
	}

	/**
	 * 将对象中的所有属性加入threadlocal
	 * @param obj
	 * @return
	 */
	public boolean putAllParams(Object obj){
		JSONObject jsonObj = new JSONObject();
		try {
			if (obj != null) {
				jsonObj = this.toJsonObject(obj);
			}
		} catch (Exception e) {
			logger.warn(String.format("putAllParams error. obj=%s,e=%s", obj, e.getClass() + ":" + e.getMessage()), e);
			return false;
		}
		UbagConf.setAllRequestConf(jsonObj);
		return true;
	}

	public Map<String, Object> getAllParams(){
		Map<String, Object> map = UbagConf.getRequestConf();
		return map;
	}

	public Object getParam(String key){
		Object obj = UbagConf.getRequestConf(key);
		return obj;
	}


	//删除一个属性
	public boolean removeParam(String key) {
		UbagConf.setRequestConf(key, null);
		return true;
	}

	//将一个对象作为一个元素放入list中
	public boolean putListParam(String listKey, Object value) {
		Object o = UbagConf.getRequestConf(listKey);
		if (o == null) {
			o = new JSONArray();
		}
		((JSONArray) o).add(value);
		UbagConf.setRequestConf(listKey, o);
		return true;
	}

	//将一个对象作为一个元素放入list中, 为了避免list太大，可设置list最大值
	public boolean putListParam(String listKey, Object value, int maxsize) {
		Object o = UbagConf.getRequestConf(listKey);
		if (o == null) {
			o = new JSONArray();
			UbagConf.setRequestConf(listKey, o);
		}
		if(((JSONArray) o).size()<maxsize){
			((JSONArray) o).add(value);
			return true;
		}else{
			logger.warn(String.format("putListParam的list尺寸超限，maxsize=%s, listsize=%s", maxsize, ((JSONArray) o).size()));
			return false;
		}
	}


	//将一个k/v添加到一个对象中
	public JSONObject objAdd(Object obj, String valueName, Object value) {
		JSONObject result = new JSONObject();
		try {
			if (obj != null) {
				result = this.toJsonObject(obj);
				result.put(valueName, value);
			}
		} catch (Exception e) {
			logger.warn(String.format("objAdd error. obj=%s,valueName=%s,value=%s,e=%s", obj, valueName, value, e.getClass() + ":" + e.getMessage()), e);
		}
		return result;
	}

	public JSONObject putVal(Object obj, String key, Object value){
		try {
			JSONObject result = obj == null? new JSONObject() : this.toJsonObject(obj);
			result.put(key, value);
			return result;
		} catch (Exception e) {
			logger.warn(String.format("objAdd error. obj=%s,valueName=%s,value=%s,e=%s", obj, key, value, e.getClass() + ":" + e.getMessage()), e);
		}
		return new JSONObject();
	}

	//将一个对象中的所有字段，添加到一个对象中
	public JSONObject objAddAll(Object obj, Object objMap) {
		JSONObject result = new JSONObject();
		try {
			if (obj != null) {
				result = this.toJsonObject(obj);
				if(objMap!=null){
					result.putAll(this.toJsonObject(objMap));
				}
			}
		} catch (Exception e) {
			logger.warn(String.format("objAddAll error. obj=%s,objMap=%s,e=%s", obj, objMap, e.getClass() + ":" + e.getMessage()), e);
		}
		return result;
	}

	public JSONObject objRemove(Object obj, String valueName) {
		JSONObject result = null;
		if (obj == null) {
			return null;
		} else {
			result = this.toJsonObject(obj);
			if (result.containsKey(valueName)) {
				result.remove(valueName);
			}
		}
		return result;
	}

	public JSONObject objAdd(String valueName, Object value) {
		JSONObject result = new JSONObject();
		result.put(valueName, value);
		return result;
	}


	public JSONArray listMmap(Object list, String keyProp, Object map) {
		try {
			if (list != null && map != null) {
				JSONArray array = this.toJsonArray(list);
				JSONObject jsonObject = this.toJsonObject(map);
				for (int i = 0; i < array.size(); i++) {
					JSONObject obj = array.getJSONObject(i);
					obj.putAll(jsonObject.getJSONObject(obj.getString(keyProp)));
				}
				return array;
			}
		} catch (Exception e) {
			logger.warn(String.format("list2map error. list=%s,keyProp=%s,e=%s", list, keyProp, e.getClass() + ":" + e.getMessage()), e);
		}
		return new JSONArray();
	}

	/**
	 * 将列表形式的对象 转换成对应的string  如果列表内非数字isNumber为false
	 * @param objs [626, 704, 725, 0, 723, 709, 711]
	 * @return  626,704,725,0,723,709,711
	 *
	 */
	public String toSqlIn(boolean isNumber, Object... objs){
		if(objs==null || objs.length==0){
			return "";
		}
		StringBuffer rs = new StringBuffer();
		for(int i=0; i<objs.length; i++){
			if(objs[i]!=null){
				if(!isNumber){
					rs.append("'");
				}
				rs.append(objs[i]);
				if(!isNumber){
					rs.append("'");
				}
				if(i<objs.length-1 && rs.length()>0){
					rs.append(",");
				}
			}
		}
		if(rs.toString().endsWith(",")){
			return rs.toString().substring(0,rs.toString().length()-1);
		}else{
			return rs.toString();
		}
	}

	public String toSqlIn(boolean isNumber, List list){
		if(list==null || list.size()==0){
			return "";
		}
		Object[] objs = new Object[list.size()];
		for(int i=0; i<list.size(); i++){
			objs[i]=list.get(i);
		}
		return this.toSqlIn(isNumber, objs);
	}

	public String md5(String str){
		if(str==null){
			return null;
		}
		return MD5Util.md5(str);
	}

	public Long uniqueId(){
		return IdCreator.commonInstance.next();
	}

	//一个不会重复的随机Id，与uniqueId()方法不同，此方法产生的id不连续，不容易被猜测
	public Long randomUniqueId(){
		return IdCreator.commonInstance.next()*100+this.randomInt(99);
	}

	public Long serverId(){
		return IdCreator.id;
	}

	public String uuid(){
		return UUID.randomUUID().toString();
	}

	public Long randomLong(){
		Random r = new Random();
		return r.nextLong();
	}
	//返回一个伪随机数，在 0（包括）和指定值（不包括）之间均匀分布的 int 值。
	public Integer randomInt(int range){
		Random r = new Random();
		return r.nextInt(range);
	}

	public Double randomDouble(){
		Random r = new Random();
		return r.nextDouble();
	}

	public String urlEncode(Object str){
		if(str==null){
			return null;
		}
		String charset = UbagConf.getRequestString(UbagConf.URLConf.url_default_charset);
		String s = null;
		if(charset==null || "".equals(charset)){
			s = URLEncoder.encode(str.toString());
		}else{
			try {
				s = URLEncoder.encode(str.toString(),charset);
			} catch (UnsupportedEncodingException e) {
				throw new SourceVerifyException(ErrorCode.SERVICE_ERROR, String.format("urlEncode异常。e=%s", e.getClass()+"-"+e.getMessage()),e);
			}
		}
		return s;
	}

	public String urlAppendObject(String url, String key, Object param){
		StringBuffer result = new StringBuffer(url);
		if(url.contains("?")){
			result.append(String.format("&%s=%s", key, this.urlEncode(param)));
		}else{
			result.append(String.format("?%s=%s", key, this.urlEncode(param)));
		}
		return result.toString();
	}

	public String urlAppendMap(String url, Map<String,Object> param){
		StringBuffer result = new StringBuffer(url);
		if(param!=null && param.size()>0){
			for(String key : param.keySet()){
				if(result.toString().contains("?")){
					result.append(String.format("&%s=%s", key, this.urlEncode(param.get(key))));
				}else{
					result.append(String.format("?%s=%s", key, this.urlEncode(param.get(key))));
				}
			}
		}
		return result.toString();
	}

	public List toList(Object list) throws TemplateModelException{
		if(list==null){
			return null;
		}
		if(list instanceof SimpleSequence){
			List unwarplist = ((SimpleSequence)list).toList();
			return unwarplist;
		}
		return (List)list;
	}

	public String format(String format, Object... args){
		return String.format(format, args);
	}

	public String urlEncode(String str,String enc){
		try {
			String s = URLEncoder.encode(str,enc);
			return s;
		} catch (Exception e) {
			throw new SourceVerifyException(ErrorCode.SERVICE_ERROR, "urlencode error. e="+e.getClass()+":"+e.getMessage(),e);
		}
	}

	public String urlDecode(String str){
		if(str==null){
			return null;
		}
		String charset = UbagConf.getRequestString(UbagConf.URLConf.url_default_charset);
		String s = null;
		if(charset==null || "".equals(charset)){
			s = URLDecoder.decode(str.toString());
		}else{
			try {
				s = URLDecoder.decode(str.toString(),charset);
			} catch (UnsupportedEncodingException e) {
				throw new SourceVerifyException(ErrorCode.SERVICE_ERROR, String.format("urlDecode异常。e=%s", e.getClass()+"-"+e.getMessage()),e);
			}
		}
		return s;
	}

	public String urlDecode(String str,String enc){
		try {
			String s = URLDecoder.decode(str,enc);
			return s;
		} catch (Exception e) {
			throw new SourceVerifyException(ErrorCode.SERVICE_ERROR, "urldecode error. e="+e.getClass()+":"+e.getMessage(),e);
		}
	}
	//判断一个数据的类型
	public String getType(Object obj){
		return TypeUtil.getType(obj);
	}

	public boolean isObject(Object obj){
		String type = this.getType(obj);
		if(type==null){
			return false;
		}
		if("map".equals(type)){
			return true;
		}
		return false;
	}

	public boolean isList(Object obj){
		String type = this.getType(obj);
		if(type==null){
			return false;
		}
		if("list".equals(type)){
			return true;
		}
		return false;
	}

	public boolean isSimple(Object obj){
		String type = this.getType(obj);
		if(type==null){
			return false;
		}
		if("simple".equals(type)){
			return true;
		}
		return false;
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

	public boolean log(Object obj){
		//logger.info(obj);
		return this.log("info",obj);
	}

	//打印日志
	public boolean log(String level, Object msg){
		Object obj = (msg==null?"null":msg);
		if("debug".equals(level)){
			logger.info("......"+obj);
		}else if("warn".equals(level)){
			logger.warn("......"+obj);
		}else{
			logger.info("......"+obj);
		}
		return true;
	}

	public boolean setLogId(Object id){
		String logId = UbagConf.getRequestString("logId");
		String newLogId = (id==null?"":id.toString() + logId==null?"":logId);
		MDC.put("logId", newLogId);
		return true;
	}

	public boolean debug(String key, Object value){
		return this.trace(UbagConf.TraceConf.trace_log_debug, key, value);
	}

	public boolean info(String key, Object value){
		return this.trace(UbagConf.TraceConf.trace_log_info, key, value);
	}

	public void localInfo(String key , Object value){
		logger.info(key + "  " + value);
	}

	public boolean warn(String key, Object value){
		return this.trace(UbagConf.TraceConf.trace_log_warn, key, value);
	}

	private JSONObject levelMap = JSONObject.parseObject("{'debug':1,'info':2,'warn':3}");
	private boolean trace(String level, String key, Object value){
		if(key==null){
			return false;
		}
		//默认级别为info
		String configLevel = UbagConf.getRequestString(UbagConf.TraceConf.trace_log_level, "info");
		//如果设置有问题，默认也为info
		if(!levelMap.containsKey(configLevel)){
			configLevel = "info";
		}
		//参数传递的level的级别，传递的level为warn、info等，而Threadlocal中以_warn，_info为key存储，所以，此处用endWith()判断
		Integer levelNumber = 0;
		for(String s : levelMap.keySet()){
			if(level.endsWith(s)){
				levelNumber = levelMap.getInteger(s);
			}
		}
		//如果参数的level大于等于配置的level，则记录值
		if(levelNumber >= levelMap.getInteger(configLevel)){
			JSONObject msg = new JSONObject();
			msg.put(key, value);
			this.putListParam(level, msg);
			return true;
		}
		return false;
	}

	/**
	 * 搜索str，找到所有符合timeFormat时间格式的日期，并将增加days天，并替换
	 * 例如str="'before': '2019-09-10 10:01:30', 'name':'abc',  'after': '2019-09-11 10:01:30'   "
	 * timeRegex = "[0-9]{4}-[0-4]{2}-[0-9]{2}"
	 * timeFormat = "yyyy-MM-dd"
	 * days=1
	 * 则会找到所有符合"yyyy-MM-dd"格式的日志，并增加1天，结果返回"'before': '2019-09-11 10:01:30', 'name':'abc',  'after': '2019-09-12 10:01:30'   "
	 * @param str
	 * @param timeRegex
	 * @param timeFormat
	 * @return
	 */
	public String replaceAndAddDay(String str, String timeRegex, String timeFormat, Integer days){
		if(str==null){
			return null;
		}
		Pattern pt = Pattern.compile(timeRegex);
		Matcher match = pt.matcher(str);
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while (match.find()) {
			//start、end分别是匹配所在的起始和结束位置
			int start = match.start();
			int end = match.end();
			//匹配的字符串
			String s = match.group();
			//解析为日期
			SimpleDateFormat format = new SimpleDateFormat(timeFormat);
			Date date = null;
			try {
				date = format.parse(s);
			} catch (ParseException e) {
				throw new SourceVerifyException(ErrorCode.TEMPLATE_UTIL_ERROR, String.format("时间解析异常dateStr=%s", s),e);
			}
			String dateStr = format.format(DateUtils.addDays(date, days));
			sb.append(str.substring(i,start));
			sb.append(dateStr);
			i = end;
		}
		sb.append(str.substring(i,str.length()));
		return sb.toString();
	}

	public String replace(String str, String regex, String replacement){
		if(str==null){
			return null;
		}
		String result = str.replaceAll(regex, replacement);
		return result;
	}

	public String replaceFirst(String str, String regex, String replacement){
		if(str==null){
			return null;
		}
		String result = str.replaceFirst(regex, replacement);
		return result;
	}

	public boolean matchs(String str, String regex){
		boolean result = str.matches(regex);
		return result;
	}

	public String eval(String str, Map<String,Object> context){
		String result = null;
		long start = TimeUtil.currentTimeMillis();
		boolean result_tag = true;
		String markKey = StringUtil.substring(String.format("eval-parseConfig-%s", str), 100).replaceAll("[\\\\\"]", " ");
		boolean isMark = false;
		try {
			isMark = TimeTrace.markStart(markKey);
			result = ConfigTemplateUtils.parse(context, str);
			return result;
		} catch (Exception e) {
			result_tag = false;
			throw e;
		} finally{
			long time = TimeUtil.currentTimeMillis()-start;
			if(isMark) {
				TimeTrace.markEnd(markKey,time, result_tag?0L:1L);
			}
		}
	}
	

	/**
	 * 获取指定长度子串
	 * @param s
	 * @param s
	 * @param maxsize 最大尺寸
	 * @return
	 */
	public String substring(Object s, int maxsize){
		return StringUtil.substring(s, maxsize);
	}

	public JSONObject getDoConfigError(){
		Object error = UbagConf.getRequestConf(UbagConf.TemplateConf.template_doconfig_error);
		if(error==null){
			return null;
		}
		Throwable t = (Throwable)error;
		JSONObject result = new JSONObject();
		result.put("className", t.getClass().getName());
		result.put("message", t.getMessage());
		result.put("stackTrace", ExceptionUtils.getStackTrace(t));
		return result;
	}

	public Boolean throwDoConfigError(){
		UbagConf.setRequestConf(UbagConf.TemplateConf.template_throw_doconfig_error, true);
		return true;
	}

	/**
	 * 获取指定名称的环境变量。环境变量配置在maven的各个profile文件中，并且需要在config-common中设置环境变量名称，例如host.merlin, hoset.query, host.ubag, redis.key.prefix, project.id
	 */
	public String getEnv(String name){
		String value = UbagConf.getString(name, null);
		return value;
	}

	//返回指定的系统配置，为了安全，只能设置指定名称的系统属性
	public String setSystemProperty(String key, String value){
		if(key.startsWith("_config_source") || key.startsWith("timer.") || key.startsWith("ubag.") ){
			Object obj = System.setProperty(key, value);
			if(obj==null){
				return null;
			}else{
				return obj.toString();
			}
		}else{
			throw new SourceVerifyException(ErrorCode.COMMONCONFIG_SET_SYSTEM_PROPERTY_ERROR, "设置自定义系统属性，必须以_config_source, timer., ubag.开头");
		}
	}

	//得到所有自定义的系统属性
	public JSONObject getAllSystemPropertys(){
		JSONObject obj = new JSONObject();
		Properties props = System.getProperties();
		for(Entry<Object, Object> entry : props.entrySet()){
			obj.put(entry.getKey().toString(), entry.getValue());
		}
		return obj;
	}

	/**
	 * 对sql进行转义。避免生成的sql语法错误，或者防止SQL注入。比如对输入的%和_和'，就需要进行转义，因为这3个字符是SQL的特殊字符，如果不处理会导致sql出错或者是查询数据不正确。
	 * StringEscapeUtils 不但提供了 SQL 特殊字符转义处理的功能，还提供了 HTML、XML、JavaScript、Java 特殊字符的转义和还原的方法。
	 * @return
	 */
	public String escapeSql(String str){
		String s = StringEscapeUtils.escapeSql(str);
		return s;
	}

	/**
	 * 对html进行转义
	 * @param str
	 * @return
	 */
	public String escapeHtml(String str){
		String s = StringEscapeUtils.escapeHtml(str);
		return s;
	}

	public boolean sleep(long time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new SourceVerifyException(ErrorCode.SERVICE_ERROR, String.format("sleep error, e=", e.getClass()+"-"+e.getMessage()),e);
		}
		return true;
	}

	/**
	 * 将一个逗号分割的字符串，转换为一个tag封装的字符串，例如，str=123,456 tag=" separator=,  则返回"123","456"
	 * 可用于将一个逗号分割的字符串，转换为一个sql中的in字符串
	 * @param str
	 * @param tag
	 * @Paran separator
	 * @return
	 */
	public String warp(String str, String tag, String separator){
		if(str==null || str.length()==0){
			return "";
		}
		StringBuffer result = new StringBuffer();
		//替换中文逗号
		str=str.replaceAll("，", ",");
		String[] strarray = str.split(",");
		for(int i = 0; i< strarray.length; i++){
			result.append(String.format("%s%s%s", tag, strarray[i].trim(), tag));
			if(i!=strarray.length-1){
				result.append(separator);
			}
		}
		return result.toString();
	}

	/**
	 * 分割字符串，默认按照空白字符分割，空白字符可以是多个空格、tab等，和shell的工具类似
	 * @param str
	 * @return
	 */
	public String[] split(String str){
		if(str==null){
			return new String[0];
		}
		//过滤前后空格
		return this.split(str.trim(), "\\s+", Integer.MAX_VALUE);
	}

	/**
	 * 分割字符串
	 * @param str
	 * @param regex 分隔符
	 * @return
	 */
	public String[] split(String str, String regex, int limit){
		if(str==null || str.length()==0){
			return new String[0];
		}
		String[] arr = str.trim().split(regex, limit);
		return arr;
	}

	/**
	 * 将参数queryParams按照key排序，并连接为a=1&b=123形式。常用于接口签名钱排序参数。
	 * @param queryParams    参数, 参数中的值必须 URLEncoder.encode(value, "UTF-8")
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String sortSignParam(Object queryParams) {
		if(queryParams==null){
			return null;
		}
		JSONObject params = this.toJsonObject(queryParams);
        //排序的参数中不应该包含sign参数
        if(params.containsKey("sign")){
        	params.remove("sign");
        }
        if(params.isEmpty()){
            return null;
        }
        String[] keys = params.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        StringBuilder sb = new StringBuilder();
        for(String key : keys){
            String value = params.getString(key);
            if(StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value)){
                sb.append(key).append("=").append(value).append("&");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

	/**
	 * 根据参数进行签名
	 * @param queryParams
	 * @param secretKey
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getSign(Object queryParams, String secretKey){
		String sortParam = this.sortSignParam(queryParams);
		sortParam = sortParam + secretKey;
		String sign = this.md5(sortParam);
		return sign;
	}

	public JSONArray listToJsonArray(Object collName, Object... fileList){
		if(fileList.length <= 0) return null;
		JSONArray collJsonArray = new JSONArray();

		for(Object o:fileList){
			JSONObject jo = TemplateUtil.instance.toJsonObject(o);
			if(jo.containsKey(collName)) collJsonArray.addAll(jo.getJSONArray(collName.toString()));
		}
		return collJsonArray;
	}

	//按照 columnOrder 顺序组装成 object 二维数组  commonColumn 字段比较 也在排序字段中
	public static Object[][] toObjectArray(Object arrayObj,String columnOrder,Object commonObj){
		if(arrayObj == null) return new Object[0][0];
		JSONArray jsonArray = TemplateUtil.instance.toJsonArray(arrayObj);
		JSONObject commonObject = TemplateUtil.instance.toJsonObject(commonObj);

		String[] columns = columnOrder.split(",",-1);
		int x = columns.length;
		int y = jsonArray.size();
		Object[][]  objects =  new Object[y][x];
		for(int i = 0;i < y;i++){
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			Object[] columnObj = new Object[x];
			for(int j = 0;j < x ; j++){
				 Object data = jsonObject.get(columns[j]);
				 if(data == null && commonObject != null){
					  data = commonObject.get(columns[j]);
				 }
				columnObj[j] = data;
			}
			objects[i] = columnObj;
		}
		return objects;
	}
	
	/**
     * 向 URL 追加单个参数（自动处理分隔符和编码）
     *
     * @param originalUrl 原始 URL（可能含查询参数、锚点）
     * @param paramName   参数名（需非空）
     * @param paramValue  参数值（null 会被转为空字符串）
     * @return 追加参数后的完整 URL
     */
    public String appendParam(String originalUrl, String paramName, String paramValue) {
        // 校验必填参数
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("原始URL不能为空");
        }
        if (paramName == null || paramName.isBlank()) {
            throw new IllegalArgumentException("参数名不能为空");
        }

        // 参数值为空时处理为空白字符串
        String value = paramValue == null ? "" : paramValue;

        // 1. 编码参数名和值（避免特殊字符如 & ? # 等破坏URL结构）
        String encodedName = URLEncoder.encode(paramName, StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String newParam = encodedName + "=" + encodedValue;

        // 2. 拆分 URL 为 基础URL + 锚点（# 后面的内容不参与参数拼接）
        int anchorIndex = originalUrl.indexOf("#");
        String baseUrl = originalUrl;
        String anchor = "";
        if (anchorIndex != -1) {
            baseUrl = originalUrl.substring(0, anchorIndex);
            anchor = originalUrl.substring(anchorIndex); // 保留锚点（含 #）
        }

        // 3. 判断基础URL是否已有查询参数（含 ?）
        if (baseUrl.contains("?")) {
            // 已有查询参数：判断是否以 & 结尾，避免重复拼接
            if (baseUrl.endsWith("&")) {
                baseUrl += newParam;
            } else {
                baseUrl += "&" + newParam;
            }
        } else {
            // 无查询参数：直接用 ? 拼接
            baseUrl += "?" + newParam;
        }

        // 4. 拼接锚点，返回最终URL
        return baseUrl + anchor;
    }

	public static void main(String[] args) throws Exception {
		TemplateUtil t = TemplateUtil.getInstance();

		JSONObject obj = new JSONObject();
		obj.put("errno", "0");
		obj.put("a", 10);
		obj.put("b", 22.12);
		obj.put("c", "33");
		System.out.println("--ee" + Integer.valueOf("144 ".trim()));
		t = TemplateUtil.getInstance();
		String assss = "[{\"ext\":{\"user_id\":12321321}},{\"ext\":{\"user_id\":11112321321}}]";
		
		System.out.println(t.isDouble("0"));
		System.out.println(t.isDouble("0.1"));
		System.out.println(t.isDouble("100"));
		System.out.println(t.isDouble("-1"));
		
		
		// 测试场景1：无查询参数、无锚点
        String url1 = "https://api.example.com/path";
        System.out.println(t.appendParam(url1, "name", "zhangsan_books"));
        // 输出：https://api.example.com/path?name=%E5%BC%A0%E4%B8%89

        // 测试场景2：已有查询参数、无锚点
        String url2 = "https://api.example.com/path?age=20";
        System.out.println(t.appendParam(url2, "city", "北京"));
        // 输出：https://api.example.com/path?age=20&city=%E5%8C%97%E4%BA%AC

        // 测试场景3：已有查询参数且以 & 结尾
        String url3 = "https://api.example.com/path?page=1&";
        System.out.println(t.appendParam(url3, "size", "10"));
        // 输出：https://api.example.com/path?page=1&size=10

        // 测试场景4：含锚点（# 后内容不影响参数）
        String url4 = "https://api.example.com/path?a=1#detail";
        System.out.println(t.appendParam(url4, "b", "2"));
        // 输出：https://api.example.com/path?a=1&b=2#detail

        // 测试场景5：参数含特殊字符（&、空格等）
        String url5 = "https://api.example.com/search";
        System.out.println(t.appendParam(url5, "query", "java & elasticsearch"));
        // 输出：https://api.example.com/search?query=java+%26+elasticsearch


	}
}

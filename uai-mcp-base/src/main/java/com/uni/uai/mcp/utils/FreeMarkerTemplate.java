package com.uni.uai.mcp.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import freemarker.core.*;

import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.exception.ErrorCode;
import com.uni.ubag.common.exception.SourceVerifyException;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

public class FreeMarkerTemplate {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static Configuration config = new Configuration();
	static {
		// 如果小数点后多余两位，就只保留两位，否则输出实际值
		config.setNumberFormat("#.##");
		config.setDefaultEncoding("utf8");
		//禁用本地化查找
		config.setLocalizedLookup(false);
		//安全模式，禁止执行系统命令
		config.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
		// 设置模板加载器（从 Classpath 加载）
		config.setClassForTemplateLoading(FreeMarkerTemplate.class, "/");
		//异常处理器
		//config.setTemplateExceptionHandler(new MyTemplateExceptionHandler());
	}

	public static String parse(Map<String, Object> contextMap, String text) {
		try {
			StringWriter writer = new StringWriter();
			Template template = getTemplate(text);
			template.process(contextMap, writer);
			String result = writer.toString();
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), e.getClass(), "FreeMarkTemplate.parse() writer.close()异常", null, false, 0L, ExceptionUtil.toStackTrace(e));
			}
			return result;
		} catch (Exception e) {
			//由于在模版中，还会嵌套调用模版，例如，在模版中，调用loop.list_filter，
			//如果嵌套模版调用抛出异常，也会被此处的catch捕获。会导致嵌套的异常被外层异常再包括一层，使得只能通过e.getCause后才能知道模版解析异常的真正原因。
			//为了能清楚看到最源头的异常，则只抛出最源头异常
			String errdesc = "";
			String errormsg = e.getClass()+"-"+e.getMessage();
			//freemark会将模版方法中抛出的异常进行封装，然后再抛出，
			//例如，调用在s.getAndParseConfig()中调用t.check()抛出的SourceVerifyException异常，会封装到一个TemplateModelException异常中。
			//导致在此处看不到异常的源头，所有，对这些异常的cause做统一处理。
			SourceVerifyException e2 = ExceptionUtil.getSourceVerifyException(e);
			
			//不要在此处处理normalreturn异常，而应放在最外层处理，因为init、merge都是单独parse，如果init抛出normalreturn异常，被parse处理了，merge模块还能正常执行，这不是期望的结果。
			//期望是init中抛出的normal return，在整个doconfig处理。
			//如果希望parse时也处理normalreturn，可调用u.parseUserConfig()，此方法也和doConfig()一样，会处理normalreturn
			//String normalResultResult = ExceptionUtil.checkNormalReturn(e2);
			//if(normalResultResult!=null) {
			//	return normalResultResult;
			//}
			
			if(e2!=null){
				e2.addViewMsgHead("");
				throw e2;
			}
			//对于解析中的其他异常
			if(e instanceof InvalidReferenceException){
				errdesc = "非法引用异常";
			}else if(e instanceof UnexpectedTypeException){
				errdesc = "不是所期望的类型异常";
			}else if(e instanceof TemplateModelException){
				errdesc = "模版模型异常, 或模版方法调用时异常";
			}else if(e instanceof ParseException){
				errdesc = "Parse模版时，模版词法异常";
			}else if(e instanceof TemplateException){
				errdesc = "其他模版异常";
			}else{
				errdesc = "非TemplateException异常";
			}
			if(e.getCause()!=null && !"".equals(e.getCause())){
				Throwable th = e.getCause();
				errdesc = String.format(errdesc + ", cause by %s", th);
			}
			//logger.warn(String.format("配置模版解析时异常. text=%s", text), e);
			SourceVerifyException ex = new SourceVerifyException(ErrorCode.COMMONCONFIG_TEMPLATE_ERROR,
					String.format("配置模版解析时异常. %s, e=%s, text=%s", errdesc, errormsg, text),e);
			//由于封装后的SourceVerifyException，丢失了堆栈信息，例如，如果模版中调用了t.***()方法，如果此方法抛出空指针，则看不到堆栈信息，无法定位是哪一行抛出的
			//所以，为了可以通过commonlog日志酒呢个排查问题，将异常堆栈打印到commonlog中
			UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), e.getClass(), "FreeMarkTemplate.parse()执行异常", null, false, 0L, ExceptionUtil.toStackTrace(e));
			throw new RuntimeException(String.format("parse异常: text=%s, context=%s", text, contextMap), e);
		}
	}
	
	private static Template createTemplate(String text) throws IOException{
		StringReader reader = new StringReader(text);
		Template template = new Template("", reader, config);
		return template;
	}
	
	private static Template getTemplate(String text) throws IOException{
		Template template = createTemplate(text);
		return template;
    }
	
	public static void main(String[] args) throws IOException, TemplateException {
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("list1", new String[] { "a", "b" });
		root.put("param1", "hello");
		root.put("a", 11111111111.123);
		root.put("b", 122.222);
		String text = "--中国-${list1[0]}---$param1---${a-b}";
		String result = parse(root, text);
		System.out.println("output: " + result);
		
		text = "---${datas[1].data}";
		result = parse(root, text);
		System.out.println("output: " + result);
		
		//空引用处理：
		//root.put("aa", 222);
		text = "-----${(aa[0].abc)!a}";
		result = parse(root, text);
		System.out.println("output: " + result);
		

	}
}

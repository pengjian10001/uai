package com.uni.uai.mcp.utils.context;

import java.util.HashMap;
import java.util.Map;

import com.uni.uai.mcp.utils.ConfigTemplateUtils;
import com.uni.uai.mcp.utils.FreeMarkerTemplate;
import com.uni.uai.mcp.utils.JSONUtil;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.FileUtil;

/**
 * 扩展的TemplateUtil
 *
 * @author pengjian
 */
public class TemplateExtendUtil {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	public static TemplateExtendUtil instance = new TemplateExtendUtil();
	
	public static TemplateExtendUtil getInstance() {
		return instance;
	}

	public static String parse(Object context, String localPath) {
		Map<String, Object> contextMap = JSONUtil.getInstance().toJsonObject(context);
		UbagConf.setAllRequestConf(contextMap);
		String text = FileUtil.getInstance().getResourceContent(localPath);
		String result = ConfigTemplateUtils.parse(contextMap, text);
        return result;
    }
}

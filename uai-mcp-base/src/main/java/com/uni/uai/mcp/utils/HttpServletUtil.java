package com.uni.uai.mcp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSONObject;
import com.uni.uai.mcp.common.FileInfo;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.ubag.common.conf.UbagConf;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.common.util.RegexUtil;
import com.uni.ubag.log.util.UbagLogUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;


public class HttpServletUtil {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final String METHOD_POST = "POST";
	private static HttpServletUtil instance = new HttpServletUtil();
	public static HttpServletUtil getInstance() {
		return instance;
	}
	
	/**
	 * 此方法是读取流，只能调用一次
	 * @param req
	 * @return
	 */
	public Map<String, Object> handlePostJsonParam(HttpServletRequest req) {
		Map<String, Object> param = new HashMap<String, Object>();
		String method = req.getMethod();
		String contentType = req.getContentType();
		if (method.equals(METHOD_POST)
				&& contentType != null && contentType.contains("application/json")) {
			//如果时postjson请求，则从请求体中读取
			try {
				BufferedReader reader = req.getReader();
				StringBuilder body = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					body.append(line);
				}
				if(body.length() > 0) {
					JSONObject jsonObject = JSONUtil.toJsonObject(body.toString());
					if(jsonObject!=null) {
						param.putAll(jsonObject);
						UbagConf.setAllRequestConf(jsonObject);
					}
				}
			}
			catch (Exception e) {
				String msg = String.format("servlet处理参数异常", e.getClass()+ "-" + e.getMessage());
				UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), e.getClass().getName(), ExceptionUtil.toStackTrace(e), e, false, 0L, msg);
			} 
		}
		this.setParamToUbag(param);
		return param;
	}
	
	public Map<String, Object> handleParam(HttpServletRequest request){
		JSONObject param = new JSONObject();
		Map<String, String[]> map = request.getParameterMap();
		if(map!=null && map.size() > 0) {
			for(String key : map.keySet()) {
				String[] values = map.get(key);
				if(values != null) {
					Object v = null;
					if(values.length == 1) {
						v = values[0];
					}else {
						v = values;
					}
					param.put(key, v);
				}
			}
		}
		this.setParamToUbag(param);
		return param;
	}
	
	public Map<String, Object> handlePostFileParam(HttpServletRequest request, String fileLocation) throws IOException, ServletException{
		JSONObject param = new JSONObject();
		List<FileInfo> uploadedFiles = handleFileUpload(request, fileLocation);
        if (!uploadedFiles.isEmpty()) {
             logger.info("上传文件数量: {}, 文件名: {}", 
                     uploadedFiles.size(), 
                     uploadedFiles.stream().map(FileInfo::getFileName).collect(Collectors.joining(",")));
        }
        param.put(UaiConf.FILE_INFO_LIST_NAME, uploadedFiles);
		this.setParamToUbag(param);
		return param;
	}
	
    /**
     * 处理文件上传
     * @param request
     * @param fileLocation 文件存储的临时目录
     * @return
     * @throws IOException
     * @throws ServletException
     */
    private List<FileInfo> handleFileUpload(HttpServletRequest request, String fileLocation) throws IOException, ServletException {
        List<FileInfo> fileInfos = new ArrayList<>();

        // 获取所有上传的文件
        for (Part part : request.getParts()) {
            // 只处理文件类型的part
            if (part.getContentType() != null) {
                String fileName = getFileName(part);
                if (fileName == null || fileName.isEmpty()) {
                    continue;
                }

                // 创建临时文件
                File tempFile = File.createTempFile(
                        "upload_" + System.currentTimeMillis() + "_",
                        getFileExtension(fileName),
                        new File(fileLocation)
                );

                // 写入文件内容
                try (InputStream input = part.getInputStream();
                     OutputStream output = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                // 记录文件信息
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(fileName);
                fileInfo.setFileSize(part.getSize());
                fileInfo.setContentType(part.getContentType());
                fileInfo.setTempFilePath(tempFile.getAbsolutePath());
                fileInfos.add(fileInfo);

                // 标记临时文件为JVM退出时删除
                tempFile.deleteOnExit();
            }
        }

        return fileInfos;
    }

    /**
     * 从Part中获取文件名
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

	private void setParamToUbag(Map<String, Object> param) {
		if(param != null && param.size() > 0) {
			for(String key : param.keySet()) {
				Object value = param.get(key);
				UbagConf.setRequestConf(key, value);
				UbagConf.setRequestMapParam(UbagConf.WebConf.request_user_param, key, value);
			}
		}
	}
	
	public static void main(String[] args) {
		String url = "http://localhost:8080/pages/mcp/chat/page_query_get_ai_response";
		List<String> list = RegexUtil.findMatchs(url, "(http[s]?://[^/]+)/.*", 1);
		System.out.println(list);
	}
}

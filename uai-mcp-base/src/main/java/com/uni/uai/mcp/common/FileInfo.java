package com.uni.uai.mcp.common;

import com.uni.ubag.common.util.JSONUtil;

/**
 * 文件信息封装类

	jakarta.servlet.http.Part 中的 getContentType() 方法用于返回上传文件的 MIME 类型（媒体类型），其返回值取决于客户端请求中指定的文件类型，并没有固定的 "所有可能返回值列表"，因为 MIME 类型可以是任意有效的媒体类型。
	该方法的返回遵循以下规则：
	对于上传的文件，返回值通常是客户端浏览器根据文件扩展名或内容推断的 MIME 类型
	常见的返回值示例包括（但不限于）：
	文本文件：text/plain
	    HTML 文件：text/html
	    CSS 文件：text/css
	    JavaScript 文件：application/javascript
	    JSON 数据：application/json
	图片文件：
	    JPEG：image/jpeg
	    PNG：image/png
	    GIF：image/gif
	    BMP：image/bmp
	    SVG：image/svg+xml
	音频文件：
	    MP3：audio/mpeg
	    WAV：audio/wav
	视频文件：
	    MP4：video/mp4
	    AVI：video/x-msvideo
	文档文件：
	    PDF：application/pdf
	    Microsoft Word：application/msword 或 application/vnd.openxmlformats-officedocument.wordprocessingml.document
	    Microsoft Excel：application/vnd.ms-excel 或 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
	压缩文件：
	    ZIP：application/zip
	    GZIP：application/gzip
	特殊情况：
	    如果无法确定 MIME 类型，可能返回 application/octet-stream（二进制流）
	对于非文件类型的表单字段，可能返回 null 或特定的文本类型
	
	注意，客户端可以伪造 MIME 类型，因此在服务器端不应仅依赖此方法来验证文件类型，还应结合文件内容检测等方式进行安全验证。
	
 */
public class FileInfo {
    private String fileName;
    private long fileSize;
    private String contentType;
    private String tempFilePath; //上传文件在服务端存储的临时文件地址

    // getter和setter
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
}

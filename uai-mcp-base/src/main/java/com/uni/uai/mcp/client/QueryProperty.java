package com.uni.uai.mcp.client;

import java.util.ArrayList;
import java.util.List;

import com.uni.uai.mcp.common.FileInfo;
import com.uni.ubag.common.util.JSONUtil;

public class QueryProperty {
	//客户端名称
	String clientName;
	//会话id
	String sessionId;
	//用户id
	String ucid;
	//上传文件的信息
	List<FileInfo> fileInfoList = new ArrayList<FileInfo>();
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public String getUcid() {
		return ucid;
	}
	public void setUcid(String ucid) {
		this.ucid = ucid;
	}
	public List<FileInfo> getFileInfoList() {
		return fileInfoList;
	}
	public void setFileInfoList(List<FileInfo> fileInfoList) {
		this.fileInfoList = fileInfoList;
	}
	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
}

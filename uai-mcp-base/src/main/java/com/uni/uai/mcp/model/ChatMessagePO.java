package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class ChatMessagePO {
	private Long id;
	//会话id
	private String sessionId;
	private String content;
	private String type;
	//单次对话id
	private String singleId;
	
	private Integer state;
	private java.sql.Timestamp mtime;
	
	public java.sql.Timestamp getMtime() {
		return mtime;
	}

	public void setMtime(java.sql.Timestamp mtime) {
		this.mtime = mtime;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSingleId() {
		return singleId;
	}

	public void setSingleId(String singleId) {
		this.singleId = singleId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	

}

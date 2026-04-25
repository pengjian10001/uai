package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class LabelToolPO {
	private Long id;
	private Long labelId;
	private Long toolId;
	private String labelName;
	private String toolName;
	
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

	public Long getLabelId() {
		return labelId;
	}

	public void setLabelId(Long labelId) {
		this.labelId = labelId;
	}

	public Long getToolId() {
		return toolId;
	}

	public void setToolId(Long toolId) {
		this.toolId = toolId;
	}

	public String getLabelName() {
		return labelName;
	}

	public void setLabelName(String labelName) {
		this.labelName = labelName;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	

}

package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class LabelPO {
	private Long id;
	private Integer type;
	private String name;
	private String description;
	private String ext;
	private Long parentId;
	
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

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	

}

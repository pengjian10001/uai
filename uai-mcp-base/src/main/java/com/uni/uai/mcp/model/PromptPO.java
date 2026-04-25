package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class PromptPO {
	private Long id;
	private String model;
	private Integer type;
	private String name;
	private String description;
	private String paramSchema;
	private String version;
	
	private String promptTemplate;
	private String promptConfig;
	
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
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

	public String getParamSchema() {
		return paramSchema;
	}

	public void setParamSchema(String paramSchema) {
		this.paramSchema = paramSchema;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}


	public String getPromptTemplate() {
		return promptTemplate;
	}

	public void setPromptTemplate(String promptTemplate) {
		this.promptTemplate = promptTemplate;
	}

	public String getPromptConfig() {
		return promptConfig;
	}

	public void setPromptConfig(String promptConfig) {
		this.promptConfig = promptConfig;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	
	public static class PromptConfig{
		String role;

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}
	}
	

}

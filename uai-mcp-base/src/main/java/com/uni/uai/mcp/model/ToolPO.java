package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class ToolPO {
	private Long id;
	private String model;
	private Integer type;
	private String name;
	private String description;
	private String paramSchema;
	//工具返回值类的源码
	private String returnClass;
	//处理工具返回值的脚本
	private String returnScript;
	private String version;
	
	private String dataSourceDesc;
	private String dataSourceConfig;
	
	private Integer state;
	private java.sql.Timestamp mtime;
	
	public static ToolPO createToolPO(String name, String description, String schema, String returnClass, String returnScript) {
		ToolPO po = new ToolPO();
		po.setName(name);
		po.setDescription(description);
		po.setParamSchema(schema);
		po.setReturnClass(returnClass);
		po.setReturnScript(returnScript);
		po.setState(0);
		// 获取当前日期
		java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
		po.setMtime(timestamp);
		return po;
	}
	
	public java.sql.Timestamp getMtime() {
		return mtime;
	}

	public void setMtime(java.sql.Timestamp mtime) {
		this.mtime = mtime;
	}

	public String getReturnClass() {
		return returnClass;
	}

	public void setReturnClass(String returnClass) {
		this.returnClass = returnClass;
	}

	public String getReturnScript() {
		return returnScript;
	}

	public void setReturnScript(String returnScript) {
		this.returnScript = returnScript;
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

	public String getDataSourceDesc() {
		return dataSourceDesc;
	}

	public void setDataSourceDesc(String dataSourceDesc) {
		this.dataSourceDesc = dataSourceDesc;
	}

	public String getDataSourceConfig() {
		return dataSourceConfig;
	}

	public void setDataSourceConfig(String dataSourceConfig) {
		this.dataSourceConfig = dataSourceConfig;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	

}

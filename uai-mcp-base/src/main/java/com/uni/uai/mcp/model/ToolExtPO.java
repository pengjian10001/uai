package com.uni.uai.mcp.model;

import com.uni.ubag.common.util.JSONUtil;

public class ToolExtPO {
	private Long id;
	private String name;
	//工具返回值类的源码
	private String returnClass;
	//returnClass编译后得到的Class，不存于数据库，只存于内存
	private Class<?> returnClassAfterCompiler;
	
	public static ToolExtPO createToolPO(Long id, String name, String returnClass, Class<?> returnClassAfterCompiler) {
		ToolExtPO po = new ToolExtPO();
		po.setId(id);
		po.setName(name);
		po.setReturnClass(returnClass);
		po.setReturnClassAfterCompiler(returnClassAfterCompiler);
		return po;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReturnClass() {
		return returnClass;
	}

	public void setReturnClass(String returnClass) {
		this.returnClass = returnClass;
	}

	public Class<?> getReturnClassAfterCompiler() {
		return returnClassAfterCompiler;
	}

	public void setReturnClassAfterCompiler(Class<?> returnClassAfterCompiler) {
		this.returnClassAfterCompiler = returnClassAfterCompiler;
	}

	@Override
	public String toString() {
		return JSONUtil.toJSONString(this);
	}
	

}

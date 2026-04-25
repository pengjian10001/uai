package com.uni.uai.mcp.utils.complier;

public class CompilerUtil {
	SafeDynamicCompiler compiler = new SafeDynamicCompiler();
	private static CompilerUtil instance = new CompilerUtil();
	public static CompilerUtil getInstance() {
		return instance;
	}
	
	public Class<?> compile(String className, String sourceCode) throws Exception {
		Class<?> clazz = compiler.compileAndLoad(className, sourceCode);
		return clazz;
	}
	
	
	
}

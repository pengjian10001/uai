package com.uni.uai.mcp.agent.example2;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;

public class TestNonAIAgent extends TestBase {

	public static void main(String[] args) {
		// 初始化取款Agent（AI Agent，关联银行工具）
		WithdrawAgent withdrawAgent = AgenticServices
		        .agentBuilder(WithdrawAgent.class)
		        .chatModel(BASE_MODEL) // 配置基础大语言模型
		        //.tools(bankTool) // 绑定银行操作工具
		        .build();

		// 初始化存款Agent（AI Agent，关联银行工具）
		CreditAgent creditAgent = AgenticServices
		        .agentBuilder(CreditAgent.class)
		        .chatModel(BASE_MODEL)
		        //.tools(bankTool)
		        .build();

		// 构建银行监控Agent（混合AI Agent与非AI Agent）
		SupervisorAgent bankSupervisor = AgenticServices
		        .supervisorBuilder()
		        .chatModel(BASE_MODEL) // 配置规划器所需的大语言模型
		        .subAgents(withdrawAgent, creditAgent, new ExchangeOperator()) // 子Agent：取款、存款、货币兑换（非AI）
		        .build();
		
		bankSupervisor.invoke("退款");


	}

}

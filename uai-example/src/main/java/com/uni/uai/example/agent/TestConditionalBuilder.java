package com.uni.uai.example.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
/**
 * 条件工作流
 */
public class TestConditionalBuilder extends TestBase{

	public static void main(String[] args) {
		// 初始化各Agent
		CategoryRouter routerAgent = AgenticServices
		        .agentBuilder(CategoryRouter.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("category") // 分类结果存入共享变量category
		        .build();

		MedicalExpert medicalExpert = AgenticServices
		        .agentBuilder(MedicalExpert.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("response") // 医疗解答存入共享变量response
		        .build();
		LegalExpert legalExpert = AgenticServices
		        .agentBuilder(LegalExpert.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("response") // 法律解答存入共享变量response
		        .build();
		// 构建条件工作流：根据分类结果调用对应的专家Agent
		UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
		        // 条件1：分类为医疗类，调用医疗专家Agent
		        //**参见后面API，conditionalBuilder()返回ConditionalAgentService，其重载了多个subAgents()方法，其中subAgents(Predicate<AgenticScope> condition, Object... agents)，用于配置“满足条件时，执行多个子智能体”
		        .subAgents( agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
		        // 条件2：分类为法律类，调用法律专家Agent
		        .subAgents( agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
		        .build();

		// 构建顺序工作流：先分类，再调用对应专家
		ExpertRouterAgent expertRouterAgent = AgenticServices
		        .sequenceBuilder(ExpertRouterAgent.class)
		        .subAgents(routerAgent, expertsAgent) // 先分类，再处理
		        .outputKey("response") //**此处简单的将子Agent的输出作为输出。如果需要复杂逻辑，则需定义output()
		        .build();

		// 调用专家路由Agent，处理医疗类请求
		String response = expertRouterAgent.ask("我摔断了腿，应该怎么办？");

	}
}

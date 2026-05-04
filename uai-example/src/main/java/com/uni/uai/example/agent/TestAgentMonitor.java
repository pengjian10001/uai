package com.uni.uai.example.agent;

import java.nio.file.Path;
import java.util.Map;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.observability.MonitoredExecution;

/**
 * 测试监视器，实现Agent调用的可视化
 */
public class TestAgentMonitor extends TestBase{
	public static void main(String[] args) {
		// 初始化Agent监控器
		AgentMonitor monitor = new AgentMonitor();

		// 初始化创意写作Agent，并注册监听器（仅监听调用前）
		CreativeWriter creativeWriter = AgenticServices.agentBuilder(CreativeWriter.class)
		        .listener(new AgentListener() {
		            @Override
		            public void beforeAgentInvocation(AgentRequest request) {
		                // 仅在创意写作Agent调用前，输出主题日志
		                System.out.println("调用创意写作Agent，主题：" + request.inputs().get("topic"));
		            }
		        })
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		// 初始化风格编辑Agent
		StyleEditor styleEditor = AgenticServices.agentBuilder(StyleEditor.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		// 初始化风格评分Agent（指定名称）
		StyleScorer styleScorer = AgenticServices.agentBuilder(StyleScorer.class)
		        .name("styleScorer") // 指定Agent名称，用于监听器判断
		        .chatModel(BASE_MODEL)
		        .outputKey("score")
		        .build();

		// 构建循环工作流（评分+编辑，直到评分≥0.8或达到最大迭代次数）
		UntypedAgent styleReviewLoop = AgenticServices.loopBuilder()
		        .subAgents(styleScorer, styleEditor)
		        .maxIterations(5) // 最大迭代次数
		        .exitCondition(agenticScope -> agenticScope.readState("score", 0.0) >= 0.8) // 退出条件
		        .build();

		// 构建顶层顺序工作流（生成故事+循环优化），注册多个监听器
		UntypedAgent styledWriter = AgenticServices.sequenceBuilder()
		        .subAgents(creativeWriter, styleReviewLoop)
		        .listener(monitor) // 注册Agent监控器，跟踪所有Agent调用
		        .listener(new AgentListener() {
		            @Override
		            public void afterAgentInvocation(AgentResponse response) {
		                // 仅当调用的是styleScorerAgent时，输出当前评分
		                if (response.agentName().equals("styleScorer")) {
		                    System.out.println("当前风格评分：" + response.output());
		                }
		            }
		        })
		        .outputKey("story")
		        .build();
		
		//当按如下方式调用styledWriterAgent时：
		// 定义输入参数（主题：龙与巫师，风格：喜剧）
		Map<String, Object> input = Map.of(
		        "topic", "龙与巫师",
		        "style", "喜剧");
		// 执行工作流，获取最终故事
		String story = (String) styledWriter.invoke(input);
		//AgentMonitor会将所有Agent调用记录在树形结构中，该结构还会跟踪每次Agent调用的开始时间、结束时间、持续时间、令牌数、输入和输出。此时，可以从监控器中获取记录的执行信息，并例如打印到控制台进行查看。
		// 获取第一个成功的执行记录
		MonitoredExecution execution = monitor.successfulExecutions().get(0);
		// 打印执行记录（树形结构）
		System.out.println(execution);

		//最后，通过HtmlReportGenerator类提供的静态generateReport方法，还可以为AgentMonitor收集的数据（Agent系统拓扑和记录的执行信息）生成可视化的HTML报告。例如，为上述执行记录生成报告：
		// 生成HTML报告，保存到当前工作目录的review-loop.html文件中
		HtmlReportGenerator.generateReport(monitor, Path.of("review-loop.html"));


	}
}

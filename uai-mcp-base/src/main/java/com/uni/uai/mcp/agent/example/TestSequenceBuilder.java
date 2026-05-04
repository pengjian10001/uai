package com.uni.uai.mcp.agent.example;

import java.util.Map;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
/**
 * 顺序工作流
 */
public class TestSequenceBuilder extends TestBase {
	public static void main(String[] args) {
		// 1. 初始化三个子Agent（假设BASE_MODEL是已配置好的基础对话模型）
		CreativeWriter creativeWriter = AgenticServices
		        .agentBuilder(CreativeWriter.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		AudienceEditor audienceEditor = AgenticServices
		        .agentBuilder(AudienceEditor.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		StyleEditor styleEditor = AgenticServices
		        .agentBuilder(StyleEditor.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();
		// 2. 构建顺序工作流（无类型Agent版本）
		UntypedAgent novelCreator = AgenticServices
		        .sequenceBuilder()
		        .subAgents(creativeWriter, audienceEditor, styleEditor) // 按顺序调用子Agent
		        .outputKey("story")  //**此例输出key均为story，即story会被Agent依次改写
		        .build();
		// 3. 定义输入参数并执行工作流
		Map<String, Object> input = Map.of(
		        "topic", "龙与巫师",
		        "style", "奇幻",
		        "audience", "青少年"
		);

		// 执行工作流，获取最终编辑后的故事。
		//**UntypedAgent有一个invoke方法，返回Object类型，此处强制转换为String
		String story = (String) novelCreator.invoke(input);

	}

}

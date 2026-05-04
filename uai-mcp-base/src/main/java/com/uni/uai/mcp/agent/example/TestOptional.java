package com.uni.uai.mcp.agent.example;

import java.util.Map;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * 可选Agent
 */
public class TestOptional extends TestBase{
	public static void main(String[] args) {
		// 初始化创意写作Agent
		CreativeWriter creativeWriter = AgenticServices
		        .agentBuilder(CreativeWriter.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		// 初始化受众编辑Agent，并设置为可选
		AudienceEditor audienceEditor = AgenticServices
		        .agentBuilder(AudienceEditor.class)
		        .chatModel(BASE_MODEL)
		        .optional(true) // 标记为可选Agent
		        .outputKey("story")
		        .build();

		// 初始化风格编辑Agent
		StyleEditor styleEditor = AgenticServices
		        .agentBuilder(StyleEditor.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();
		
		// 构建顺序工作流（包含可选Agent）
		UntypedAgent novelCreator = AgenticServices
		        .sequenceBuilder()
		        .subAgents(creativeWriter, audienceEditor, styleEditor)
		        .outputKey("story")
		        .build();

		// 输入中未提供"audience"（受众）参数，因此受众编辑Agent会被跳过
		Map<String, Object> input = Map.of(
		        "topic", "龙与巫师",
		        "style", "奇幻"
		);

		// 执行工作流，仍能正常生成并编辑故事
		String story = (String) novelCreator.invoke(input);


	}
}

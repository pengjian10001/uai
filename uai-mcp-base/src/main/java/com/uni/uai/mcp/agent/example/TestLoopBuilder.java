package com.uni.uai.mcp.agent.example;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
/**
 * 循环工作流
 */
public class TestLoopBuilder extends TestBase{

	public static void main(String[] args) {
		// 初始化风格编辑Agent和风格评分Agent（假设BASE_MODEL是已配置好的基础对话模型）
		StyleEditor styleEditor = AgenticServices
		        .agentBuilder(StyleEditor.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		StyleScorer styleScorer = AgenticServices
		        .agentBuilder(StyleScorer.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("score") // 评分结果存入共享变量score
		        .build();

		//配置好这个styleReviewLoop循环工作流后，它可以被视为一个独立的Agent（**类似langgraph4j中的子图），与创意写作Agent（CreativeWriter）组合成一个顺序工作流，构建出StyledWriter（风格化写作Agent）
		UntypedAgent styleReviewLoop = AgenticServices
		        .loopBuilder()
		        .subAgents(styleScorer, styleEditor)
		        .maxIterations(5)
		        .testExitAtLoopEnd(true) // 仅在一轮循环结束后检查退出条件
		        .exitCondition( (agenticScope, loopCounter) -> {
		            double score = agenticScope.readState("score", 0.0);
		            // 前3次迭代要求评分≥0.8，之后降低要求至≥0.6
		            return loopCounter <= 3 ? score >= 0.8 : score >= 0.6;
		        })
		        .build();


		//该Agent实现了一个更复杂的工作流，融合了故事生成和风格审核优化的完整流程。
		// 初始化创意写作Agent
		CreativeWriter creativeWriter = AgenticServices
		        .agentBuilder(CreativeWriter.class)
		        .chatModel(BASE_MODEL)
		        .outputKey("story")
		        .build();

		// 构建顺序工作流：先生成故事，再循环优化风格
		StyledWriter styledWriter = AgenticServices
		        .sequenceBuilder(StyledWriter.class)
		        .subAgents(creativeWriter, styleReviewLoop) // 先调用创作Agent，再调用循环工作流
		        .outputKey("story")
		        .build();

		// 调用风格化写作Agent，生成符合指定风格的故事
		String story = styledWriter.writeStoryWithStyle("龙与巫师", "喜剧");

	}

}

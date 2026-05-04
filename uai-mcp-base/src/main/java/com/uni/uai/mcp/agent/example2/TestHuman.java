package com.uni.uai.mcp.agent.example2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;

public class TestHuman extends TestBase {
	public static void main(String[] args) throws Exception{
		// 初始化人机交互Agent：询问用户的星座
		HumanInTheLoop humanInTheLoop = AgenticServices.humanInTheLoopBuilder()
		        .description("向用户询问其星座的Agent")
		        .outputKey("sign") // 用户响应（星座）存入共享变量sign。
		        .responseProvider(scope -> {
		            // 从AgenticScope中读取用户姓名，拼接询问语句并输出到控制台
		            System.out.println("你好 " + scope.readState("name") + "，请问你的星座是什么？");
		            System.out.print("请输入：");
		            try {
		                // 从标准输入读取用户响应
		                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		                return reader.readLine();
		            } catch (IOException e) {
		                throw new RuntimeException("读取用户输入失败", e);
		            }
		        })
		        .build();

		// 初始化占星Agent
		AstrologyAgent astrologyAgent = AgenticServices.agentBuilder(AstrologyAgent.class)
		        .chatModel(BASE_MODEL) // 配置基础大语言模型
		        .outputKey("horoscope") // 星座运势存入共享变量horoscope
		        .build();

		// 构建顺序工作流Agent：先询问星座，再生成运势
		UntypedAgent horoscopeAgent = AgenticServices.sequenceBuilder()
		        .subAgents(humanInTheLoop, astrologyAgent) // 先调用人机交互Agent，再调用占星Agent
		        .outputKey("horoscope") // 工作流最终输出：星座运势
		        .build();
		
		horoscopeAgent.invoke(Map.of("name", "马里奥"));

	}

}

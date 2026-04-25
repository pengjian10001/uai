package com.uni.uai.graph.example;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import java.util.Map;
import java.util.Optional;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph.StreamMode;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeActionWithConfig;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.bsc.langgraph4j.GraphRepresentation.Type;

public class FixedEdgeExample {
	public static void main(String[] args) throws GraphStateException {
		// 2. 定义节点：执行逻辑，返回状态更新请求
		// 节点1：初始化消息
		AsyncNodeAction<MyState> node1 = AsyncNodeAction.node_async((MyState state) -> {
		    System.out.println("执行节点1：初始化消息");
		    return Map.of("messages", "开始执行工作流");
		});

		// 节点2：处理逻辑，生成结果
		AsyncNodeAction<MyState> node2 = AsyncNodeAction.node_async((MyState state) -> {
		    System.out.println("执行节点2：处理逻辑");
		    return Map.of(
		            "messages", "处理完成",
		            "result", "Hello LangGraph4j" // 更新结果字段
		    );
		});
		System.out.println("---");
		
		// 3. 定义边：此处用固定边（后续讲条件边）
		// 无需显式定义，通过StateGraph的addEdge方法关联
		// 构建图：绑定状态Schema和构造器，**1. 使用Schema、初始状态构造图。Schema是定义了如何更新状态，构造器用于初始化状态
		StateGraph<MyState> graphBuilder = new StateGraph<>(MyState.SCHEMA, MyState::new)
		        .addNode("node1", node1) // 添加节点：节点ID + 节点逻辑。 **2.添加节点
		        .addNode("node2", node2)
		        .addEdge(START, "node1") // 入口：START → node1。**3.定义节点之间的关系（边）
		        .addEdge("node1", "node2") // 固定边：node1 → node2
		        .addEdge("node2", END); // 终止：node2 → END				
		
		// 1. 构建编译配置（可空，使用默认配置）
		CompileConfig compileConfig = CompileConfig.builder()
		        .recursionLimit(100) // 最大递归深度，防止无限循环（默认25）
		        .interruptBefore("node2") // 执行node2前断点（人机协同用）
		        .graphId("my-first-graph") // 图唯一标识（日志/监控用）
		        .build();

		// 2. 编译图：生成可执行的CompiledGraph
		CompiledGraph<MyState> graph = graphBuilder.compile(compileConfig);
		// 极简编译：使用默认配置
		// CompiledGraph<MyState> graph = graphBuilder.compile();
		
		
		// 极简配置（仅必选threadId）
		RunnableConfig simpleConfig = RunnableConfig.builder()
		        .threadId("session-001") // 会话ID，如用户ID+时间戳
		        .build();

		// 完整配置（带元数据、流式模式）
		RunnableConfig fullConfig = RunnableConfig.builder()
		        .threadId("user-123-conv-456")
		        .streamMode(StreamMode.SNAPSHOTS) // 仅返回状态变更
		        .putMetadata("userId", "123") // 传递用户ID
		        .putMetadata("llmModel", "gpt-4o-mini") // 传递模型选择
		        .putMetadata("isVip", true) // 传递功能开关
		        .build();

		// 修改现有配置的元数据
		RunnableConfig updatedConfig = fullConfig.updateMetadata(Map.of("llmModel", "gpt-4o"));
		
		
		// 节点中访问配置
		NodeActionWithConfig<MyState> configNode = (MyState state, RunnableConfig config) -> {
		    // 获取threadId
		    String threadId = config.threadId().orElse("default");
		    // 获取元数据
		    String userId = (String) config.metadata("userId").orElse("anonymous");
		    String model = (String) config.metadata("model").orElse("gpt-3.5");
		    var graphId = config.graphId();
		    // 3. 获取图ID
		    System.out.printf("会话[%s]：用户[%s]使用模型[%s]执行%n", threadId, userId, model);
		    // 返回状态更新
		    return Map.of("messages", "基于配置执行节点");
		};
		graphBuilder.addNode("configNode", AsyncNodeActionWithConfig.node_async(configNode));
		
		
		// 初始输入：状态的初始数据
		Map<String, Object> initInput = Map.of("messages", "初始输入");

		// 同步执行：阻塞直到完成，返回最终状态Map
		Optional<MyState> finalResult = graph.invoke(initInput, simpleConfig);

		// 打印结果
		System.out.println("同步执行最终结果：" + finalResult);
		// 输出：{messages: [开始执行工作流, 处理完成], result: Hello LangGraph4j}


		AsyncGenerator<?> generator = graph.stream(initInput, fullConfig);

		// 迭代获取每一步结果，实时处理
		generator.forEach(step -> {
		    System.out.println("流式执行步骤：" + step);
		    // 输出每一步的节点ID、状态更新等信息
		});

		// 流式完成后，通过GraphResult安全获取最终结果
		GraphResult finalGraphResult = GraphResult.from(generator);
		if (finalGraphResult.isStateData()) {
		    // 正常完成：获取最终状态
		    Map<String, Object> finalState = finalGraphResult.asStateData();
		    System.out.println("流式执行最终状态：" + finalState);
		}
		
		
		
		
		
		
		
		
		
		
		
		// 生成Mermaid可视化代码（推荐，简洁易读）
		GraphRepresentation mermaidGraph = graph.getGraph(Type.MERMAID);
		System.out.println("Mermaid可视化代码：");
		System.out.println(mermaidGraph.getContent());

		// 生成PlantUML可视化代码
		GraphRepresentation plantUmlGraph = graph.getGraph(Type.PLANTUML);
		System.out.println("PlantUML可视化代码：");
		System.out.println(plantUmlGraph.getContent());





	}
}

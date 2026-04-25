package com.uni.uai.graph.example;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import java.util.Collection;
import java.util.Map;

// 1. 定义自定义状态：包含messages（追加模式）、result（覆盖模式）
public class CheckPointExample {

	public static void main(String[] args) throws Exception {
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
		
		// 3. 定义边：此处用固定边（后续讲条件边）
		// 无需显式定义，通过StateGraph的addEdge方法关联
		// 构建图：绑定状态Schema和构造器，**1. 使用Schema、初始状态构造图。Schema是定义了如何更新状态，构造器用于初始化状态
		StateGraph<MyState> graphBuilder = new StateGraph<>(MyState.SCHEMA, MyState::new)
		        .addNode("node1", node1) // 添加节点：节点ID + 节点逻辑。 **2.添加节点
		        .addNode("node2", node2)
		        .addEdge(START, "node1") // 入口：START → node1。**3.定义节点之间的关系（边）
		        .addEdge("node1", "node2") // 固定边：node1 → node2
		        .addEdge("node2", END); // 终止：node2 → END						
		
		
		
		// 1. 创建检查点实例
		BaseCheckpointSaver saver = new MemorySaver();

		// 2. 编译图时绑定
		CompileConfig configWithCheckpoint = CompileConfig.builder()
		        .checkpointSaver(saver) // 绑定检查点
		        .build();
		
		CompiledGraph<MyState> graphWithCheckpoint = graphBuilder.compile(configWithCheckpoint);
		
		// 3. 执行图（必传threadId）
		RunnableConfig runConfig = RunnableConfig.builder().threadId("user-123").build();
		
		graphWithCheckpoint.invoke(Map.of(), runConfig);

		// 4. 状态管理
		// 获取当前状态
		StateSnapshot<MyState> currentState = graphWithCheckpoint.getState(runConfig);
		System.out.println("currentState:" + currentState);
		// 获取状态历史
		Collection<StateSnapshot<MyState>> stateHistory = graphWithCheckpoint.getStateHistory(runConfig);
		System.out.println("stateHistory:" + stateHistory);
		// 手动更新状态（如人工介入修改）
		graphWithCheckpoint.updateState(runConfig, Map.of("result", "人工修改后的结果"), "node2");



	}

	


	
}

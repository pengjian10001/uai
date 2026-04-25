package com.uni.uai.graph.example;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import java.util.Map;
import java.util.Optional;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph.StreamMode;
import org.bsc.langgraph4j.GraphRepresentation.Type;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeActionWithConfig;
import org.bsc.langgraph4j.utils.EdgeMappings;

public class ConditionEdgeExample {
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
		StateGraph<MyState> graphBuilder = new StateGraph<>(MyState.SCHEMA, MyState::new);
		graphBuilder.addNode("node1", node1) // 添加节点：节点ID + 节点逻辑。 **2.添加节点
        			.addNode("node2", node2);	
		graphBuilder.addEdge(START, "node2");  //一定要增加入口点
		        			
		
		// 定义路由函数：基于state的result字段判断
		EdgeAction<MyState> router = (MyState state) -> {
		    String result = state.value("result").orElse("").toString();
		    if (result.contains("success")) return "success-node—label";
		    else if (result.contains("error")) return "error-node—label";
		    else return "retry-node—label";
		};
		// 添加条件边：node2执行后，根据路由函数结果跳转
		graphBuilder.addConditionalEdges("node2",
		        AsyncEdgeAction.edge_async(router), // 路由函数
		        EdgeMappings.builder() // 路由映射：标识 → 目标节点
		                .to("success-node", "success-node—label")
		                .to("error-node", "error-node—label")
		                .to("retry-node", "retry-node—label")
		                .build()
		);
		// 为分支节点添加后续逻辑
		graphBuilder.addNode("success-node", AsyncNodeAction.node_async(s -> Map.of("messages", "执行成功")))
		        .addNode("error-node", AsyncNodeAction.node_async(s -> Map.of("messages", "执行失败")))
		        .addNode("retry-node", AsyncNodeAction.node_async(s -> Map.of("messages", "重试执行")))
		        .addEdge("success-node", END)
		        .addEdge("error-node", END)
		        .addEdge("retry-node", "node2"); // 重试：跳回node2


		
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
		
		// 生成Mermaid可视化代码（推荐，简洁易读）
		GraphRepresentation mermaidGraph = graph.getGraph(Type.MERMAID);
		System.out.println("Mermaid可视化代码：");
		System.out.println(mermaidGraph.getContent());				
		




	}
}

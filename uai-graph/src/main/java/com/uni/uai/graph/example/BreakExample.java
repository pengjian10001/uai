package com.uni.uai.graph.example;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import org.bsc.langgraph4j.action.InterruptableAction;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// 1. 定义自定义状态：包含messages（追加模式）、result（覆盖模式）
public class BreakExample {
	static class Node2 implements InterruptableAction<MyState>, NodeAction<MyState>{
	    @Override  //**实现节点NodeAction接口的方法
	    public Map<String, Object> apply(MyState state) throws Exception {
	        //这是原有Node2中的逻辑
	        System.out.println("执行节点2：处理逻辑");
	        return Map.of(
	                "messages", "处理完成",
	                "result", "Hello LangGraph4j" // 更新结果字段
	        );
	    }

	    @Override //**实现断点InterruptableAction接口的方法
	    public Optional<InterruptionMetadata<MyState>> interrupt(String nodeId, MyState state, RunnableConfig config) {
	        System.out.println("----断点interrupt，nodeId=" + nodeId);
	        // 动态判断是否中断：状态中hasError为true时暂停
	        boolean hasError = (boolean) state.value("hasError").orElse(false);
	        if (hasError) {
	            //参见《数据科学 大模型 LangGraph4j 5 API》，根据interrupt()返回值，空 = 继续执行，有值 = 中断执行
	            // 返回中断元数据，触发暂停
	            return Optional.of(InterruptionMetadata.builder(nodeId, state).build());
	        }
	        // 不中断，继续执行
	        return Optional.empty();
	    }
	}

	

	public static void main(String[] args) throws Exception {
		// 2. 定义节点：执行逻辑，返回状态更新请求
		// 节点1：初始化消息
		AsyncNodeAction<MyState> node1 = AsyncNodeAction.node_async((MyState state) -> {
		    System.out.println("执行节点1：初始化消息");
		    return Map.of("messages", "开始执行工作流");
		});
		
		// 实现动态断点节点
		Node2 node2 = new Node2();
		
		// 3. 定义边：此处用固定边（后续讲条件边）
		// 无需显式定义，通过StateGraph的addEdge方法关联
		// 构建图：绑定状态Schema和构造器，**1. 使用Schema、初始状态构造图。Schema是定义了如何更新状态，构造器用于初始化状态
		StateGraph<MyState> graphBuilder = new StateGraph<>(MyState.SCHEMA, MyState::new);
		
		graphBuilder.addNode("node1", node1) // 添加节点：节点ID + 节点逻辑。 **2.添加节点
				.addNode("node2", AsyncNodeAction.node_async(node2))
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
		
		System.out.println("-------------- 第一次invoke");
		Optional<MyState> state = graphWithCheckpoint.invoke(Map.of("hasError", true), runConfig);
		System.out.println(state);
		
		System.out.println("-------------- 第二次invoke");
		state = graphWithCheckpoint.invoke(GraphInput.resume(), runConfig);
		System.out.println(state);
		
		System.out.println("-------------- 第三次invoke");
		state = graphWithCheckpoint.invoke(GraphInput.resume(Map.of("hasError", false, "messages", "人工修复错误")), runConfig);
		System.out.println(state);

		
		//runConfig = RunnableConfig.builder().threadId("user-456").build();
		System.out.println("-------------- 第一次stream");
		// 第一次执行：触发断点暂停. **hasError在MyState的SCHEMA中并没有定义，此处也可以设置，默认的Chanel是新值替换旧值。
		AsyncGenerator<?> generator = graphWithCheckpoint.stream(Map.of("hasError", true), runConfig);
		consumeResult(generator);
		
		System.out.println("-------------- 第二次stream");
		// 人工处理后，恢复执行（不传新状态，使用原有状态）
		generator = graphWithCheckpoint.stream(GraphInput.resume(), runConfig);
		consumeResult(generator);
		
		System.out.println("-------------- 第三次stream");

		// 人工处理后，恢复执行（传入新状态，覆盖原有状态）
		generator = graphWithCheckpoint.stream(GraphInput.resume(Map.of("hasError", false, "messages", "人工修复错误")), runConfig);
		consumeResult(generator);



	}
	
	public static void consumeResult(AsyncGenerator<?> generator) {
		generator.forEach(step -> System.out.println("步骤：" + step));

		// 获取最终结果，判断是否为中断
		GraphResult result = GraphResult.from(generator);
		if (result.isInterruptionMetadata()) {
		    InterruptionMetadata<MyState> interruption = result.asInterruptionMetadata();
		    System.out.println("中断节点：" + interruption.nodeId());
		    System.out.println("中断时状态：" + interruption.state());
		}
		
		GraphResult finalResult = GraphResult.from(generator);
		if (finalResult.isEmpty()) {
		    System.out.println("执行无结果");
		} else if (finalResult.isStateData()) {
		    Map<String, Object> state = finalResult.asStateData();
		    System.out.println("正常完成，状态：" + state);
		} else if (finalResult.isNodeOutput()) {
		    var nodeOutput = finalResult.asNodeOutput();
		    System.out.println("节点执行完成，节点ID：" + nodeOutput.node());
		} else if (finalResult.isInterruptionMetadata()) {
		    var interruption = finalResult.asInterruptionMetadata();
		    System.out.println("执行中断，节点：" + interruption.nodeId());
		} else if (finalResult.isCheckpointSaverTag()) {
		    var tag = finalResult.asCheckpointSaverTag();
		    System.out.println("检查点标识：" + tag);
		}


	}

	


	
}

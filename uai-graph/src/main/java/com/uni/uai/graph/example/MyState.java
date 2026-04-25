package com.uni.uai.graph.example;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompiledGraph.StreamMode;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.action.CommandAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeActionWithConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.utils.EdgeMappings;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

// 1. 定义自定义状态：包含messages（追加模式）、result（覆盖模式）
public class MyState extends AgentState {
    // 定义Schema：key=状态字段，value=通道（指定更新规则）
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "messages", Channels.appender(ArrayList::new), // 追加：新值添加到列表末尾
            "result", Channels.base((old, newVal) -> newVal)  // 自定义归约：直接覆盖（默认规则，可省略）
    );
    // 构造器必须实现 **当前状态的数据结构为Map<String, Object>
    public MyState(Map<String, Object> initData) {
        super(initData);
    }

	



	


	
}

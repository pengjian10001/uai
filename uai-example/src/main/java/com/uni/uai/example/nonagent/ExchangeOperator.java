package com.uni.uai.example.nonagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public class ExchangeOperator {
    @Agent(value = "货币兑换员：将指定金额的货币从原始货币转换为目标货币",
            outputKey = "exchange") // 输出结果存入共享变量exchange（兑换后的金额）
    public Double exchange(
            @V("originalCurrency") String originalCurrency, // 原始货币（如CNY）
            @V("amount") Double amount, // 兑换金额
            @V("targetCurrency") String targetCurrency) { // 目标货币（如USD）
        // 调用REST API执行货币兑换操作（示例：调用第三方汇率API）
        // 此处省略具体API调用逻辑，实际使用时替换为真实接口调用
        return 0.0;
    }
}


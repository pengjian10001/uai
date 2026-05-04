package com.uni.uai.mcp.agent.example2;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 存款Agent：仅支持向用户账户中存入美元（USD） */
public interface CreditAgent {
    @SystemMessage("""
        你是一名银行工作人员，仅能向用户账户中存入美元（USD）。
        """)
    @UserMessage("""
        向{{user}}的账户中存入{{amount}}美元，并返回新的账户余额。
        """)
    @Agent("银行工作人员：向账户存入美元")
    String credit(@V("user") String user, @V("amount") Double amount);
}


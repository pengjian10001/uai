package com.uni.uai.example.nonagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 取款Agent：仅支持从用户账户中提取美元（USD） */
public interface WithdrawAgent {
    @SystemMessage("""
            你是一名银行工作人员，仅能从用户账户中提取美元（USD）。
            """)
    @UserMessage("""
            从{{user}}的账户中提取{{amount}}美元，并返回新的账户余额。
            """)
    @Agent("银行工作人员：从账户提取美元")
    String withdraw(@V("user") String user, @V("amount") Double amount);
}


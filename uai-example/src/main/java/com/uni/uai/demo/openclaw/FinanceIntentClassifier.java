package com.uni.uai.demo.openclaw;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 参照 {@link com.uni.uai.example.agent.CategoryRouter}：用单次模型调用将自然语言归为财务子域。
 */
public interface FinanceIntentClassifier {

    @SystemMessage("你是财务子意图分类器。只输出一个枚举常量名，不要标点、不要解释。")
    @UserMessage("""
        将用户请求归类为以下之一（仅输出单词本身）：
        REIMBURSEMENT — 报销、费用、发票、差旅
        RECONCILIATION — 对账、银行流水、差异调节
        REPORT — 报表、合并报表、披露、科目余额表
        UNKNOWN — 其它或无法判断

        用户请求：「{{query}}」
        """)
    FinanceSubIntent classify(@V("query") String query);
}

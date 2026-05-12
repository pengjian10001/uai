package com.uni.uai.demo.openclaw;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 参照 {@link com.uni.uai.example.agent.CategoryRouter}：用单次模型调用将自然语言归为分析子域。
 */
public interface AnalysisIntentClassifier {

    @SystemMessage("你是数据分析子意图分类器。只输出一个枚举常量名，不要标点、不要解释。")
    @UserMessage("""
        将用户请求归类为以下之一（仅输出单词本身）：
        TREND — 趋势、同比、环比、时间序列
        SUMMARY — 摘要、总结、要点提炼
        COMPARISON — 对比、差异、A/B、多版本
        UNKNOWN — 其它或无法判断

        用户请求：「{{query}}」
        """)
    AnalysisSubIntent classify(@V("query") String query);
}

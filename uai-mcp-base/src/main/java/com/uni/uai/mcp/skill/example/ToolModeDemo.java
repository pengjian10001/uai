package com.uni.uai.mcp.skill.example;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import java.nio.file.Path;

import com.uni.uai.mcp.llm.ChatModelFactory;
interface CustomerAi {
    String chat(String message);
}
class AfterSalesTools {
    @Tool("读取退款规则摘要")
    public String readPolicySummary() {
        return "未发货全退；已发货扣5%手续费。";
    }
    @Tool("计算退款金额")
    public String calculateRefund(double orderAmount, double feeRate) {
        double value = Math.round(orderAmount * (1 - feeRate) * 100.0) / 100.0;
        return "应退金额: " + value + " 元";
    }
}
public class ToolModeDemo {
    public static void main(String[] args) {
        ChatModel chatModel = ChatModelFactory.getInstance().getDefaultChatModel(); // 替换为真实模型
        //** FileSystemSkillLoader.loadSkills方法返回List<FileSystemSkill>，再用Skills.from方法封装为Skills，才能给AiServices使用
        Skills skills = Skills.from(FileSystemSkillLoader.loadSkills(Path.of("/Users/pengjian/work/skills/")));
        CustomerAi ai = AiServices.builder(CustomerAi.class)
            .chatModel(chatModel)
            .tools(new AfterSalesTools())
            .toolProvider(skills.toolProvider())
            .systemMessage("你有以下skills:\n" + skills.formatAvailableSkills()
                + "\n当请求匹配某个skill时，先调用 activate_skill。")
            .build();
        String r = ai.chat("订单金额200，已发货，算退款");
        System.out.println(r);
    }
}


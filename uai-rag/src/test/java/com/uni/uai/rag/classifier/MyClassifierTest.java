package com.uni.uai.rag.classifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.classification.ClassificationResult;

public class MyClassifierTest {

	public static void main(String[] args) {
		Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
		map.put("xiaozhuge", List.of("近三个月带看量", "联网门店", "成交金额"));
		map.put("xiaoaoding", List.of("报告报错", "", "api文档", "SQL转换", "如何配置报告"));
		
		MyClassifier classifier = new MyClassifier(map);
		ClassificationResult<String> result = classifier.classifyWithScores("我今天要配置报告，在哪里找文档呢");
		System.out.println(result.scoredLabels());
		result = classifier.classifyWithScores("部署 2-3 组参数组合，通过 A/B 测试对比实际效果（如用户投诉率、意图识别成功率）");
		System.out.println(result.scoredLabels());

	}

}

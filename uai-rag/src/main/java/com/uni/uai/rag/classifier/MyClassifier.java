package com.uni.uai.rag.classifier;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import java.util.Set;

import dev.langchain4j.classification.ClassificationResult;
import dev.langchain4j.classification.EmbeddingModelTextClassifier;

public class MyClassifier {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private EmbeddingModelTextClassifier<String> classifier = null;
	//private EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
	EmbeddingModel embeddingModel = new BgeSmallZhV15EmbeddingModel();
	
	public MyClassifier(Map<String, ? extends Collection<String>> examplesByLabel) {
		Map<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
		//排空，排重
		for(Entry<String, ? extends Collection<String>> entry : examplesByLabel.entrySet()) {
			String key = entry.getKey();
			Collection<String> value = entry.getValue();
			Set<String> set = new LinkedHashSet<String>();
			for(String s : value) {
				if(s != null && !"".equals(s.trim())) {
					set.add(s);
				}
			}
			if(set.size()>0) {
				map.put(key, set);
			}
		}
		//对于rag，设置minScore为0.7，且取最大值
		this.classifier = new EmbeddingModelTextClassifier<String>(embeddingModel, map, 1, 0.5, 1);
	}
	
	public ClassificationResult<String> classifyWithScores(String text) {
		return classifier.classifyWithScores(text);
	}

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

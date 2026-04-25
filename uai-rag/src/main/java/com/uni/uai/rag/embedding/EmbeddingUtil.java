package com.uni.uai.rag.embedding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.uni.uai.rag.llm.EmbeddingModelFactory;
import com.uni.ubag.common.concurrent.Action;
import com.uni.ubag.common.concurrent.AsynResourceBroker;
import com.uni.ubag.common.concurrent.ResourceBroker;
import com.uni.ubag.common.concurrent.ResourceResult;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.util.JSONUtil;
import com.uni.ubag.common.util.StringUtil;
import com.uni.ubag.log.proxy.ProxyAction;
import com.uni.ubag.log.util.UbagLogUtil;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

public class EmbeddingUtil {
	private static EmbeddingUtil instance = new EmbeddingUtil();
	public static EmbeddingUtil getInstance() {
		return instance;
	}
	
	ResourceBroker resourceBroker = AsynResourceBroker.resourceEmbeddingModelBrokerLongTime;
	
	public float[] embed(String text) {
		EmbeddingModel model = EmbeddingModelFactory.getInstance().getDefaultEmbeddingModel();
		String logKey = String.format("embed, text=%s", StringUtil.substring(text, 100));
		float[] result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.RPC, logKey, text, new ProxyAction<float[]>() {
			public float[] exec() throws Throwable{
				Response<Embedding> response = model.embed(text);
		        Embedding embedding = response.content();
		        float[] vector = embedding.vector();
		        return vector;
			}
		});
		return result;
	}
	
	public Map<String, float[]> embedAll(List<String> texts) {
		EmbeddingModel model = EmbeddingModelFactory.getInstance().getDefaultEmbeddingModel();
		String logKey = String.format("embedAll, textssize=%s", texts.size());
		Map<String, float[]> result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.RPC, logKey, JSONUtil.toJSONString(texts), new ProxyAction<Map<String, float[]>>() {
			public Map<String, float[]> exec() throws Throwable{
				Map<String, float[]> resultMap = new LinkedHashMap<String, float[]>();
				List<TextSegment> listSegment = new ArrayList<TextSegment>();
				for(String s : texts) {
					TextSegment segment = TextSegment.from(s);
					listSegment.add(segment);
				}
				Response<List<Embedding>> response = model.embedAll(listSegment);
				List<Embedding> embeddings = response.content();
				for(int i = 0; i < embeddings.size(); i++) {
					float[] vector = embeddings.get(i).vector();
					resultMap.put(texts.get(i), vector);
				}
		        return resultMap;
			}
		});
		return result;
	}
	
	/**
	 * 异步处理
	 * @param texts
	 * @return
	 */
	public Map<String, float[]> embedAsyn(List<String> texts) {
		String logKey = String.format("embedAsyn, textsize=%s", texts.size());
		
		Map<String, float[]> result = UbagLogUtil.getInstance().tryCatchAndLog(UbagConfigEnum.UbagLogType.RPC, logKey, JSONUtil.toJSONString(texts), new ProxyAction<Map<String, float[]>>() {

			@Override
			public Map<String, float[]> exec() throws Throwable {
				ResourceResult<Map<String, float[]>> resultResoult = resourceBroker.submit(new Action<Map<String, float[]>>() {

					@Override
					public Map<String, float[]> exec() throws Exception {
						Map<String, float[]> map = embedAll(texts);
						return map;
					}
					
				});
				
				//获取异步请求的结果
				Map<String, float[]> resultMap = resultResoult.get();
				return resultMap;
			}
		});
		return result;
	}
	
	public static void main(String[] args) {
		EmbeddingUtil instance  = EmbeddingUtil.getInstance();
		List<String> texts = new ArrayList<String>();
		texts.add("你好");
		texts.add("中国");
		
		Map<String, float[]> resultMap = instance.embedAsyn(texts);
		System.out.println(resultMap.size());
		for(String key : texts) {
			float[] vector = resultMap.get(key);
			System.out.println(key);
			System.out.println(JSONUtil.toJSONString(vector));
		}
		
		resultMap = instance.embedAll(texts);
		System.out.println(resultMap.size());
		for(String key : texts) {
			float[] vector = resultMap.get(key);
			System.out.println(key);
			System.out.println(JSONUtil.toJSONString(vector));
		}
		
		System.exit(0);
		
	}

}

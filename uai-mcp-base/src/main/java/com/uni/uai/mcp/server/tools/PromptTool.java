package com.uni.uai.mcp.server.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uni.uai.mcp.data.DataSourceUtil;
import com.uni.uai.mcp.llm.ChatModelFactory;
import com.uni.uai.mcp.model.PromptPO;
import com.uni.uai.mcp.server.ServerUtil.PromptDesc;
import com.uni.uai.mcp.utils.FreeMarkerTemplate;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.JSONUtil;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class PromptTool {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static PromptTool instance = new PromptTool();
	public static PromptTool getInstance() {
		return instance;
	}

	/**
	 * 意图识别。即根据用户查询，识别使用哪个prompt模版
	 * @param queryText
	 * @return
	 */
	public String promptIdentify(String queryText) {
		//从数据库中获取到所有prompt
		List<PromptPO> listPromptPO = DataSourceUtil.getInstance().getPromptsFromDB();
		List<PromptDesc> options = new ArrayList<PromptDesc>();
		for(int i = 0; i < listPromptPO.size(); i++) {
			PromptPO po = listPromptPO.get(i);
			options.add(new PromptDesc(po.getName(), po.getDescription()));
		}
		//最后加一个未知
		options.add(new PromptDesc("_unknow", "未知，与其他选项不匹配时，选择这个"));
		
		//意图识别的prompt
		var p = """
				你是一个意图判断专家，下面以JSON数组的形式给出一组【选项】，每个选项有2个字段：name表示选项的名称，description表示选项的描述。\n
				将【用户问题】与这些【选项】的description一一匹配，找出最匹配【用户问题】的一项，并返回此选项name的值。\n
				【选项】：\n
				${options}\n
				【用户问题】：\n
				${queryText}\n
				返回：
				最匹配的选项对应的name，不要返回其他无关的内容
				""";
		
		//返回的结果，就是prompt的名称
		String llmanswer = this.queryOptionsPrompt(p, JSONUtil.toJSONString(options), queryText);;
		if(llmanswer.equals("_unknow")) {
			return queryText;
		}else {
			//根据prompt名称，赵大对应的prompt模版
			String prompt = null;
			for(int i = 0; i < listPromptPO.size(); i++) {
				PromptPO po = listPromptPO.get(i);
				if(llmanswer.equals(po.getName())) {
					String promptTemplate = po.getPromptTemplate();
					String paramSchema = po.getParamSchema();
					//再从用户查询中，解析出prompt模版中需要的变量，从而解析prompt模版，得到能使用的prompt
					prompt = this.getAndParsedPrompt(promptTemplate, paramSchema, queryText);
					break;
				}
			}
			return prompt;
		}
	}
	
	public String getAndParsedPrompt(String promptTemplate, String paramSchema, String queryText) {
		//从用户查询中，按照prompt模版的参数结构提取参数的prompt
		var p = """
				下面以JSON数组的形式给出一组【选项】，每个选项有2个主要字段：name表示选项的名称，description表示选项的描述。\n
				仔细理解这些【选项】的description，从【用户问题】中提取相关信息，并以JSON对象返回提取的内容。\n
				【选项】：\n
				${options}\n
				【用户问题】：\n
				${queryText}\n
				返回：
				生成一个符合 JSON 格式规范的内容来回答问题。确保输出的内容是一个完整的 JSON 对象，不要包含任何非 JSON 格式的字符或文本描述，不要markdown格式。\n
				返回的JSON对象中，每一个元素格式如下：\n
				key=【选项】中对应的name的值 \n
				value=从【用户问题】中提取的相关信息 \n。
				注意：不要返回其他无关的内容。
				""";
		//返回的结果，是prompt模版中的参数
		String llmanswer = queryOptionsPrompt(p, paramSchema, queryText);
		
		String result = FreeMarkerTemplate.parse(JSONUtil.parseObject(llmanswer), promptTemplate);
		return result;
	}
	
	/**
	 * 解析一个prompt模版，其带有一组选项options和用户查询queryText两个参数
	 * 例如，prompt模版为
	   下面以JSON数组的形式给出一组【选项】，每个选项有2个主要字段：name表示选项的名称，description表示选项的描述。\n
		仔细理解这些【选项】的description，从【用户问题】中提取相关信息，并以JSON对象返回提取的内容。\n
		【选项】：\n
				${options}\n
		【用户问题】：\n
				${queryText}\n
		返回：
		生成一个符合 JSON 格式规范的内容来回答问题。确保输出的内容是一个完整的 JSON 对象，不要包含任何非 JSON 格式的字符或文本描述，不要markdown格式。\n
		返回的JSON对象中，每一个元素格式如下：\n
				key=【选项】中对应的name的值 \n
				value=从【用户问题】中提取的相关信息 \n。
		注意：不要返回其他无关的内容。
	 * @param promptTemplate
	 * @param options
	 * @param queryText
	 * @return
	 */
	private String queryOptionsPrompt(String promptTemplate, String options, String queryText) {
		Map<String,Object> context = new HashMap<>();
		context.put("options", options);
		context.put("queryText", queryText);
		String promptQuery = FreeMarkerTemplate.parse(context, promptTemplate);
		List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
		chatMessageList.add(new UserMessage(promptQuery));
		
		ChatModel model = ChatModelFactory.getInstance().getDefaultChatModel();
		//返回的结果，是prompt模版中的参数
		String llmanswer = model.chat(chatMessageList).aiMessage().text();
		return llmanswer;
	}
	
	public static void main(String[] args) {
		var promptTemplate = """
				解释${language!'java'}代码是如何工作的：\n\n
				${code}
				""";
		String paramSchema = """
				[
				    {
				      "name": "code",
				      "description": "要解释的代码",
				      "required": true
				    },
				    {
				      "name": "language",
				      "description": "程序语言",
				      "required": false
				    }
				  ]
				""";
		String queryText = """
				解析java代码logger.info(abc)
				""";
		String prompt = PromptTool.getInstance().getAndParsedPrompt(promptTemplate, paramSchema, queryText);
		System.out.println(prompt);
	}
	
}

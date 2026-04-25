package com.uni.uai.mcp.rag.retriever;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;

import com.alibaba.fastjson2.JSONArray;
import com.uni.uai.mcp.common.FileInfo;
import com.uni.uai.mcp.common.UaiConf;
import com.uni.uai.mcp.utils.JSONUtil;
import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.data.db.sqlite.JsonSqlUtil2;
import com.uni.ubag.log.util.UbagLogUtil;
import com.uni.ubag.util.excel.easyexcel.EasyExcelUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class JsonSqlContentRetriever implements ContentRetriever {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
    		 """
			你是编写SQL查询的专家。
			你可以使用以下[表结构]访问SQLite数据库：\n
			{{databaseStructure}} \n
			如果用户提出的问题可以通过查询该数据库来回答，则按如下[步骤]生成一个SQL SELECT查询。
			1.将用户问题转换为使用[表结构]中的表名和字段名同等含义的描述。
			2.根据[步骤]1生成SQL SELECT查询。如果有多个[表结构]，则分析表之间的关系，优先尝试在SQL中使用JOIN
			
			[注意]:
			除了有效的SQL语句外，不要输出任何其他内容！
			"""
    );

    private final String databaseStructure;

    private final PromptTemplate promptTemplate;
    private final ChatModel chatLanguageModel;
    private final List<FileInfo> fileInfos;
    private final int maxRetries = 2;

    public JsonSqlContentRetriever(
                                       String databaseStructure,
                                       PromptTemplate promptTemplate,
                                       ChatModel chatLanguageModel,
                                       List<FileInfo> fileInfos) {
    	this.fileInfos = fileInfos;
        this.databaseStructure = getOrDefault(databaseStructure, () -> generateDDL(fileInfos));
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
        this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
    }

    public static JsonSqlContentRetrieverBuilder builder() {
        return new JsonSqlContentRetrieverBuilder();
    }
    
    private static String generateDDL(List<FileInfo> fileInfos) {
        StringBuilder ddl = new StringBuilder();
        
        List<String> tables = new ArrayList<String>();
		try {
			//List<FileInfo> listFileInfo = UaiConf.getExcelFileInfo();
			if(fileInfos != null && fileInfos.size()>0) {
				for(FileInfo fileInfo : fileInfos) {
					List<Map<String, Object>>  excelData = EasyExcelUtil.getInstance().read(fileInfo.getTempFilePath());
					//为避免重复上传相同文件导致的错误，先尝试删除表
					JsonSqlUtil2.getInstance().dropTable(fileInfo.getFileName());
					JsonSqlUtil2.getInstance().createTable(fileInfo.getFileName(), JSONUtil.getInstance().toJsonArray(excelData));
					
					tables = JsonSqlUtil2.getInstance().showTables();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(tables.size() == 0) {
			//如果为空，则没有必要再执行这个工具，抛出异常
			throw new RuntimeException("没有上传excel文件");
		}else {
			for(int i = 0; i < tables.size(); i++) {
	        	ddl.append(String.format("%s.  %s  \n", i+1, tables.get(i)));
	        }
		}
        return ddl.toString();
    }

    @Override
    public List<Content> retrieve(Query naturalLanguageQuery) {
    	List<Content> list = new ArrayList<Content>();

        String sqlQuery = null;
        String errorMessage = null;
        
        int attemptsLeft = maxRetries + 1;
        while (attemptsLeft > 0) {
            attemptsLeft--;
            
            sqlQuery = generateSqlQuery(naturalLanguageQuery, sqlQuery, errorMessage);

            sqlQuery = clean(sqlQuery);

            if (!isSelect(sqlQuery)) {
                return emptyList();
            }

            long start = System.currentTimeMillis();
            Exception ee = null;
            boolean issuccess = true;
            boolean isMark = false;
            try {
            	isMark = TimeTrace.markStart("JsonSqlContentRetriever");
                validate(sqlQuery);
                System.out.println("执行sql=" + sqlQuery);
                String result = execute(sqlQuery, attemptsLeft);
                Content content = format(result, sqlQuery);
                list = singletonList(content);
                return list;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                ee = e;
                issuccess = false;
                UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "JsonSqlContentRetriever异常:" + ee.getClass(), sqlQuery, ee, false, 0L, ExceptionUtil.toStackTrace(ee));
            } finally {
            	long time = System.currentTimeMillis()-start;
            	if(isMark) {
    				TimeTrace.markEnd("JsonSqlContentRetriever", time, issuccess?0L:1L);
    			}
            	//避免tostring导致垃圾回收，日志只打印list.size
            	UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), "JsonSqlContentRetriever", sqlQuery, ee, issuccess, time, list);
            }
        }

        return emptyList();
    }

    protected String generateSqlQuery(Query naturalLanguageQuery, String previousSqlQuery, String previousErrorMessage) {

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createSystemPrompt().toSystemMessage());
        messages.add(UserMessage.from(naturalLanguageQuery.text()));

        if (previousSqlQuery != null && previousErrorMessage != null) {
            messages.add(AiMessage.from(previousSqlQuery));
            messages.add(UserMessage.from(previousErrorMessage));
        }

        return chatLanguageModel.chat(messages).aiMessage().text();
    }

    protected Prompt createSystemPrompt() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("databaseStructure", databaseStructure);
        return promptTemplate.apply(variables);
    }

    protected String clean(String sqlQuery) {
        if (sqlQuery.contains("```sql")) {
            return sqlQuery.substring(sqlQuery.indexOf("```sql") + 6, sqlQuery.lastIndexOf("```"));
        } else if (sqlQuery.contains("```")) {
            return sqlQuery.substring(sqlQuery.indexOf("```") + 3, sqlQuery.lastIndexOf("```"));
        }
        return sqlQuery;
    }

    protected void validate(String sqlQuery) {

    }

    protected boolean isSelect(String sqlQuery) {
        try {
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            return statement instanceof Select;
        } catch (JSQLParserException e) {
            return false;
        }
    }

    protected String execute(String sqlQuery, int attemptsLeft) throws Exception {
        JSONArray result = JsonSqlUtil2.getInstance().querySql(sqlQuery);

      //如果没有结果，可能是某个字段不对，抛出异常，重试，如果是最后一次重试，则不抛出异常
        if(result.size()==0 && attemptsLeft >0) {
        	logger.info("剩余重试" + attemptsLeft);
        	String msg = String.format("%s 返回结果为空，建议重新理解用户的问题，调整一下SQL中的条件", sqlQuery);
        	logger.info(msg);
        	throw new RuntimeException(msg);
        }

        return result.toString();
    }

    private static Content format(String result, String sqlQuery) {
        return Content.from(String.format("Result of executing '%s':\n%s", sqlQuery, result));
    }

    public static class JsonSqlContentRetrieverBuilder {
        private String databaseStructure;
        private PromptTemplate promptTemplate;
        private ChatModel chatLanguageModel;
        private List<FileInfo> fileInfos;

        JsonSqlContentRetrieverBuilder() {
        }

        public JsonSqlContentRetrieverBuilder databaseStructure(String databaseStructure) {
            this.databaseStructure = databaseStructure;
            return this;
        }

        public JsonSqlContentRetrieverBuilder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public JsonSqlContentRetrieverBuilder chatModel(ChatModel chatLanguageModel) {
            this.chatLanguageModel = chatLanguageModel;
            return this;
        }
        
        public JsonSqlContentRetrieverBuilder fileInfos(List<FileInfo> fileInfos) {
            this.fileInfos = fileInfos;
            return this;
        }

        public JsonSqlContentRetriever build() {
            return new JsonSqlContentRetriever(this.databaseStructure, this.promptTemplate, this.chatLanguageModel, this.fileInfos);
        }

        public String toString() {
            return "JsonSqlContentRetriever.JsonSqlContentRetrieverBuilder(databaseStructure=" + this.databaseStructure + ", promptTemplate=" + this.promptTemplate + ", chatLanguageModel=" + this.chatLanguageModel + ")";
        }
    }
}

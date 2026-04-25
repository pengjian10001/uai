package com.uni.uai.mcp.server.tools;

import java.util.List;

import javax.sql.DataSource;

import com.uni.uai.mcp.common.FileInfo;
import com.uni.uai.mcp.data.DataConfig;
import com.uni.uai.mcp.llm.ChatModelFactory;
import com.uni.uai.mcp.rag.retriever.JsonSqlContentRetriever;
import com.uni.uai.mcp.rag.retriever.SqlContentRetriever;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;

public class RagTool {
	private static RagTool instance = new RagTool();
	public static RagTool getInstance() {
		return instance;
	}

	/**
	 * 将用户的问题，转换为对mcp库表的sql查询，以回答mcp server有什么能力
	 * @param queryText
	 * @return
	 */
	public String queryMcpServerInfoFromDB(String queryText) {
		DataSource dataSource = DataConfig.getInstance().getDefaultDataSource();
		
		PromptTemplate prompt = PromptTemplate.from(
	            """
				你是编写SQL查询的专家。
				你可以使用以下结构访问{{sqlDialect}}数据库：\n
				{{databaseStructure}} \n
				如果用户提出的问题可以通过查询该数据库来回答，则生成一个SQL SELECT查询。
				除了有效的SQL语句外，不要输出任何其他内容！
				"""
	    );
		
		SqlContentRetriever retriever = SqlContentRetriever.builder()
				.chatModel(ChatModelFactory.getInstance().getDefaultChatModel())
				.dataSource(dataSource)
				.promptTemplate(prompt)
				.build();
		
		Query query = new Query(queryText);
		List<Content> list = retriever.retrieve(query);
		if(list != null && list.size() > 0) {
			return list.get(0).textSegment().text();
		}else {
			return null;
		}
	}
	
	/**
	 * 将用户的问题，转换为对sqlite库表的sql查询，以回答分析相关问题
	 * @param queryText
	 * @return
	 */
	public String queryAyanlysisFromSqlite(String queryText, List<FileInfo> fileInfos) {
		PromptTemplate prompt = PromptTemplate.from(
	            """
				你是编写SQL查询的专家。
				你可以使用以下结构访问SQLite数据库：\n
				{{databaseStructure}} \n
				如果用户提出的问题可以通过查询该数据库来回答，则生成一个SQL SELECT查询。
				除了有效的SQL语句外，不要输出任何其他内容！
				"""
	    );
		
		JsonSqlContentRetriever retriever = JsonSqlContentRetriever.builder()
				.chatModel(ChatModelFactory.getInstance().getDefaultChatModel())
				.promptTemplate(prompt)
				.fileInfos(fileInfos)
				.build();
		
		Query query = new Query(queryText);
		List<Content> list = retriever.retrieve(query);
		if(list != null && list.size() > 0) {
			return list.get(0).textSegment().text();
		}else {
			return null;
		}
	}
	
}

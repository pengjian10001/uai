package com.uni.uai.mcp.rag.retriever;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.uni.uai.mcp.llm.ChatModelFactory;
import com.uni.uai.mcp.utils.YmlConfigUtil;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.data.impl.db.DbBaseUtil;
import com.uni.ubag.data.impl.db.DbBaseUtil.DataSourceConfig;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;

public class SqlContentRetrieverTest {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	public static void main(String[] args) throws PropertyVetoException {
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
		dataSource.setDriverClass("com.mysql.jdbc.Driver");
		dataSource.setJdbcUrl("jdbc:mysql://m10827.mars.test.mysql.ljnode.com:10827/mcp?useUnicode=true&characterEncoding=utf-8&useSSL=false");
		dataSource.setUser("root");
		dataSource.setPassword("2CB47bFA34");
		
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
		
		Query query = new Query("角色testabc有什么工具");
		List<Content> list = retriever.retrieve(query);
		System.out.println(list);
	}

}

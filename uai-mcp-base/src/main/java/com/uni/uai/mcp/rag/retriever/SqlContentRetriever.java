package com.uni.uai.mcp.rag.retriever;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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

import javax.sql.DataSource;

import com.uni.ubag.common.constant.UbagConfigEnum;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;
import com.uni.ubag.common.util.ExceptionUtil;
import com.uni.ubag.common.util.TimeTrace;
import com.uni.ubag.log.util.UbagLogUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

public class SqlContentRetriever implements ContentRetriever {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
    		 """
			你是编写SQL查询的专家。
			你可以使用以下结构访问{{sqlDialect}}数据库：\n
			{{databaseStructure}} \n
			如果用户提出的问题可以通过查询该数据库来回答，则生成一个SQL SELECT查询。
			除了有效的SQL语句外，不要输出任何其他内容！
			"""
    );

    private final DataSource dataSource;
    private final String sqlDialect;
    private final String databaseStructure;

    private final PromptTemplate promptTemplate;
    private final ChatModel ChatModel;

    private final int maxRetries;

    /**创建一个{@code SqlContentRetriever}实例。
    * @param dataSource {@link dataSource}用于执行SQL查询。
						必选参数。
						警告!数据库用户必须具有非常有限的只读权限！< / b >

    * @param sqlDialect SQL方言，将在{@link SystemMessage}中提供给LLM。
    					LLM应该知道特定的SQL方言，以便生成有效的SQL查询。
    *					例如：“MySQL”、“PostgreSQL”等。
    *					可选参数。如果没有指定，它将从{@code DataSource}确定。
    * @param databaseStructure数据库的结构，它将在{@code SystemMessage}中提供给LLM。
    					LLM应该熟悉可用的表、列、关系等，以便生成有效的SQL查询。
    *					最好指定完整的“CREATE TABLE…”每个表的DDL语句。
    *					可选参数。如果没有指定，它将从{@code DataSource}生成。
						< b >警告!在这种情况下，所有表对LLM都是可见的！< / b >
    * @param promptTemplate {@link promptTemplate}用于创建{@code SystemMessage}。
    *					可选参数。默认值：{@link #DEFAULT_PROMPT_TEMPLATE}。
    * @param ChatModel {@link ChatModel}用于生成SQL查询。
    *					必选参数。
    * @param maxRetries当数据库不能执行生成的SQL查询时重试的最大次数。
    *					错误信息将被发送回LLM以尝试更正查询。
    *					可选参数。默认值:1。
     */
    public SqlContentRetriever(DataSource dataSource,
                                       String sqlDialect,
                                       String databaseStructure,
                                       PromptTemplate promptTemplate,
                                       ChatModel ChatModel,
                                       Integer maxRetries) {
        this.dataSource = ensureNotNull(dataSource, "dataSource");
        this.sqlDialect = getOrDefault(sqlDialect, () -> getSqlDialect(dataSource));
        this.databaseStructure = getOrDefault(databaseStructure, () -> generateDDL(dataSource));
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
        this.ChatModel = ensureNotNull(ChatModel, "ChatModel");
        this.maxRetries = getOrDefault(maxRetries, 2); //默认值设置为2
    }

    // TODO (for v2)
    // -在提示符中为每个表提供几行数据
    // -选项选择要使用/忽略的表列表
    public static String getSqlDialect(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateDDL(DataSource dataSource) {
        StringBuilder ddl = new StringBuilder();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String createTableStatement = generateCreateTableStatement(tableName, metaData);
                ddl.append(createTableStatement).append("\n");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ddl.toString();
    }

    private static String generateCreateTableStatement(String tableName, DatabaseMetaData metaData) {
        StringBuilder createTableStatement = new StringBuilder();

        try {
            ResultSet columns = metaData.getColumns(null, null, tableName, null);
            ResultSet pk = metaData.getPrimaryKeys(null, null, tableName);
            ResultSet fks = metaData.getImportedKeys(null, null, tableName);

            String primaryKeyColumn = "";
            if (pk.next()) {
                primaryKeyColumn = pk.getString("COLUMN_NAME");
            }

            createTableStatement
                    .append("CREATE TABLE ")
                    .append(tableName)
                    .append(" (\n");

            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int size = columns.getInt("COLUMN_SIZE");
                String nullable = columns.getString("IS_NULLABLE").equals("YES") ? " NULL" : " NOT NULL";
                String columnDef = columns.getString("COLUMN_DEF") != null ? " DEFAULT " + columns.getString("COLUMN_DEF") : "";
                String comment = columns.getString("REMARKS");

                createTableStatement
                        .append("  ")
                        .append(columnName)
                        .append(" ")
                        .append(columnType)
                        .append("(")
                        .append(size)
                        .append(")")
                        .append(nullable)
                        .append(columnDef);

                if (columnName.equals(primaryKeyColumn)) {
                    createTableStatement.append(" PRIMARY KEY");
                }

                createTableStatement.append(",\n");

                if (comment != null && !comment.isEmpty()) {
                    createTableStatement
                            .append("  COMMENT ON COLUMN ")
                            .append(tableName)
                            .append(".")
                            .append(columnName)
                            .append(" IS '")
                            .append(comment)
                            .append("',\n");
                }
            }

            while (fks.next()) {
                String fkColumnName = fks.getString("FKCOLUMN_NAME");
                String pkTableName = fks.getString("PKTABLE_NAME");
                String pkColumnName = fks.getString("PKCOLUMN_NAME");
                createTableStatement
                        .append("  FOREIGN KEY (")
                        .append(fkColumnName)
                        .append(") REFERENCES ")
                        .append(pkTableName)
                        .append("(")
                        .append(pkColumnName)
                        .append("),\n");
            }

            if (createTableStatement.charAt(createTableStatement.length() - 2) == ',') {
                createTableStatement.delete(createTableStatement.length() - 2, createTableStatement.length());
            }

            createTableStatement.append(");\n");

            ResultSet tableRemarks = metaData.getTables(null, null, tableName, null);
            if (tableRemarks.next()) {
                String tableComment = tableRemarks.getString("REMARKS");
                if (tableComment != null && !tableComment.isEmpty()) {
                    createTableStatement
                            .append("COMMENT ON TABLE ")
                            .append(tableName)
                            .append(" IS '")
                            .append(tableComment)
                            .append("';\n");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return createTableStatement.toString();
    }

    public static SqlContentRetrieverBuilder builder() {
        return new SqlContentRetrieverBuilder();
    }

    @Override
    public List<Content> retrieve(Query naturalLanguageQuery) {
    	List<Content> list = new ArrayList<Content>();;

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
            	isMark = TimeTrace.markStart("SqlContentRetriever");
                validate(sqlQuery);

                try (Connection connection = dataSource.getConnection();
                     Statement statement = connection.createStatement()) {

                    String result = execute(sqlQuery, statement, attemptsLeft);
                    Content content = format(result, sqlQuery);
                    list = singletonList(content);
                    return list;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                ee = e;
                issuccess = false;
                UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.EXCEPTION.getCode(), "SqlContentRetriever异常:" + ee.getClass(), sqlQuery, ee, false, 0L, ExceptionUtil.toStackTrace(ee));
            } finally {
            	long time = System.currentTimeMillis()-start;
            	if(isMark) {
    				TimeTrace.markEnd("SqlContentRetriever", time, issuccess?0L:1L);
    			}
            	//避免tostring导致垃圾回收，日志只打印list.size
            	UbagLogUtil.getInstance().putUbagLogList(UbagConfigEnum.UbagLogType.CODE.getCode(), "SqlContentRetriever", sqlQuery, ee, issuccess, time, list);
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

        return ChatModel.chat(messages).aiMessage().text();
    }

    protected Prompt createSystemPrompt() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sqlDialect", sqlDialect);
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

    protected String execute(String sqlQuery, Statement statement, int attemptsLeft) throws SQLException {
        List<String> resultRows = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery(sqlQuery)) {
            int columnCount = resultSet.getMetaData().getColumnCount();

            // header
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(resultSet.getMetaData().getColumnName(i));
            }
            resultRows.add(String.join(",", columnNames));
            
            boolean hasResult = false;
            // rows
            while (resultSet.next()) {
            	hasResult = true;
                List<String> columnValues = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {

                    String columnValue = resultSet.getObject(i) == null ? "" : resultSet.getObject(i).toString();

                    if (columnValue.contains(",")) {
                        columnValue = "\"" + columnValue + "\"";
                    }
                    columnValues.add(columnValue);
                }
                resultRows.add(String.join(",", columnValues));
            }
            //如果没有结果，可能是某个字段不对，抛出异常，重试，如果是最后一次重试，则不抛出异常
            if(!hasResult && attemptsLeft >0) {
            	logger.info("剩余重试" + attemptsLeft);
            	String msg = String.format("%s 返回结果为空，建议重新理解用户的问题，调整一下SQL中的条件", sqlQuery);
            	logger.info(msg);
            	throw new RuntimeException(msg);
            }
        }

        return String.join("\n", resultRows);
    }

    private static Content format(String result, String sqlQuery) {
        return Content.from(String.format("Result of executing '%s':\n%s", sqlQuery, result));
    }

    public static class SqlContentRetrieverBuilder {
        private DataSource dataSource;
        private String sqlDialect;
        private String databaseStructure;
        private PromptTemplate promptTemplate;
        private ChatModel ChatModel;
        private Integer maxRetries;

        SqlContentRetrieverBuilder() {
        }

        public SqlContentRetrieverBuilder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public SqlContentRetrieverBuilder sqlDialect(String sqlDialect) {
            this.sqlDialect = sqlDialect;
            return this;
        }

        public SqlContentRetrieverBuilder databaseStructure(String databaseStructure) {
            this.databaseStructure = databaseStructure;
            return this;
        }

        public SqlContentRetrieverBuilder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public SqlContentRetrieverBuilder chatModel(ChatModel ChatModel) {
            this.ChatModel = ChatModel;
            return this;
        }

        public SqlContentRetrieverBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public SqlContentRetriever build() {
            return new SqlContentRetriever(this.dataSource, this.sqlDialect, this.databaseStructure, this.promptTemplate, this.ChatModel, this.maxRetries);
        }

        public String toString() {
            return "SqlContentRetriever.SqlContentRetrieverBuilder(dataSource=" + this.dataSource + ", sqlDialect=" + this.sqlDialect + ", databaseStructure=" + this.databaseStructure + ", promptTemplate=" + this.promptTemplate + ", ChatModel=" + this.ChatModel + ", maxRetries=" + this.maxRetries + ")";
        }
    }
}

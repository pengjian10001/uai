package com.uni.uai.mcp.rag;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.elasticsearch.client.RestClient;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationScript;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

public class ESStoreReadTest {

	public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		RestClient restClient = ESStoreTestBase.getRestClient();

		ElasticsearchConfigurationKnn knn= ElasticsearchConfigurationKnn.builder()
		        //numCandidates 的作用：该参数控制从每个分片返回的候选结果数量
		        //需大于maxResults以确保有足够的结果可供筛选（通常设置为 maxResults * 5）。
		        .numCandidates(10)
		        .build();

		
		// 3. 创建 ElasticsearchEmbeddingStore
		ElasticsearchEmbeddingStore embeddingStore = ElasticsearchEmbeddingStore.builder()
				.configuration(knn)
			    .restClient(restClient)
			    .indexName("test_langchain4j")
			    .build();	
		
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
				.apiKey("ba7d8f57-7b1e-4e46-b5ba-38697c018148")
				.baseUrl("https://openapi-ait.ke.com/v1")
				.modelName("text-embedding-ada-002")
				//.dimensions(1536)   //  移除这一行，OpenAI 模型不需要指定维度
				.build();
        
        Embedding queryEmbedding = embeddingModel.embed("员工").content();
        //logger.info(queryEmbedding);
        
        Filter filter = MetadataFilterBuilder.metadataKey("name").isEqualTo("员工文档");
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        		.filter(filter)
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
        for(EmbeddingMatch<TextSegment> match : relevant) {
        	System.out.println(match.score()); 
        	System.out.println(match.embedded().text());
        }

        restClient.close();
	}

}

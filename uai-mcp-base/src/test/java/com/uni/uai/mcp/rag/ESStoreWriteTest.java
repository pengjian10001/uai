package com.uni.uai.mcp.rag;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

public class ESStoreWriteTest {

	public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

		RestClient restClient = ESStoreTestBase.getRestClient();

		// 3. 创建 ElasticsearchEmbeddingStore
		ElasticsearchEmbeddingStore embeddingStore = ElasticsearchEmbeddingStore.builder()
			    .restClient(restClient)
			    .indexName("test_langchain4j")
			    .build();
			
		ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
		//加载单个文档
		Document document = FileSystemDocumentLoader.loadDocument("/Users/pengjian/Downloads/贝壳职业道德行为守则.pdf", parser);

		
		//DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        //List<TextSegment> segments = splitter.split(document);
		
		// given
        TextSegment segment1 = TextSegment.from("一些带看相关的信息", Metadata.metadata("name", "业务报告"));
        TextSegment segment2 = TextSegment.from("一些员工守则", Metadata.metadata("name", "员工文档"));

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
				.apiKey("ba7d8f57-7b1e-4e46-b5ba-38697c018148")
				.baseUrl("https://openapi-ait.ke.com/v1")
				.modelName("text-embedding-ada-002")
				//.dimensions(1536)   //  移除这一行，OpenAI 模型不需要指定维度
				.build();
        
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        
        embeddingStore.addAll(List.of("9412bbf5-d3cc-4edd-ae88-79a648b9cf7b"), List.of(embedding1), List.of(segment1));
        embeddingStore.add(embedding2, segment2);
		

		restClient.close();

	}

}

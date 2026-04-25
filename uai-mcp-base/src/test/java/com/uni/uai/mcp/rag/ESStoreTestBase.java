package com.uni.uai.mcp.rag;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.elasticsearch.client.RestClient;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

public class ESStoreTestBase {

	public static RestClient getRestClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		// 创建信任所有证书的 SSL 上下文
		SSLContext sslContext = SSLContextBuilder
		    .create()
		    .loadTrustMaterial(new TrustStrategy() {
		        @Override
		        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		            return true; // 信任所有证书
		        }
		    })
		    .build();
		String apiKey = "OUp2TFZKZ0JLc3QzLWpXOGU4Vlk6UzhQUTREbXFSSUN2bFJVWm5QNVNOZw==";
		
		RestClient restClient = RestClient
			    .builder(HttpHost.create("https://localhost:9200"))
			    .setHttpClientConfigCallback(httpClientBuilder -> 
		            httpClientBuilder
		                .setSSLContext(sslContext)
		                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // 禁用主机名验证
		        )
			    .setDefaultHeaders(new BasicHeader[]{
			        new BasicHeader("Authorization", "ApiKey " + apiKey)
			    })
			    .build();
		
		return restClient;
	}

}

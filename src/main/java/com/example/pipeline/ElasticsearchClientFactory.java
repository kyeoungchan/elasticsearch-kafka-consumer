package com.example.pipeline;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;

public class ElasticsearchClientFactory {

    public static ElasticsearchAsyncClient create(ElasticSearchSinkConnectorConfig config) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                        config.getString(config.ES_USERNAME),
                        config.getString(config.ES_PASSWORD)
                )
        );

        RestClient restClient = RestClient.builder(
                        new HttpHost(config.getString(config.ES_CLUSTER_HOST), config.getInt(config.ES_CLUSTER_PORT), "https")
                )
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    // SSL 검증을 우회하는 SSLContext 생성
                    SSLContext sslContext = null;
                    try {
                        sslContext = SSLContextBuilder.create()
                                .loadTrustMaterial(null, (chain, authType) -> true)
                                .build();
                    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                        throw new RuntimeException(e);
                    }

                    return httpClientBuilder
                            .setSSLContext(sslContext)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // 호스트명 검증 비활성화
                            .setDefaultCredentialsProvider(credentialsProvider);
                })
                .build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchAsyncClient(transport);
    }
}

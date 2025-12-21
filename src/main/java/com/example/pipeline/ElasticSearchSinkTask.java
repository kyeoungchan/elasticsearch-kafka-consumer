package com.example.pipeline;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

@Slf4j
public class ElasticSearchSinkTask extends SinkTask {

    private ElasticSearchSinkConnectorConfig config;
    private ElasticsearchClient esClient;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        try {
            config = new ElasticSearchSinkConnectorConfig(props);

            log.info("=== ES Connector Configuration ===");
            log.info("ES Host: {}", config.getString(config.ES_CLUSTER_HOST));
            log.info("ES Port: {}", config.getInt(config.ES_CLUSTER_PORT));
            log.info("ES Index: {}", config.getString(config.ES_INDEX));
            log.info("ES Username: {}", config.getString(config.ES_USERNAME));
            log.info("================================");

        } catch (ConfigException e) {
            throw new ConnectException(e.getMessage(), e);
        }

        // 엘라스틱서치에 적재하기 위해 ElasticsearchClient를 생성한다.
        this.esClient = ElasticsearchClientFactory.create(config);
    }

    @Override
    public void put(Collection<SinkRecord> records) {

        /* 레코드가 1개 이상으로 들어올 경우, 엘라스틱서치로 전송하기 위한 BulkRequest 인스턴스를 생성한다.
         * BulkRequest: 1개 이상의 데이터들을 묶음으로 엘라스틱서치로 전송
         * 레코드들을 BulkRequest 인스턴스에 추가 */
        if (!records.isEmpty()) {
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
            for (SinkRecord record : records) {

                Object value = record.value();

                if (value == null) {
                    log.warn("Skipping tombstone record: {}", record);
                    continue;
                }

                Map<String, Object> map;

                /* BulkRequest에 데이터를 추가할 때는 Map 타입의 데이터와 인덱스 이름이 필요하다.
                 * 토픽의 메시지 값은 JSON 형태의 String 타입이므로 JSON을 Map으로 변환할 때는 gson 라이브러리를 사용하여 변환한다.
                 * 인덱스는 사용자로부터 받은 값을 기반으로 설정한다. */

                // 이미 Map이면 그대로 사용
                if (value instanceof Map<?, ?>) {
                    map = (Map<String, Object>) value;
                } else if (value instanceof String) {
                    try {
                        map = new Gson().fromJson(value.toString(), Map.class);
                    } catch (Exception e) {
                        log.error("Invalid JSON: {}", value);
                        continue;
                    }
                } else {
                    log.error("Unsupported record value type: {} -> {}",
                            value.getClass(), value);
                    continue;
                }
                bulkRequestBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(config.getString(config.ES_INDEX))
                                .document(map)
                        )
                );
                log.info("record: {}", record);
            }
            BulkRequest bulkRequest = bulkRequestBuilder.build();

            // Retry 로직 추가
            int maxRetries = 3;
            for (int i = 1; i <= maxRetries; i++) {
                try {
                    // 동기 방식으로 변경
                    BulkResponse response = esClient.bulk(bulkRequest);
                    if (response.errors()) {
                        log.error("Bulk request failed: {}", response);
                    } else {
                        log.info("Bulk save Success");
                    }
                    // 성공하면 리턴
                    return;
                } catch (Exception e) {
                    log.error("Bulk request failed (attempt {}/{}): {}", i, maxRetries, e.getMessage(), e);
                    if (i == maxRetries) {
                        throw new ConnectException("Bulk request failed", e);
                    }

                    // 재시도 전 대기
                    try {
                        Thread.sleep(1000L * i);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new ConnectException("Interrupted during retry", ex);
                    }
                }
            }

        }
    }

    /**
     * flush() 메서드는 일정 주기마다 호출
     * put() 메서드에서 엘라스틱서치로 데이터를 전송하므로 여기서는 크게 중요치 않다.
     */
    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        log.info("flush");
    }

    /**
     * 커넥터가 종료될 경우 엘라스틱서치와 연동하는 esClient 변수를 안전하게 종료한다.
     */
    @Override
    public void stop() {
        try {
            esClient._transport().close();
        } catch (IOException e) {
            log.info(e.getMessage(), e);
        }
    }
}

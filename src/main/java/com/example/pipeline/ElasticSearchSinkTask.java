package com.example.pipeline;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
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
    private ElasticsearchAsyncClient esClient;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        try {
            config = new ElasticSearchSinkConnectorConfig(props);
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


                if (record.value() == null) {
                    log.warn("Skipping tombstone record: {}", record);
                    continue;
                }

                Gson gson = new Gson();
                /* BulkRequest에 데이터를 추가할 때는 Map 타입의 데이터와 인덱스 이름이 필요하다.
                 * 토픽의 메시지 값은 JSON 형태의 String 타입이므로 JSON을 Map으로 변환할 때는 gson 라이브러리를 사용하여 변환한다.
                 * 인덱스는 사용자로부터 받은 값을 기반으로 설정한다. */
                try {
                    Map<String, Object> map = gson.fromJson(record.value().toString(), Map.class);
                    bulkRequestBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(config.getString(config.ES_INDEX))
                                    .document(map)
                            )
                    );
                    log.info("record: {}", record);
                } catch (JsonSyntaxException e) {
                    log.error("Invalid JSON: {}", record.value());
                    throw e;
                }
            }
            BulkRequest bulkRequest = bulkRequestBuilder.build();

            esClient.bulk(bulkRequest)
                    // 데이터 전송 결과를 비동기로 받아서 확인
                    .whenComplete((bulkResponse, ex) -> {
                        if (ex != null) {
                            log.error(ex.getMessage(), ex);
                        } else {
                            if (bulkResponse.errors()) {
                                // 실패한 항목들의 상세 정보 로깅
                                bulkResponse.items().forEach(item -> {
                                    if (item.error() != null) {
                                        log.error("Failed to index document: {}", item.error().reason());
                                    }
                                });
                            } else {
                                log.info("bulk save success");
                            }
                        }
                    });
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

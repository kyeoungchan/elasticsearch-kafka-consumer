package com.example.pipeline;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.google.gson.Gson;
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

        // 엘라스틱서치에 적재하기 위해 ElasticsearchClient를 적재한다.
        this.esClient = ElasticsearchClientFactory.create(config);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (!records.isEmpty()) {
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
            for (SinkRecord record : records) {
                Gson gson = new Gson();
                Map<String, Object> map = gson.fromJson(record.value().toString(), Map.class);
                bulkRequestBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(config.getString(config.ES_INDEX))
                                .document(map)
                        )
                );
                log.info("record: {}", record);
            }
            BulkRequest bulkRequest = bulkRequestBuilder.build();

            esClient.bulk(bulkRequest)
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

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        log.info("flush");
    }

    @Override
    public void stop() {
        try {
            esClient.close();
        } catch (IOException e) {
            log.info(e.getMessage(), e);
        }
    }
}

package com.example.pipeline;

import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

/**
 * 커넥터는 템플릿처럼 재사용을 높이는 데에 중점을 둔다.
 * elasticsearch에 저장할 때 필요한 설정을 선언한다.
 */
public class ElasticSearchSinkConnectorConfig extends AbstractConfig {

    // 토픽의 데이터를 저장할 엘라스틱서치 호스트 이름 설정
    public static final String ES_CLUSTER_HOST = "es.host";
    private static final String ES_CLUSTER_HOST_DEFAULT_VALUE = "localhost";
    private static final String ES_CLUSTER_HOST_DOC = "엘라스틱서치 호스트 입력";

    // 토픽의 데이터를 저장할 엘라스틱서치 포트 이름 설정
    public static final String ES_CLUSTER_PORT = "es.port";
    private static final String ES_CLUSTER_PORT_DEFAULT_VALUE = "9200";
    private static final String ES_CLUSTER_PORT_DOC = "엘라스틱서치 포트 입력";

    // 토픽의 데이터를 저장할 엘라스틱서치 인덱스 이름 설정
    public static final String ES_INDEX = "es.index";
    private static final String ES_INDEX_DEFAULT_VALUE = "kafka-connector-index";
    private static final String ES_INDEX_DOC = "엘라스틱서치 인덱스 입력";

    public static final String ES_USERNAME = "elastic";
    public static final String ES_PASSWORD = "huiAcuWoRCmn0viudl2H";

    /* 앞서 설정한 es.host, es.port, es.index를 ConfigDef 클래스로 생성
     * ConfigDef 인스턴스는 커넥터에서 설정값이 정상적으로 들어왔는지 검증하기 위해 사용된다. */
    public static ConfigDef CONFIG = new ConfigDef()
            .define(ES_CLUSTER_HOST, Type.STRING, ES_CLUSTER_HOST_DEFAULT_VALUE, Importance.HIGH, ES_CLUSTER_HOST_DOC)
            .define(ES_CLUSTER_PORT, Type.INT, ES_CLUSTER_PORT_DEFAULT_VALUE, Importance.HIGH, ES_CLUSTER_PORT_DOC)
            .define(ES_INDEX, Type.STRING, ES_INDEX_DEFAULT_VALUE, Importance.HIGH, ES_INDEX_DOC);

    public ElasticSearchSinkConnectorConfig(Map<String, String> props) {
        super(CONFIG, props);
    }
}

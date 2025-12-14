package com.example.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkConnector;

/**
 * 커넥터를 생성했을 때 최초로 실행된다.
 * 데이터를 적재하는 로직을 포함한느 것은 아니고, Task를 실행하기 위한 이전 단계로서 설정값을 확인하고 Task 클래스를 지정하는 역할을 수행한다.
 */
@Slf4j
public class ElasticSearchSinkConnector extends SinkConnector {

    private Map<String, String> configProperties;

    /**
     * 커넥터의 버전 설정
     */
    @Override
    public String version() {
        return "1.0";
    }

    /**
     * 커넥터가 최초로 실행될 때 실행되는 구문
     * 사용자로부터 설정값을 가져와서 ElasticSearchSinkConnectorConfig 인스턴스 생성
     */
    @Override
    public void start(Map<String, String> props) {
        this.configProperties = props;
        try {
            new ElasticSearchSinkConnectorConfig(props);
        } catch (ConfigException e) {
            throw new ConnectException(e.getMessage(), e);
        }
    }

    /**
     * 커넥터를 실행했을 경우 Task 역할을 할 클래스를 선언
     * 만약 다수의 Task를 운영할 경우 Task 클래스 분기 로직을 넣을 수 있다.
     * 예를 들어, 타깃 애플리케이션의 버전에 맞는 로직을 담은 Task 클래스들을 2개 이상 만드는 경우 사용자가 원하는 Task 클래스를 taskClass()에서 선택할 수 있다.
     */
    @Override
    public Class<? extends Task> taskClass() {
        return ElasticSearchSinkTask.class;
    }

    /**
     * Task 별로 다른 설정값을 부여할 경우에 여기에서 로직을 넣는다.
     * 여기서는 모든 Task에 동일한 설정값 부여
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        Map<String, String> taskProps = new HashMap<>();
        taskProps.putAll(configProperties);
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(taskProps);
        }
        return taskConfigs;
    }

    /**
     * ElasticSearchSinkConnectorConfig에서 설정한 설정값 반환
     * ➡ 사용자가 커넥터를 생성할 때 설정값을 정상적으로 입력했는지 검증 시 사용
     */
    @Override
    public ConfigDef config() {
        return ElasticSearchSinkConnectorConfig.CONFIG;
    }

    /**
     * 커넥터 종료 시 로그 출력
     */
    @Override
    public void stop() {
        log.info("Stop Elasticsearch Connector");
    }
}

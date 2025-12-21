# 🧑🏻‍💻 엘라스틱서치 싱크 커넥터 로직
![elasticsearch_sink_connector_logic.jpeg](img/elasticsearch_sink_connector_logic.jpeg)  

# 🧑🏻‍💻 실행 가이드
```shell
# 빌드를 통해 jar 파일 생성
$ ./gradlew build

# build/libs에 jar 파일 생성 확인
$ ls build/libs

# 로컬에 카프카 플러그인 디렉토리 생성
$ cd ~/kafka
$ mkdir plugins

# 다시 프로젝트 libs 경로로 돌아온 후 복붙
$ cd /Users/kyeongchanwoo/projects/elasticsearch-kafka-consumer/build/libs
$ cp elasticsearch-kafka-consumer-0.0.1-SNAPSHOT.jar ~/kafka/plugins

# 새로 jar 파일 생성 후 덮어쓰기를 바로 하고 싶다면
$ cp -i build/libs/elasticsearch-kafka-consumer-0.0.1-SNAPSHOT.jar ~/kafka/plugins
```

```shell
# 로컬 카프카 config/connect-distributed.properties 내용 수정
bootstrap.servers=my-kafka:9092
# 상대경로로 접근
plugin.path=plugins
```

```shell
# 엘라스틱서치 싱크 커넥터를 포함된 커넥트 실행
$ bin/connect-distributed.sh config/connect-distributed.properties
```

```shell
# 분산모드 카프카 커넥트에 엘라스틱서치 커넥터 추가됐는지 확인
$ curl http://localhost:8083/connector-plugins
# 아래의 결과를 보면 엘라스틱서치 커넥터가 추가된 것을 확인할 수 있다.
[
  {
    "class":"com.example.pipeline.ElasticSearchSinkConnector",
    "type":"sink",
    "version":"1.0"
  },
  {
    "class":"org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
    "type":"source",
    "version":"3.9.0"
  },
  {
    "class":"org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
    "type":"source",
    "version":"3.9.0"
  },
  {
    "class":"org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "type":"source","version":
    "3.9.0"
  }
]
```

```shell
# 엘라스틱서치 싱크 커넥터 실행
$ curl -L -X POST 'localhost:8083/connectors' \
-H 'Content-Type: application/json' \
--data-raw '{
  "name": "es-sink-connector",
  "config": {
      "connector.class": "com.example.pipeline.ElasticSearchSinkConnector",
      "topics": "select-color",
      "es.host": "localhost",
      "es.port": "9200",
      "es.index": "kafka-to-es",
      "es.username": "elastic",
      "es.password": "password"
  }
}'
```

```shell
$ curl -X GET http://localhost:8083/connectors
# 결과
["es-sink-connector"]
```
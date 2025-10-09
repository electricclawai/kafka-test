# Kafka Functional Testing Project

This project demonstrates Kafka producer and consumer functionality with comprehensive functional tests running in GitHub Actions.

## Project Structure

```
.
├── .github/
│   └── workflows/
│       └── kafka-ct.yaml          # GitHub Actions workflow
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/kafka/
│   │   │       ├── KafkaApplication.java
│   │   │       ├── producer/
│   │   │       │   └── SimpleKafkaProducer.java
│   │   │       └── consumer/
│   │   │           └── SimpleKafkaConsumer.java
│   │   └── resources/
│   │       └── logback.xml
│   └── functionalTest/
│       ├── java/
│       │   └── com/example/kafka/functional/
│       │       └── KafkaFunctionalTest.java
│       └── resources/
│           └── logback-test.xml
├── build.gradle                   # Gradle build configuration
├── settings.gradle                # Gradle settings
├── gradlew                        # Gradle wrapper (Unix)
├── gradlew.bat                    # Gradle wrapper (Windows)
└── README.md
```

## Features

- **Simple Kafka Producer**: Sends messages to Kafka topics
- **Simple Kafka Consumer**: Consumes messages from Kafka topics
- **Functional Tests**: Comprehensive tests for:
  - Kafka connectivity
  - Single message produce/consume
  - Multiple messages produce/consume
  - Message ordering verification
  - Messages with null keys

## Prerequisites

- Java 17 or higher
- Gradle 8.5 (included via wrapper)
- Docker and Docker Compose (for local testing)

## Running Locally

### 1. Start Kafka locally

```bash
docker-compose up -d
```

### 2. Build the project

```bash
./gradlew build
```

### 3. Run functional tests

```bash
./gradlew functionalTest
```

### 4. Stop Kafka

```bash
docker-compose down
```

## GitHub Actions Workflow

The project includes a GitHub Actions workflow (`.github/workflows/kafka-ct.yaml`) that:

1. Sets up JDK 17
2. Starts Kafka and Zookeeper using Docker Compose
3. Waits for Kafka to be ready
4. Builds the project
5. Runs functional tests
6. Cleans up containers

### Workflow Triggers

- Manual trigger via `workflow_dispatch`
- Push to `sc971` branch

### Running the Workflow

1. Push your code to the `sc971` branch, or
2. Manually trigger via GitHub Actions UI

## Configuration

### Kafka Broker Configuration

The Kafka broker address can be configured via:
- Environment variable: `KAFKA_BROKER`
- System property: `kafka.broker`
- Default: `localhost:9092`

### Test Topics

Tests automatically create unique topics with UUID suffixes to avoid conflicts.

## Dependencies

- **Apache Kafka**: 3.6.0
- **JUnit Jupiter**: 5.10.0
- **AssertJ**: 3.24.2
- **Awaitility**: 4.2.0
- **SLF4J & Logback**: For logging

## Docker Compose Configuration

The workflow uses Wurstmeister images for Kafka and Zookeeper:

```yaml
version: '3.8'
services:
  zookeeper:
    image: wurstmeister/zookeeper:latest
    ports:
      - "2181:2181"

  kafka:
    image: wurstmeister/kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT://kafka:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_BROKER_ID: 1
```

## Test Execution

Tests are executed in order and include:

1. **Connectivity Test**: Verifies Kafka broker is accessible
2. **Single Message Test**: Tests basic produce/consume functionality
3. **Multiple Messages Test**: Tests batch processing
4. **Ordering Test**: Verifies message ordering within partitions
5. **Null Key Test**: Tests messages without keys

## Logging

Logging is configured via Logback:
- Application logs: `INFO` level
- Kafka client logs: `WARN` level
- Test logs: `DEBUG` level for application, `INFO` for others

## Troubleshooting

### Kafka not ready

If tests fail with connectivity issues:
1. Check Docker logs: `docker logs kafka`
2. Verify Kafka is running: `docker ps`
3. Check network connectivity to Kafka broker

### Test failures

- Tests use Awaitility with 30-second timeouts
- Check logs for detailed error messages
- Verify Kafka broker address matches your environment

## License

This project is provided as-is for educational and testing purposes.

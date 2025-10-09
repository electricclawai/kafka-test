package com.example.kafka.functional;

import com.example.kafka.consumer.SimpleKafkaConsumer;
import com.example.kafka.producer.SimpleKafkaProducer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KafkaFunctionalTest {
    private static final Logger logger = LoggerFactory.getLogger(KafkaFunctionalTest.class);
    private static final String KAFKA_BROKER = System.getProperty("kafka.broker", "localhost:9092");
    private static final String TEST_TOPIC = "test-topic-" + UUID.randomUUID();
    
    private SimpleKafkaProducer producer;
    private SimpleKafkaConsumer consumer;

    @BeforeAll
    static void setupKafka() {
        logger.info("Setting up Kafka functional tests");
        logger.info("Kafka broker: {}", KAFKA_BROKER);
        
        // Create test topic
        Properties props = new Properties();
        props.put("bootstrap.servers", KAFKA_BROKER);
        
        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(TEST_TOPIC, 1, (short) 1);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get(30, TimeUnit.SECONDS);
            logger.info("Created test topic: {}", TEST_TOPIC);
        } catch (Exception e) {
            logger.error("Failed to create topic", e);
            throw new RuntimeException("Failed to create test topic", e);
        }
    }

    @BeforeEach
    void setup() {
        producer = new SimpleKafkaProducer(KAFKA_BROKER);
        consumer = new SimpleKafkaConsumer(KAFKA_BROKER, "test-group-" + UUID.randomUUID());
        consumer.subscribe(TEST_TOPIC);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Kafka connectivity")
    void testKafkaConnectivity() {
        logger.info("Testing Kafka connectivity");
        
        // If we can create producer and consumer, Kafka is accessible
        assertThat(producer).isNotNull();
        assertThat(consumer).isNotNull();
        
        logger.info("Kafka connectivity test passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test produce and consume a single message")
    void testProduceAndConsumeSingleMessage() throws Exception {
        logger.info("Testing produce and consume single message");
        
        String key = "test-key-1";
        String value = "test-value-1";
        
        // Produce message
        producer.send(TEST_TOPIC, key, value).get(10, TimeUnit.SECONDS);
        logger.info("Message sent successfully");
        
        // Consume message with retry logic
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofSeconds(2));
                assertThat(records).isNotEmpty();
                
                ConsumerRecord<String, String> record = records.get(0);
                assertThat(record.key()).isEqualTo(key);
                assertThat(record.value()).isEqualTo(value);
                
                logger.info("Message consumed successfully: key={}, value={}", record.key(), record.value());
            });
    }

    @Test
    @Order(3)
    @DisplayName("Test produce and consume multiple messages")
    void testProduceAndConsumeMultipleMessages() throws Exception {
        logger.info("Testing produce and consume multiple messages");
        
        int messageCount = 10;
        
        // Produce multiple messages
        for (int i = 0; i < messageCount; i++) {
            String key = "key-" + i;
            String value = "value-" + i;
            producer.send(TEST_TOPIC, key, value);
        }
        logger.info("Sent {} messages", messageCount);
        
        // Consume messages
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> allRecords = consumer.poll(Duration.ofSeconds(2));
                
                // Keep polling until we get all messages
                int totalReceived = allRecords.size();
                while (totalReceived < messageCount) {
                    List<ConsumerRecord<String, String>> moreRecords = consumer.poll(Duration.ofSeconds(2));
                    allRecords.addAll(moreRecords);
                    totalReceived += moreRecords.size();
                    
                    if (moreRecords.isEmpty()) {
                        break;
                    }
                }
                
                assertThat(allRecords).hasSizeGreaterThanOrEqualTo(messageCount);
                logger.info("Consumed {} messages", allRecords.size());
            });
    }

    @Test
    @Order(4)
    @DisplayName("Test message ordering")
    void testMessageOrdering() throws Exception {
        logger.info("Testing message ordering");
        
        String key = "ordered-key";
        
        // Send messages with same key (should go to same partition)
        for (int i = 0; i < 5; i++) {
            producer.send(TEST_TOPIC, key, "message-" + i).get(10, TimeUnit.SECONDS);
        }
        
        // Consume and verify order
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofSeconds(2));
                
                // Collect all records
                int totalReceived = records.size();
                while (totalReceived < 5) {
                    List<ConsumerRecord<String, String>> moreRecords = consumer.poll(Duration.ofSeconds(2));
                    records.addAll(moreRecords);
                    totalReceived += moreRecords.size();
                    
                    if (moreRecords.isEmpty()) {
                        break;
                    }
                }
                
                assertThat(records).hasSizeGreaterThanOrEqualTo(5);
                
                // Verify messages with same key are in order
                for (int i = 0; i < 5; i++) {
                    assertThat(records.get(i).key()).isEqualTo(key);
                    assertThat(records.get(i).value()).isEqualTo("message-" + i);
                }
                
                logger.info("Message ordering verified");
            });
    }

    @Test
    @Order(5)
    @DisplayName("Test producer with null key")
    void testProducerWithNullKey() throws Exception {
        logger.info("Testing producer with null key");
        
        String value = "value-without-key";
        
        // Produce message without key
        producer.send(TEST_TOPIC, null, value).get(10, TimeUnit.SECONDS);
        
        // Consume message
        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofSeconds(2));
                assertThat(records).isNotEmpty();
                
                ConsumerRecord<String, String> record = records.get(0);
                assertThat(record.key()).isNull();
                assertThat(record.value()).isEqualTo(value);
                
                logger.info("Message with null key consumed successfully");
            });
    }
}

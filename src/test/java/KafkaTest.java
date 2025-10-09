import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class KafkaTest {
    static String BROKER = System.getenv().getOrDefault("KAFKA_BROKER", "localhost:9092");
    static String TOPIC = "test-" + UUID.randomUUID();
    KafkaProducer<String, String> producer;
    KafkaConsumer<String, String> consumer;

    @BeforeAll
    static void createTopic() throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", BROKER);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1))).all().get();
        }
    }

    @BeforeEach
    void setup() {
        Properties pProps = new Properties();
        pProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER);
        pProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        pProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaProducer<>(pProps);

        Properties cProps = new Properties();
        cProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER);
        cProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        cProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(cProps);
        consumer.subscribe(List.of(TOPIC));
    }

    @AfterEach
    void cleanup() {
        producer.close();
        consumer.close();
    }

    @Test
    void testSendAndReceive() throws Exception {
        producer.send(new ProducerRecord<>(TOPIC, "key1", "value1")).get();
        
        ConsumerRecords<String, String> records = ConsumerRecords.empty();
        for (int i = 0; i < 10 && records.isEmpty(); i++) {
            records = consumer.poll(Duration.ofSeconds(2));
        }
        
        assertFalse(records.isEmpty());
        ConsumerRecord<String, String> record = records.iterator().next();
        assertEquals("key1", record.key());
        assertEquals("value1", record.value());
    }
}

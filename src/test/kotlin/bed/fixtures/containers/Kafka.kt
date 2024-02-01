package bed.fixtures.containers

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.CreateTopicsResult
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS


class Kafka(network: Network) {
    val container: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))
        .withNetwork(network)
        .also { it.start() }
        .also { createTopics(it) }

    val topic: String = "hospitalbed.events"
    private fun createTopics(kafka: KafkaContainer): CreateTopicsResult? {
        return listOf(NewTopic(topic, 1, 1))
            .let {
                AdminClient
                    .create(mapOf(Pair(BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)))
                    .createTopics(it)
            }
    }

    fun buildConsumer(): KafkaConsumer<String, String> =
        KafkaConsumer(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to container.bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "tc-${UUID.randomUUID()}",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
                ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to "100"
            ),
            StringDeserializer(),
            StringDeserializer()
        )

    companion object {
        fun drain(
            consumer: KafkaConsumer<String, String>,
            expectedRecordCount: Int
        ): List<ConsumerRecord<String, String>> {
            val allRecords: MutableList<ConsumerRecord<String, String>> = ArrayList()
            Unreliables.retryUntilTrue(20, SECONDS) {
                consumer.poll(Duration.ofMillis(50))
                    .iterator()
                    .forEachRemaining { allRecords.add(it) }
                allRecords.size == expectedRecordCount
            }
            return allRecords
        }
    }
}

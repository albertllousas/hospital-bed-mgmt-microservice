package bed.fixtures.containers

import io.debezium.testing.testcontainers.ConnectorConfiguration
import io.debezium.testing.testcontainers.DebeziumContainer
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer


class Debezium(kafka: Kafka, postgres: Postgres) {

    private val LOGGER = LoggerFactory.getLogger(Debezium::class.java)

    val container = DebeziumContainer("quay.io/debezium/connect:2.4.1.Final")
        .withNetwork(kafka.container.network)
        .withKafka(kafka.container)
//        .withLogConsumer(Slf4jLogConsumer(LOGGER))
        .dependsOn(kafka.container)
        .also {

            it.start()
            val config = ConnectorConfiguration.forJdbcContainer(postgres.container)
                .with("transforms", "outbox")
                .with("plugin.name", "pgoutput")
                .with("topic.prefix", "hospital")
                .with("table.include.list", "public.outbox")
                .with("table.whitelist", "public.outbox")
                .with("value.converter", "io.debezium.converters.BinaryDataConverter")
                .with("value.converter.delegate.converter.type", "org.apache.kafka.connect.json.JsonConverter")
                .with("value.converter.delegate.converter.type.schemas.enable", "false")
                .with("transforms.outbox.table.field.event.key", "id")
                .with("transforms.outbox.table.field.event.type", "event_type")
                .with("transforms.outbox.route.tombstone.on.empty.payload", "true")
                .with("transforms.outbox.route.topic.replacement", "hospitalbed.events")
                .with("transforms.outbox.type", "io.debezium.transforms.outbox.EventRouter")
            it.registerConnector("my-connector", config)
        }

}

package bed.imperative.shell.outputs.db

import bed.functional.core.BedCreated
import bed.functional.core.Transaction
import bed.fixtures.TestBuilders
import bed.fixtures.containers.Debezium
import bed.fixtures.containers.Kafka
import bed.fixtures.containers.Postgres
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Tag("integration")
class DebeziumEventPublisherTest {

    private val network: Network = Network.newNetwork()

    private val db = Postgres(network)

    private val kafka = Kafka(network)

    private val debezium = Debezium(kafka, db)

    @AfterEach
    fun `tear down`() {
        kafka.container.stop()
        db.container.stop()
        debezium.container.stop()
    }

    private val handle = Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password).open()

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.950Z"), ZoneId.of("UTC"))

    private val debeziumHandler = DebeziumEventPublisher(clock = clock)

    private val consumer = kafka.buildConsumer().also { it.subscribe(listOf(kafka.topic)) }

    @Test
    fun `should dispatch domain event to the outbox table and captured by the CDC tool`() {
        val domainEvent = BedCreated(TestBuilders.buildBed())

        debeziumHandler.publish(domainEvent, JdbiTransaction(handle))

        Kafka.drain(consumer, 1).first().also {
            val payload = debeziumHandler.objectMapper.readValue<ExternalHospitalBedEvent>(it.value())
            payload.eventType shouldBe "hospital_bed_created_event"
            payload.hospitalBed.id shouldBe domainEvent.bed.id.value
        }
        handle.createQuery("select count(*) from outbox").mapTo(Long::class.java).one() shouldBe 0
    }

    object NonJdbiTransaction : Transaction

    @Test
    fun `should crash when the transaction is not a jdbi one`() {
        val exception = shouldThrow<IllegalArgumentException> {
            debeziumHandler.publish(BedCreated(TestBuilders.buildBed()), NonJdbiTransaction)
        }
        exception.message shouldBe "Unrecognized transaction type: NonJdbiTransaction"
    }
}
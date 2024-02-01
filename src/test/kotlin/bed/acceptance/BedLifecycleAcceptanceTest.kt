package bed.acceptance

import bed.fixtures.EagerApplicationContextBuilder
import bed.fixtures.call
import bed.fixtures.containers.Debezium
import bed.fixtures.containers.Kafka
import bed.fixtures.containers.Postgres
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.CREATED
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.testcontainers.containers.Network

@Tag("acceptance")
@MicronautTest(contextBuilder = [EagerApplicationContextBuilder::class])
@Property(name = "mocks.http.factory.enabled", value = "false")
@TestInstance(PER_CLASS)
class BedLifecycleAcceptanceTest : TestPropertyProvider {

    @Inject
    @Client("/")
    private lateinit var client: HttpClient

    private val network: Network = Network.newNetwork()

    private val db = Postgres(network)

    private val kafka = Kafka(network)

    private val consumer = kafka.buildConsumer().also { it.subscribe(listOf(kafka.topic)) }

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun getProperties() = mutableMapOf<String, String>(
        "datasource.jdbc-url" to db.container.jdbcUrl,
        "datasource.username" to db.container.username,
        "datasource.password" to db.container.password,
    )

    private val debezium = Debezium(kafka, db)

    @AfterEach
    fun `tear down`() {
        kafka.container.stop()
        db.container.stop()
        debezium.container.stop()
    }

    @Test
    fun `should create a bed`() {
        val request: HttpRequest<Any> = HttpRequest.POST(
            "/beds",
            """ { "room_id":"B011", "ward": "CARDIOLOGY", "features": ["ELECTRIC"] } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe CREATED
        Kafka.drain(consumer, 1).first().also {
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            val payload = mapper.readValue(it.value(), typeRef)
            payload["event_type"] shouldBe "hospital_bed_created_event"
        }
    }
}

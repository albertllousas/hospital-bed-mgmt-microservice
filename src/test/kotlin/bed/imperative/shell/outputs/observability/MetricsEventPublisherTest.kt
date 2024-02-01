package bed.imperative.shell.outputs.observability

import bed.fixtures.TestBuilders
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedCreated
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import io.micrometer.core.instrument.Tag as MetricTag

@Tag("integration")
class MetricsEventPublisherTest {

    private val metrics = SimpleMeterRegistry()

    private val metricsEventPublisher = MetricsEventPublisher(metrics)

    @Test
    fun `publish a metric for a domain event`() {
        val event = BedCreated(TestBuilders.buildBed())

        metricsEventPublisher.publish(event)

        metrics.counter("domain.event", listOf(MetricTag.of("type", "BedCreated"))).count() shouldBe 1.0
    }

    @Test
    fun `publish a metric for a domain error`() {
        val error = BedAlreadyAllocated

        metricsEventPublisher.publish(error, this::class)

        metrics.counter(
            "domain.error",
            listOf(
                MetricTag.of("clazz", "MetricsEventPublisherTest"),
                MetricTag.of("type", "BedAlreadyAllocated")
            )
        ).count() shouldBe 1.0
    }

    @Test
    fun `publish a metric for an application crash`() {
        val boom = IllegalArgumentException("boom")

        metricsEventPublisher.publish(boom)

        metrics.counter(
            "app.crash",
            listOf(
                MetricTag.of("exception", "IllegalArgumentException")
            )
        ).count() shouldBe 1.0
    }
}

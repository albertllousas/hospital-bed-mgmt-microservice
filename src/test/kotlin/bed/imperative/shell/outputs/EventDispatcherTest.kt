package bed.imperative.shell.outputs

import bed.fixtures.TestBuilders
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedCreated
import bed.functional.core.Transaction
import bed.imperative.shell.outputs.db.DebeziumEventPublisher
import bed.imperative.shell.outputs.observability.LogEventWriter
import bed.imperative.shell.outputs.observability.MetricsEventPublisher
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class EventDispatcherTest {

    private val debeziumEventPublisher = mockk<DebeziumEventPublisher>(relaxed = true)
    private val metricsEventPublisher = mockk<MetricsEventPublisher>(relaxed = true)
    private val logEventWriter = mockk<LogEventWriter>(relaxed = true)

    private val eventDispatcher = EventDispatcher(debeziumEventPublisher, metricsEventPublisher, logEventWriter)

    object FakeTransaction : Transaction

    @Test
    fun `should publish domain events`() {
        val event = BedCreated(TestBuilders.buildBed())
        eventDispatcher.publish(listOf(event), FakeTransaction)

        verify { debeziumEventPublisher.publish(event, FakeTransaction) }
        verify { metricsEventPublisher.publish(event) }
        verify { logEventWriter.log(event) }
    }

    @Test
    fun `should report domain errors`() {
        val error = BedAlreadyAllocated
        eventDispatcher.report(error, EventDispatcherTest::class)

        verify { metricsEventPublisher.publish(error, EventDispatcherTest::class) }
        verify { logEventWriter.log(error, EventDispatcherTest::class) }
    }

    @Test
    fun `should report application crashes`() {
        val error = Exception("boom")
        eventDispatcher.report(error)

        verify { metricsEventPublisher.publish(error) }
        verify { logEventWriter.log(error) }
    }
}

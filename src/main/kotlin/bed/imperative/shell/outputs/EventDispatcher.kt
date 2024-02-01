package bed.imperative.shell.outputs

import bed.functional.core.DomainError
import bed.functional.core.DomainEvent
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.Transaction
import bed.imperative.shell.outputs.db.DebeziumEventPublisher
import bed.imperative.shell.outputs.observability.LogEventWriter
import bed.imperative.shell.outputs.observability.MetricsEventPublisher
import jakarta.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class EventDispatcher(
    private val debeziumEventPublisher: DebeziumEventPublisher,
    private val metricsEventPublisher: MetricsEventPublisher,
    private val logEventWriter: LogEventWriter,
): EventPublisher, ErrorReporter {

    override fun publish(events: List<DomainEvent>, transaction: Transaction) {
        events.forEach {
            debeziumEventPublisher.publish(it, transaction)
            logEventWriter.log(it)
            metricsEventPublisher.publish(it)
        }
    }

    override fun <T : Any> report(error: DomainError, clazz: KClass<T>) {
        logEventWriter.log(error, clazz)
        metricsEventPublisher.publish(error, clazz)
    }

    override fun report(crash: Throwable) {
        logEventWriter.log(crash)
        metricsEventPublisher.publish(crash)
    }
}

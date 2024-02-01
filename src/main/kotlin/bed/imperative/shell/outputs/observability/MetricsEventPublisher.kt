package bed.imperative.shell.outputs.observability

import bed.functional.core.DomainError
import bed.functional.core.DomainEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlin.reflect.KClass

class MetricsEventPublisher(private val metrics: MeterRegistry)  {

    fun publish(event: DomainEvent) {
        val tags = listOf(Tag.of("type", event::class.simpleName!!))
        metrics.counter("domain.event", tags).increment()
    }

    fun <T : Any> publish(event: DomainError, clazz: KClass<T>) {
        val tags = listOf(Tag.of("clazz", clazz.simpleName!!), Tag.of("type", event::class.simpleName!!))
        metrics.counter("domain.error", tags).increment()
    }

    fun publish(event: Throwable) {
        val tags = listOf(Tag.of("exception", event::class.simpleName!!))
        metrics.counter("app.crash", tags).increment()
    }
}

package bed.imperative.shell.config

import bed.imperative.shell.outputs.db.DebeziumEventPublisher
import bed.imperative.shell.outputs.observability.LogEventWriter
import bed.imperative.shell.outputs.observability.MetricsEventPublisher
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class EventHandlersBeans {

    @Singleton
    @Bean
    fun loggingHandler(): LogEventWriter = LogEventWriter()

    @Singleton
    @Bean
    fun metricsHandler(meterRegistry: MeterRegistry): MetricsEventPublisher = MetricsEventPublisher(meterRegistry)

    @Singleton
    @Bean
    fun debeziumHandler(): DebeziumEventPublisher = DebeziumEventPublisher()
}

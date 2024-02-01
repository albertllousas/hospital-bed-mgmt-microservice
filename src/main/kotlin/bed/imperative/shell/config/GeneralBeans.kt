package bed.imperative.shell.config

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import java.util.UUID

@Factory
class GeneralBeans {

    @Singleton
    @Bean
    fun generateId(): () -> UUID = { UUID.randomUUID() }

    @Singleton
    @Bean
    fun clock(): java.time.Clock = java.time.Clock.systemUTC()
}
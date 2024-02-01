package bed.imperative.shell.inputs.http

import bed.fixtures.LazyApplicationContextBuilder
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest

@MicronautTest(contextBuilder = [LazyApplicationContextBuilder::class])
@Property(name = "database.beans.factory.enabled", value = "false")
@Property(name = "core.beans.factory.enabled", value = "false")
@Property(name = "mocks.http.factory.enabled", value = "true")
annotation class MicronautHttpTest
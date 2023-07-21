package bed.mgmt

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.kotest.matchers.shouldBe
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class CustomContextBuilder : DefaultApplicationContextBuilder() {
    init {
        eagerInitSingletons(false)
    }
}

@MicronautTest(contextBuilder = [CustomContextBuilder::class])
class MicronautguideTest(
    @Client("/") private val client: HttpClient,
    private val someService: SomeService
) {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }

    @Test
    fun `http works`() {
        val response: Either<String, HttpResponse<String>> = either {
            every {
                someService.msg()
            } returns "hello"
            val request: HttpRequest<Any> = HttpRequest.GET("/hello")
           client.toBlocking().exchange(request, String::class.java)
        }

        response.isRight() shouldBe true
        response.onRight {
            it.status shouldBe HttpStatus.OK
            assertThat(it.body()).isEqualTo("hello")
        }
    }


    @MockBean(SomeService::class)
    fun someService() = mockk<SomeService>()
}


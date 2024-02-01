package bed.imperative.shell.inputs.http

import bed.functional.core.BedFeature.CARDIAC_MONITOR
import bed.functional.core.BedRepository
import bed.functional.core.Ward.ICU
import bed.fixtures.LazyApplicationContextBuilder
import bed.fixtures.TestBuilders
import bed.fixtures.call
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.micronaut.http.HttpStatus.OK
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
@MicronautHttpTest
class FindAvailableBedsHttpControllerTest(
    @Client("/") private val client: HttpClient,
    private val bedRepository: BedRepository,
) {

    @Test
    fun `should find available beds`() {
        val bed = TestBuilders.buildBed(features = listOf(CARDIAC_MONITOR))
        every { bedRepository.findAvailable(ICU, listOf(CARDIAC_MONITOR), 50, 0) } returns listOf(bed)
        val request = HttpRequest.GET<Any>("/beds/available?ward=ICU&features=CARDIAC_MONITOR")

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe OK
        response.body() shouldBe """{"data":[{"id":"${bed.id.value}","room_id":"${bed.roomId.value}","features":["CARDIAC_MONITOR"]}]}"""
    }
}
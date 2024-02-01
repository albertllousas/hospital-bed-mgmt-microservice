package bed.imperative.shell.inputs.http

import bed.functional.core.*
import bed.functional.core.BedFeature.ELECTRIC
import bed.functional.core.Ward.CARDIOLOGY
import bed.fixtures.FakeTransaction
import bed.fixtures.LazyApplicationContextBuilder
import bed.fixtures.TestBuilders
import bed.fixtures.call
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.CREATED
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
@MicronautHttpTest
class CreateBedHttpControllerTest(
    @Client("/") private val client: HttpClient,
    private val bedRepository: BedRepository,
    private val eventPublisher: EventPublisher,
    private val create: (BedId, RoomId, Ward, List<BedFeature>) -> Bed
) {

    @Test
    fun `should create a bed`() {
        val bed = TestBuilders.buildBed()
        every { create(any(), eq(RoomId("B011")), eq(CARDIOLOGY), eq(listOf(ELECTRIC))) } returns bed
        val request: HttpRequest<Any> = HttpRequest.POST(
            "/beds",
            """ { "room_id":"B011", "ward": "CARDIOLOGY", "features": ["ELECTRIC"] } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe CREATED
        response.body() shouldBe """{"bed_id":"${bed.id.value}"}"""
        verify { bedRepository.save(bed, FakeTransaction) }
        verify { eventPublisher.publish(bed.events, FakeTransaction) }
    }
}

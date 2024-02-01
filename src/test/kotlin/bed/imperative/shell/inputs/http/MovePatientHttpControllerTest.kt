package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.functional.core.Bed
import bed.functional.core.BedAllocated
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedNotFound
import bed.functional.core.BedReleased
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.MovePatientError
import bed.fixtures.FakeTransaction
import bed.fixtures.LazyApplicationContextBuilder
import bed.fixtures.TestBuilders
import bed.fixtures.call
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.NOT_FOUND
import io.micronaut.http.HttpStatus.NO_CONTENT
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Clock

@Tag("integration")
@MicronautHttpTest
private class MovePatientHttpControllerTest(
    @Client("/") private val client: HttpClient,
    private val bedRepository: BedRepository,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val move: (Bed, Bed, Clock) -> Either<MovePatientError, Pair<Bed, Bed>>
) {

    @Test
    fun `should move a patient to a bed`() {
        val fromBed = TestBuilders.buildBed()
        val toBed = TestBuilders.buildBed()
        every { bedRepository.find(fromBed.id) } returns fromBed.right()
        every { bedRepository.find(toBed.id) } returns toBed.right()
        every {
            move(eq(fromBed), eq(toBed), any())
        } returns Pair(
            fromBed.copy(events = listOf(BedReleased(fromBed))),
            toBed.copy(events = listOf(BedAllocated(toBed)))
        ).right()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${fromBed.id.value}/move",
            """ { "to_bed":"${toBed.id.value}" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NO_CONTENT
        verify { eventPublisher.publish(listOf(BedReleased(fromBed), BedAllocated(toBed)), FakeTransaction) }
    }

    @Test
    fun `should fail when 'from' bed does not exists`() {
        val from = TestBuilders.buildBed()
        val to = TestBuilders.buildBed()
        every { bedRepository.find(from.id) } returns BedNotFound.left()
        every { bedRepository.find(to.id) } returns to.right()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${from.id.value}/move",
            """ { "to_bed":"${to.id.value}" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NOT_FOUND
        verify { errorReporter.report(BedNotFound, MovePatientHttpController::class) }
    }

    @Test
    fun `should fail when 'to' bed does not exists`() {
        val from = TestBuilders.buildBed()
        val to = TestBuilders.buildBed()
        every { bedRepository.find(from.id) } returns from.right()
        every { bedRepository.find(to.id) } returns BedNotFound.left()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${from.id.value}/move",
            """ { "to_bed":"${to.id.value}" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NOT_FOUND
        verify { errorReporter.report(BedNotFound, MovePatientHttpController::class) }
    }

    @Test
    fun `should fail when moving the patient fails`() {
        val fromBed = TestBuilders.buildBed()
        val toBed = TestBuilders.buildBed()
        every { bedRepository.find(fromBed.id) } returns fromBed.right()
        every { bedRepository.find(toBed.id) } returns toBed.right()
        every { move(eq(fromBed), eq(toBed), any()) } returns BedAlreadyAllocated.left()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${fromBed.id.value}/move",
            """ { "to_bed":"${toBed.id.value}" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldNotBe NO_CONTENT
        verify { errorReporter.report(BedAlreadyAllocated, MovePatientHttpController::class) }
    }
}

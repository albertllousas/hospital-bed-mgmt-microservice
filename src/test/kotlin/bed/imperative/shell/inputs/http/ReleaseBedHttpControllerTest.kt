package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.functional.core.Bed
import bed.functional.core.BedAlreadyReleased
import bed.functional.core.BedNotFound
import bed.functional.core.BedReleased
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.ReleaseError
import bed.fixtures.FakeTransaction
import bed.fixtures.LazyApplicationContextBuilder
import bed.fixtures.TestBuilders
import bed.fixtures.call
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.NOT_FOUND
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.HttpStatus.NO_CONTENT
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
@MicronautHttpTest
class ReleaseBedHttpControllerTest(
    @Client("/") private val client: HttpClient,
    private val bedRepository: BedRepository,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val release: (Bed) -> Either<ReleaseError, Bed> = Bed.Companion::release
) {

    @Test
    fun `should release a bed`() {
        val bed = TestBuilders.buildBed()
        val releasedBed = bed.copy(events = listOf(BedReleased(bed)))
        every { bedRepository.find(bed.id) } returns bed.right()
        every { release(bed) } returns releasedBed.right()
        val request = HttpRequest.PATCH("/beds/${bed.id.value}/release", "")

        val response = client.toBlocking().call(request, Unit::class.java)

        response.status shouldBe NO_CONTENT
        verify { bedRepository.save(releasedBed, FakeTransaction) }
        verify { eventPublisher.publish(releasedBed.events, FakeTransaction) }
    }

    @Test
    fun `should fail when bed does not exists`() {
        val bed = TestBuilders.buildBed()
        every { bedRepository.find(bed.id) } returns BedNotFound.left()
        val request = HttpRequest.PATCH("/beds/${bed.id.value}/release", "")

        val response = client.toBlocking().call(request, Unit::class.java)

        response.status shouldBe NOT_FOUND
        verify { errorReporter.report(BedNotFound, ReleaseBedHttpController::class) }
    }

    @Test
    fun `should fail when bed releasing bed fails`() {
        val bed = TestBuilders.buildBed()
        every { bedRepository.find(bed.id) } returns bed.right()
        every { release(bed) } returns BedAlreadyReleased.left()
        val request = HttpRequest.PATCH("/beds/${bed.id.value}/release", "")

        val response = client.toBlocking().call(request, Unit::class.java)

        response.status shouldNotBe NO_CONTENT
        verify { errorReporter.report(BedAlreadyReleased, ReleaseBedHttpController::class) }
    }
}

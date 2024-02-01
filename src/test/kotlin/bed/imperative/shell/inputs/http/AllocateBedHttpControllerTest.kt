package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.fixtures.FakeTransaction
import bed.fixtures.FakeTransactional
import bed.fixtures.TestBuilders
import bed.fixtures.call
import bed.functional.core.AllocateError
import bed.functional.core.Bed
import bed.functional.core.BedAllocated
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.BedNotFound
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.MovePatientError
import bed.functional.core.Patient
import bed.functional.core.PatientFinder
import bed.functional.core.PatientId
import bed.functional.core.PatientNotFound
import bed.functional.core.RoomId
import bed.functional.core.Ward
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus.NOT_FOUND
import io.micronaut.http.HttpStatus.NO_CONTENT
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Singleton
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Tag("integration")
@MicronautHttpTest
class AllocateBedHttpControllerTest(
    @Client("/") private val client: HttpClient,
    private val patientFinder: PatientFinder,
    private val bedRepository: BedRepository,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val allocate: (Bed, Patient, Clock, LocalDate) -> Either<AllocateError, Bed>
) {

    @Test
    fun `should allocate a bed for a patient`() {
        val bedId = UUID.randomUUID()
        val patientId = UUID.randomUUID()
        val patient = TestBuilders.buildPatient()
        val bed = TestBuilders.buildBed()
        val allocatedBed = bed.copy(events = listOf(BedAllocated(bed)))
        every { patientFinder.find(PatientId(patientId)) } returns patient.right()
        every { bedRepository.find(BedId(bedId)) } returns bed.right()
        every { allocate(eq(bed), eq(patient), any(), LocalDate.parse("2024-06-15")) } returns allocatedBed.right()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/$bedId/allocate",
            """ { "bed_id":"$bedId", "patient_id":"$patientId", "to":"2024-06-15" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NO_CONTENT
        verify { bedRepository.save(allocatedBed, FakeTransaction) }
        verify { eventPublisher.publish(allocatedBed.events, FakeTransaction) }
    }

    @Test
    fun `should fail when patient does not exists`() {
        every { patientFinder.find(any()) } returns PatientNotFound.left()

        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${UUID.randomUUID()}/allocate",
            """ { "bed_id":"${UUID.randomUUID()}", "patient_id":"${UUID.randomUUID()}", "to":"2024-06-15" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NOT_FOUND
        verify { errorReporter.report(PatientNotFound, AllocateBedHttpController::class) }
    }

    @Test
    fun `should fail when bed does not exists`() {
        every { patientFinder.find(any()) } returns TestBuilders.buildPatient().right()
        every { bedRepository.find(any()) } returns BedNotFound.left()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${UUID.randomUUID()}/allocate",
            """ { "bed_id":"${UUID.randomUUID()}", "patient_id":"${UUID.randomUUID()}", "to":"2024-06-15" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe NOT_FOUND
        verify { errorReporter.report(BedNotFound, AllocateBedHttpController::class) }
    }

    @Test
    fun `should fail when allocating the bed fails`() {
        every { patientFinder.find(any()) } returns TestBuilders.buildPatient().right()
        every { bedRepository.find(any()) } returns TestBuilders.buildBed().right()
        every { allocate(any(), any(), any(), any()) } returns BedAlreadyAllocated.left()
        val request: HttpRequest<Any> = HttpRequest.PATCH(
            "/beds/${UUID.randomUUID()}/allocate",
            """ { "bed_id":"${UUID.randomUUID()}", "patient_id":"${UUID.randomUUID()}", "to":"2024-06-15" } """
        )

        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldNotBe NO_CONTENT
        verify { errorReporter.report(BedAlreadyAllocated, AllocateBedHttpController::class) }
    }

}

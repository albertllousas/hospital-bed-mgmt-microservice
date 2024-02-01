package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import bed.functional.core.AllocateError
import bed.functional.core.Bed
import bed.functional.core.BedId
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.Patient
import bed.functional.core.PatientFinder
import bed.functional.core.PatientId
import bed.functional.core.Transactional
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.noContent
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.serde.annotation.Serdeable
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Controller("/beds/{bedId}/allocate")
class AllocateBedHttpController(
    private val bedRepository: BedRepository,
    private val patientFinder: PatientFinder,
    private val transactional: Transactional,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val clock: Clock = Clock.systemUTC(),
    private val allocate: (Bed, Patient, Clock, LocalDate?) -> Either<AllocateError, Bed> = Bed.Companion::allocate
) {

    @Patch
    fun allocate(@PathVariable bedId: UUID, @Body request: AllocateBedHttpRequest): HttpResponse<Unit> =
        transactional { tx ->
            patientFinder.find(PatientId(request.patientId))
                .zip(bedRepository.find(BedId(bedId)))
                .flatMap { (patient, bed) -> allocate(bed, patient, clock, request.to) }
                .onRight { bedRepository.save(it, tx) }
                .onRight { eventPublisher.publish(it.events, tx) }
                .onLeft { errorReporter.report(it, this::class) }
                .fold({ it.toHttpErrorResponse() }, { noContent() })
        }
}

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class AllocateBedHttpRequest(val patientId: UUID, val to: LocalDate?)

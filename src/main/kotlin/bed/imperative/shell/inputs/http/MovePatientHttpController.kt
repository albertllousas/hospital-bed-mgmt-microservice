package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import bed.functional.core.Bed
import bed.functional.core.BedId
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.MovePatientError
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
import java.util.UUID

@Controller("/beds/{bedId}/move")
class MovePatientHttpController(
    private val bedRepository: BedRepository,
    private val transactional: Transactional,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val clock: Clock = Clock.systemUTC(),
    private val movePatient: (Bed, Bed, Clock) -> Either<MovePatientError, Pair<Bed, Bed>> = Bed.Companion::movePatient
) {

    @Patch
    fun allocate(@PathVariable bedId: UUID, @Body request: MovePatientHttpRequest): HttpResponse<Unit> =
        transactional { tx ->
            bedRepository.find(BedId(bedId))
                .zip(bedRepository.find(BedId(request.toBed)))
                .flatMap { (from, to) -> movePatient(from, to, clock) }
                .onRight { (from, _) -> bedRepository.save(from, tx) }
                .onRight { (_, to) -> bedRepository.save(to, tx) }
                .onRight { (from, to) -> eventPublisher.publish(from.events + to.events, tx) }
                .onLeft { errorReporter.report(it, this::class) }
                .fold({ it.toHttpErrorResponse() }, { noContent() })
        }
}

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class MovePatientHttpRequest(val toBed: UUID)

package bed.imperative.shell.inputs.http

import arrow.core.Either
import arrow.core.flatMap
import bed.functional.core.Bed
import bed.functional.core.BedId
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.ReleaseError
import bed.functional.core.Transactional
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.noContent
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import java.util.UUID

@Controller("/beds/{bedId}/release")
class ReleaseBedHttpController(
    private val bedRepository: BedRepository,
    private val transactional: Transactional,
    private val eventPublisher: EventPublisher,
    private val errorReporter: ErrorReporter,
    private val release: (Bed) -> Either<ReleaseError, Bed> = Bed.Companion::release
) {

    @Patch
    fun release(@PathVariable bedId: UUID): HttpResponse<Unit> =
        transactional { tx ->
            bedRepository.find(BedId(bedId))
                .flatMap { release(it) }
                .onRight { bedRepository.save(it, tx) }
                .onRight { eventPublisher.publish(it.events, tx) }
                .onLeft { errorReporter.report(it, this::class) }
                .fold({ it.toHttpErrorResponse() }, { noContent() })
        }
}

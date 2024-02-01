package bed.imperative.shell.inputs.http

import bed.functional.core.AllocateError
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedAlreadyReleased
import bed.functional.core.BedNotFound
import bed.functional.core.DomainError
import bed.functional.core.ErrorReporter
import bed.functional.core.MovePatientError
import bed.functional.core.PatientNotFound
import bed.functional.core.ReleaseError
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.CONFLICT
import io.micronaut.http.HttpStatus.NOT_FOUND
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import java.net.http.HttpRequest

fun <T> DomainError.toHttpErrorResponse(): HttpResponse<T> = when (this) {
    BedNotFound -> HttpResponse.status(NOT_FOUND, "Bed not found")
    PatientNotFound -> HttpResponse.status(NOT_FOUND, "patient not found")
    BedAlreadyAllocated -> HttpResponse.status(CONFLICT, "Bed already allocated")
    BedAlreadyReleased -> HttpResponse.status(CONFLICT, "Bed already released")
    is MovePatientError -> TODO()
    is ReleaseError -> TODO()
    is AllocateError -> TODO()
}

@Controller
class GlobalHttpErrorHandler(private val errorReporter: ErrorReporter) {

    @Error(global = true) // exception = SpecificException::class)
    fun handleException(request: HttpRequest, exception: Throwable): HttpResponse<*> =
        HttpResponse.serverError<String>().also { errorReporter.report(exception) }
}
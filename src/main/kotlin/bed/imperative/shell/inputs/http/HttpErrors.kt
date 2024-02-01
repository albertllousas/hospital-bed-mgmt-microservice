package bed.imperative.shell.inputs.http

import bed.functional.core.*
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
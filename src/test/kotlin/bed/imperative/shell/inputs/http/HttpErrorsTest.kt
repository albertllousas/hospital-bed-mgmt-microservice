package bed.imperative.shell.inputs.http

import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedAlreadyReleased
import bed.functional.core.BedNotFound
import bed.functional.core.DomainError
import bed.functional.core.PatientNotFound
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.CONFLICT
import io.micronaut.http.HttpStatus.NOT_FOUND
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class HttpErrorsTest {

    @TestFactory
    fun `map errors to http status`() = listOf<Pair<DomainError, HttpResponse<Any>>>(
        Pair(BedNotFound, HttpResponse.status(NOT_FOUND, "Bed not found")),
        Pair(PatientNotFound, HttpResponse.status(NOT_FOUND, "patient not found")),
        Pair(BedAlreadyAllocated, HttpResponse.status(CONFLICT, "Bed already allocated")),
        Pair(BedAlreadyReleased, HttpResponse.status(CONFLICT, "Bed already released"))
    ).map { (error, expected) ->
        dynamicTest("should map $error to $expected") {
            val httpError = error.toHttpErrorResponse<HttpResponse<Any>>()
            httpError.status shouldBe expected.status
            httpError.reason() shouldBe expected.reason()
        }
    }
}

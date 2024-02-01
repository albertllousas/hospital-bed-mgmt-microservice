package bed.imperative.shell.outputs.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.functional.core.Patient
import bed.functional.core.PatientFinder
import bed.functional.core.PatientId
import bed.functional.core.PatientNotFound
import bed.functional.core.PatientStatus
import bed.imperative.shell.outputs.http.PatientHttpStatus.ADMITTED
import bed.imperative.shell.outputs.http.PatientHttpStatus.DISCHARGED
import bed.imperative.shell.outputs.http.PatientHttpStatus.OTHER
import bed.imperative.shell.outputs.http.PatientHttpStatus.READY_FOR_DISCHARGE
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.netty.FullNettyClientHttpResponse
import io.micronaut.serde.annotation.Serdeable
import jakarta.inject.Singleton
import java.net.URL
import java.util.UUID

@Singleton
class ExternalHttpPatientFinder(@Value("\${clients.patients.base-url}") baseUrl: String) : PatientFinder {

    private val httpClient: HttpClient = HttpClient.create(URL(baseUrl))

    override fun find(id: PatientId): Either<PatientNotFound, Patient> = try {
        HttpRequest.GET<PatientHttpDto>("/api/patients/${id.value}")
            .let { httpClient.toBlocking().retrieve(it, PatientHttpDto::class.java) }
            .let { it.toDomain().right() }
    } catch (e: HttpClientResponseException) {
        mapToDomainErrorOrThrow(e).left()
    }

    private fun mapToDomainErrorOrThrow(e: HttpClientResponseException): PatientNotFound =
        when {
            e.response.status.code == 404 -> PatientNotFound
            e.response is FullNettyClientHttpResponse<*> -> throw HttpCallNonSucceededException(
                httpClient = this@ExternalHttpPatientFinder::class.simpleName!!,
                errorBody = e.response.getBody(String::class.java).orElse(null),
                httpStatus = e.response.status.code
            )

            else -> throw e
        }

    private fun PatientHttpDto.toDomain() = Patient(
        id = PatientId(id),
        status = when (status) {
            ADMITTED -> PatientStatus.ADMITTED
            READY_FOR_DISCHARGE -> PatientStatus.READY_FOR_DISCHARGE
            DISCHARGED -> PatientStatus.DISCHARGED
            OTHER -> PatientStatus.OTHER
        }
    )
}

@Introspected
@Serdeable
data class PatientHttpDto(val id: UUID, val firstName: String, val lastName: String, val status: PatientHttpStatus)

enum class PatientHttpStatus {
    ADMITTED, READY_FOR_DISCHARGE, DISCHARGED, OTHER
}

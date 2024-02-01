package bed.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import java.util.UUID


fun WireMockServer.stubHttpEnpointForFindPatientNonSucceeded(
    patientId: UUID = DEFAULT_PATIENT_ID,
    responseCode: Int = 400,
    responseErrorBody: String = """{"status":400,"detail":"Some problem"}"""
) =
    this.stubFor(
        get(urlEqualTo("/api/patients/$patientId"))
            .willReturn(status(responseCode).withBody(responseErrorBody))
    )

fun WireMockServer.stubHttpEnpointForFindPatientNotFound(patientId: UUID = DEFAULT_PATIENT_ID) =
    this.stubHttpEnpointForFindPatientNonSucceeded(
        patientId, 404, """ {"status":404,"detail":"Patient not found: $patientId"} """
    )

fun WireMockServer.stubHttpEnpointForFindPatientSucceeded(patientId: UUID = DEFAULT_PATIENT_ID) =
    this.stubFor(
        get(urlEqualTo("/api/patients/$patientId"))
            .willReturn(
                status(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                              "id": "$patientId",
                              "firstName": "Jane",
                              "lastName": "Doe",
                              "status": "ADMITTED"
                            }
                        """
                    )
            )
    )

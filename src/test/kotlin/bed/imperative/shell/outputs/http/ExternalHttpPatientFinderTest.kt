package bed.imperative.shell.outputs.http

import arrow.core.left
import arrow.core.right
import bed.fixtures.stubHttpEnpointForFindPatientNonSucceeded
import bed.fixtures.stubHttpEnpointForFindPatientNotFound
import bed.fixtures.stubHttpEnpointForFindPatientSucceeded
import bed.functional.core.Patient
import bed.functional.core.PatientId
import bed.functional.core.PatientNotFound
import bed.functional.core.PatientStatus.ADMITTED
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

@Tag("integration")
class ExternalHttpPatientFinderTest {

    private val externalPatientService = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    private val patientFinder = ExternalHttpPatientFinder(externalPatientService.baseUrl())

    @Test
    fun `find a patient`() {
        val patientId = UUID.randomUUID()
        externalPatientService.stubHttpEnpointForFindPatientSucceeded(patientId = patientId)

        val result = patientFinder.find(PatientId(patientId))

        result shouldBe Patient(id = PatientId(patientId), status = ADMITTED).right()
    }

    @Test
    fun `fail when patient does not exists`() {
        val patientId = UUID.randomUUID()
        externalPatientService.stubHttpEnpointForFindPatientNotFound(patientId)

        val result = patientFinder.find(PatientId(patientId))

        result shouldBe PatientNotFound.left()
    }

    @Test
    fun `crash when there is a non successful http response`() {
        val patientId = UUID.randomUUID()
        externalPatientService.stubHttpEnpointForFindPatientNonSucceeded(patientId = patientId)
        val exception = shouldThrowExactly<HttpCallNonSucceededException> {
            patientFinder.find(PatientId(patientId))
        }
        exception.message shouldBe """Http call with 'ExternalHttpPatientFinder' failed with status '400' and body '{"status":400,"detail":"Some problem"}' """
    }
}

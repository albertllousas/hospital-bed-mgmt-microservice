package bed.functional.core

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PatientTest {

    @Test
    fun `should check if a patient is admitted`() {
        val patient = Patient(PatientId(UUID.randomUUID()), PatientStatus.ADMITTED)

        assertTrue(patient.isAdmitted())
    }
}

package bed.functional.core

import java.util.UUID

data class PatientId(val value: UUID)

data class Patient (val id: PatientId, val status: PatientStatus) {
    fun isAdmitted(): Boolean = status == PatientStatus.ADMITTED
}

enum class PatientStatus {
    ADMITTED, READY_FOR_DISCHARGE, DISCHARGED, OTHER
}

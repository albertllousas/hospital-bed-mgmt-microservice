package bed.functional.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.functional.core.BedStatus.*
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDate.*
import java.util.UUID


enum class BedFeature {
    ADJUSTABLE,
    CARDIAC_MONITOR,
    BARIATRIC,
    PRESSURE_RELIEF,
    ELECTRIC,
    MANUAL,
    WITH_RAILS,
    WITHOUT_RAILS,
    ADVANCED_MONITORING, // ICU-specific: Advanced patient monitoring systems
    LIFE_SUPPORT_COMPATIBLE, // ICU-specific: Compatibility with life support systems
    EMERGENCY_FEATURES // ICU-specific: Features for quick emergency response
    // Add more ICU-specific or general features as necessary
}

enum class Ward {
    CARDIOLOGY,
    ONCOLOGY,
    PEDIATRICS,
    MATERNITY,
    ICU
    // other wards
}

data class BedId(val value: UUID)

data class RoomId(val value: String)

sealed class BedStatus {
    object Free : BedStatus()
    data class Occupied(val by: PatientId, val from: LocalDate, val to: LocalDate? = null) : BedStatus()
}

data class Bed(
    val id: BedId,
    val roomId: RoomId,
    val ward: Ward,
    val status: BedStatus,
    val features: List<BedFeature>,
    @Transient val version: Long,
    @Transient val events: List<DomainEvent>
) {
    companion object {

        fun allocate(bed: Bed, patient: Patient, clock: Clock, to: LocalDate?): Either<AllocateError, Bed> =
            when {
                to != null && to.isBefore(now(clock)) -> InvalidDate.left()
                !patient.isAdmitted() -> PatientNotAdmitted.left()
                bed.status is Occupied -> BedAlreadyAllocated.left()
                else -> bed
                    .copy(status = Occupied(patient.id, now(clock), to)).let { it.addEvent(BedAllocated(it)) }.right()
            }

        fun release(bed: Bed): Either<ReleaseError, Bed> = when (bed.status) {
            is Free -> BedAlreadyReleased.left()
            is Occupied -> bed.copy(status = Free).let { it.addEvent(BedReleased(it)) }.right()
        }

        fun create(id: BedId, roomId: RoomId, ward: Ward, features: List<BedFeature>): Bed = Bed(
            id = id,
            roomId = roomId,
            ward = ward,
            status = Free,
            features = features,
            events = emptyList(),
            version = 0
        ).let { it.addEvent(BedCreated(it)) }

        fun movePatient(from: Bed, to: Bed, clock: Clock): Either<MovePatientError, Pair<Bed, Bed>> = when {
            from.status is Free -> BedAlreadyReleased.left()
            to.status is Occupied -> BedAlreadyAllocated.left()
            else -> {
                val fromBed = from.copy(status = Free)
                    .let { it.addEvent(BedReleased(it)) }
                val toBed = to.copy(status = Occupied((from.status as Occupied).by, now(clock), from.status.to))
                    .let { it.addEvent(BedAllocated(it)) }
                Pair(fromBed, toBed).right()
            }
        }
    }

    private fun addEvent(event: DomainEvent): Bed = copy(events = events + event)
}

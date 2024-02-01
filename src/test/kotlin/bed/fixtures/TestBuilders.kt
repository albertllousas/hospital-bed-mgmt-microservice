package bed.fixtures

import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.BedStatus
import bed.functional.core.BedStatus.Free
import bed.functional.core.Patient
import bed.functional.core.PatientId
import bed.functional.core.PatientStatus
import bed.functional.core.RoomId
import bed.functional.core.Ward
import com.github.javafaker.Faker
import java.util.UUID

val faker = Faker()

val DEFAULT_PATIENT_ID = UUID.randomUUID()

object TestBuilders {

    fun buildPatient(
        id: PatientId = PatientId(UUID.randomUUID()),
        status: PatientStatus = faker.options().option(PatientStatus::class.java)
    ) = Patient(id = id, status = status)

    fun buildBed(
        id: BedId = BedId(UUID.randomUUID()),
        roomId: RoomId = RoomId(faker.idNumber().valid()),
        ward: Ward = faker.options().option(Ward::class.java),
        status: BedStatus = Free,
        features: List<BedFeature> = List(faker.random().nextInt(1, 5)) {
            faker.options().option(BedFeature::class.java)
        },
        version: Long = 0
    ): Bed = Bed(
        id = id,
        roomId = roomId,
        status = status,
        features = features,
        ward = ward,
        version = version,
        events = emptyList()
    )
}

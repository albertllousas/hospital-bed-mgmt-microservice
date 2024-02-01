package bed.functional.core

import arrow.core.left
import arrow.core.right
import bed.functional.core.BedStatus.Free
import bed.functional.core.BedStatus.Occupied
import bed.functional.core.PatientStatus.ADMITTED
import bed.functional.core.PatientStatus.OTHER
import bed.fixtures.TestBuilders
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate.parse
import java.time.ZoneId
import java.util.UUID

class BedTest {

    private val clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

    @Nested
    inner class AllocatePatient {

        @Test
        fun `should allocate a bed for a patient`() {
            val bed = TestBuilders.buildBed(status = Free)
            val patient = TestBuilders.buildPatient(status = ADMITTED)

            val result = Bed.allocate(bed, patient, clock, parse("2024-01-15"))

            val expectedBed = bed.copy(status = Occupied(patient.id, parse("2024-01-01"), parse("2024-01-15")))
            result shouldBe expectedBed.copy(events = listOf(BedAllocated(expectedBed))).right()
        }

        @Test
        fun `should not allocate a bed for a patient if patient is not admitted`() {
            val bed = TestBuilders.buildBed(status = Free)
            val patient = TestBuilders.buildPatient(status = OTHER)

            val allocatedBed = Bed.allocate(bed, patient, clock, parse("2024-01-15"))

            allocatedBed shouldBe PatientNotAdmitted.left()
        }

        @Test
        fun `should not allocate a bed for a patient if bed is already occupied`() {
            val bed = TestBuilders.buildBed(
                status = Occupied(
                    PatientId(UUID.randomUUID()),
                    parse("2024-01-01"),
                    parse("2024-01-15")
                )
            )
            val patient = TestBuilders.buildPatient(status = ADMITTED)

            val allocatedBed = Bed.allocate(bed, patient, clock, parse("2024-01-15"))

            allocatedBed shouldBe BedAlreadyAllocated.left()
        }
    }

    @Test
    fun `should not allocate if 'to' date is in the past`() {
        val bed = TestBuilders.buildBed(status = Free)
        val patient = TestBuilders.buildPatient(status = ADMITTED)

        val result = Bed.allocate(bed, patient, clock, parse("2023-01-15"))

        result shouldBe InvalidDate.left()
    }

    @Nested
    inner class ReleasePatient {

        @Test
        fun `should release a bed`() {
            val bed = TestBuilders.buildBed(status = Occupied(PatientId(UUID.randomUUID()), parse("2024-01-01"), parse("2024-01-15")))

            val releasedBed = Bed.release(bed)

            val expectedBed = bed.copy(status = Free)
            releasedBed shouldBe expectedBed.copy(events = listOf(BedReleased(expectedBed))).right()
        }

        @Test
        fun `should not release a bed if it is already free`() {
            val bed = TestBuilders.buildBed(status = Free)

            val releasedBed = Bed.release(bed)

            releasedBed shouldBe BedAlreadyReleased.left()
        }
    }

    @Nested
    inner class MovePatient {

        @Test
        fun `should move a patient from a bed to another`() {
            val occupied = Occupied(PatientId(UUID.randomUUID()), parse("2023-01-15"), parse("2024-01-15"))
            val fromBed = TestBuilders.buildBed(status = occupied)
            val toBed = TestBuilders.buildBed(status = Free)

            val movedBeds = Bed.movePatient(fromBed, toBed, clock)

            val expectedFromBed = fromBed.copy(status = Free)
            val expectedToBed = toBed.copy(status = occupied.copy(from = parse("2024-01-01")))
            movedBeds shouldBe Pair(
                expectedFromBed.copy(events = listOf(BedReleased(expectedFromBed))),
                expectedToBed.copy(events = listOf(BedAllocated(expectedToBed)))
            ).right()
        }

        @Test
        fun `should not move a patient from a bed to another if from bed is free`() {
            val fromBed = TestBuilders.buildBed(status = Free)
            val toBed = TestBuilders.buildBed(status = Free)

            val movedBeds = Bed.movePatient(fromBed, toBed, clock)

            movedBeds shouldBe BedAlreadyReleased.left()
        }

        @Test
        fun `should not move a patient from a bed to another if to bed is occupied`() {
            val fromBed = TestBuilders.buildBed(
                status = Occupied(PatientId(UUID.randomUUID()), parse("2024-01-01"), parse("2024-01-15"))
            )
            val toBed = TestBuilders.buildBed(
                status = Occupied(PatientId(UUID.randomUUID()), parse("2024-01-01"), parse("2024-01-15"))
            )

            val movedBeds = Bed.movePatient(fromBed, toBed, clock)

            movedBeds shouldBe BedAlreadyAllocated.left()
        }
    }
}

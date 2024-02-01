package bed.imperative.shell.outputs.db

import arrow.core.left
import arrow.core.right
import bed.functional.core.BedFeature.ADJUSTABLE
import bed.functional.core.BedFeature.BARIATRIC
import bed.functional.core.BedFeature.CARDIAC_MONITOR
import bed.functional.core.BedFeature.ELECTRIC
import bed.functional.core.BedFeature.MANUAL
import bed.functional.core.BedFeature.WITHOUT_RAILS
import bed.functional.core.BedId
import bed.functional.core.BedNotFound
import bed.functional.core.BedStatus.Free
import bed.functional.core.BedStatus.Occupied
import bed.functional.core.PatientId
import bed.functional.core.Ward.ICU
import bed.functional.core.Ward.MATERNITY
import bed.functional.core.Ward.ONCOLOGY
import bed.fixtures.TestBuilders
import bed.fixtures.containers.Postgres
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.MapMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate.now
import java.time.ZoneId
import java.util.UUID

@Tag("integration")
class PostgresqlBedRepositoryTest {

    private val db = Postgres()

    private val jdbi = Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password)

    private val handle = jdbi.open()

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.950Z"), ZoneId.of("UTC"))

    private val bedRepository = PostgresqlBedRepository(jdbi, clock)

    @AfterEach
    fun `tear down`() {
        db.container.stop()
    }

    @Test
    fun `should create a bed`() {
        val bed = TestBuilders.buildBed()

        bedRepository.save(bed, JdbiTransaction(handle))

        handle.createQuery("SELECT * FROM hospital_beds")
            .map(MapMapper())
            .one()
            .also {
                assertThat(it["id"]).isEqualTo(bed.id.value)
            }
    }

    @Test
    fun `should fail updating an existent bed when there is a concurrency issue`() {
        val bed = TestBuilders.buildBed().also { bedRepository.save(it, JdbiTransaction(handle)) }
        handle.execute(
            "UPDATE hospital_beds SET version = 10 WHERE id= '${bed.id.value}'"
        )

        shouldThrow<OptimisticLockException> { bedRepository.save(bed.copy(version = 12), JdbiTransaction(handle)) }
    }

    @Test
    fun `should find a bed`() {
        val bed = TestBuilders.buildBed().also { bedRepository.save(it, JdbiTransaction(handle)) }

        val result = bedRepository.find(bed.id)

        assertThat(result).isEqualTo(bed.right())
    }

    @Test
    fun `should not find a bed`() {
        assertThat(bedRepository.find(BedId(UUID.randomUUID()))).isEqualTo(BedNotFound.left())
    }

    @Test
    fun `should find available beds`() {
        val tx = JdbiTransaction(handle)
        val firstBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val secondBed =  TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val thirdBed = TestBuilders.buildBed(status = Occupied(PatientId(UUID.randomUUID()), now())).also { bedRepository.save(it, tx) }
        val fourthBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val fifthBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }

        val result = bedRepository.findAvailable()

        result shouldBe listOf(firstBed, secondBed, fourthBed, fifthBed)
    }

    @Test
    fun `should find available beds filtering by ward`() {
        val tx = JdbiTransaction(handle)
        val firstBed = TestBuilders.buildBed(status = Free, ward = ICU).also { bedRepository.save(it, tx) }
        val secondBed =  TestBuilders.buildBed(status = Free, ward = ONCOLOGY).also { bedRepository.save(it, tx) }
        val thirdBed = TestBuilders.buildBed( ward = ICU, status = Occupied(PatientId(UUID.randomUUID()), now())).also { bedRepository.save(it, tx) }
        val fourthBed = TestBuilders.buildBed(status = Free, ward = ICU).also { bedRepository.save(it, tx) }
        val fifthBed = TestBuilders.buildBed(status = Free, ward = MATERNITY).also { bedRepository.save(it, tx) }

        val result = bedRepository.findAvailable(ward = ICU)

        result shouldBe listOf(firstBed, fourthBed)
    }

    @Test
    fun `should find available beds filtering by feature`() {
        val tx = JdbiTransaction(handle)
        val firstBed = TestBuilders.buildBed(status = Free, features = listOf(ELECTRIC, CARDIAC_MONITOR))
            .also { bedRepository.save(it, tx) }
        val secondBed =  TestBuilders.buildBed(status = Free, features = listOf(MANUAL, CARDIAC_MONITOR))
            .also { bedRepository.save(it, tx) }
        val thirdBed = TestBuilders.buildBed(features = listOf(WITHOUT_RAILS, BARIATRIC), status = Free)
            .also { bedRepository.save(it, tx) }
        val fourthBed = TestBuilders.buildBed(status = Free, features = listOf(ADJUSTABLE, CARDIAC_MONITOR))
            .also { bedRepository.save(it, tx) }
        val fifthBed = TestBuilders.buildBed(status = Free, features = listOf(ELECTRIC))
            .also { bedRepository.save(it, tx) }

        val result = bedRepository.findAvailable(features = listOf(BARIATRIC, ELECTRIC))

        result shouldBe listOf(firstBed, thirdBed, fifthBed)
    }

    @Test
    fun `should find available beds and paginate the result`() {
        val tx = JdbiTransaction(handle)
        val firstBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val secondBed =  TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val thirdBed = TestBuilders.buildBed(status = Occupied(PatientId(UUID.randomUUID()), now())).also { bedRepository.save(it, tx) }
        val fourthBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }
        val fifthBed = TestBuilders.buildBed(status = Free).also { bedRepository.save(it, tx) }

        val result = bedRepository.findAvailable(limit = 2, offset = 1)

        result shouldBe listOf(secondBed, fourthBed)
    }
}

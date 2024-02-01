package bed.imperative.shell.outputs.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.BedNotFound
import bed.functional.core.BedRepository
import bed.functional.core.BedStatus.Free
import bed.functional.core.BedStatus.Occupied
import bed.functional.core.PatientId
import bed.functional.core.RoomId
import bed.functional.core.Transaction
import bed.functional.core.Ward
import jakarta.inject.Singleton
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Singleton
class PostgresqlBedRepository(private val jdbi: Jdbi, private val clock: Clock) : BedRepository {

    override fun save(bed: Bed, transaction: Transaction) {
        if (transaction !is JdbiTransaction)
            throw IllegalArgumentException("Transaction must be of type JdbiTransaction")
        else {
            val status = when (bed.status) {
                is Occupied -> "OCCUPIED"
                is Free -> "FREE"
            }
            transaction.handle.createQuery(
                """INSERT INTO hospital_beds (
                        id,
                        room_id,
                        ward,
                        features,
                        status,
                        occupied_patient_id,
                        occupied_from,
                        occupied_to,
                        created,
                        version
                    ) VALUES (:id,:room,:ward,:features,:status,:patient,:from,:to,:created,0)  
                ON CONFLICT (id) DO UPDATE SET 
                        room_id = :room,
                        ward = :ward,
                        features = :features,
                        status = :status,
                        occupied_patient_id = :patient,
                        occupied_from = :from,
                        occupied_to = :to,
                        created = :created,
                        version = hospital_beds.version + 1 
                RETURNING version
            """
            )
                .bind("id", bed.id.value)
                .bind("room", bed.roomId.value)
                .bind("ward", bed.ward.name)
                .bind("features", bed.features.map { it.name }.toTypedArray())
                .bind("status", status)
                .bind("patient", (if (bed.status is Occupied) bed.status.by.value else null))
                .bind("from", if (bed.status is Occupied) bed.status.from else null)
                .bind("to", if (bed.status is Occupied) bed.status.to else null)
                .bind("created", LocalDateTime.now(clock))
                .mapTo(Long::class.java)
                .one()
                .also { version ->
                    if (version > 0 && version != bed.version + 1) throw OptimisticLockException(bed.id.value)
                }
        }
    }

    override fun find(bedId: BedId): Either<BedNotFound, Bed> =
        jdbi.open().use { handle ->
            handle.createQuery("""SELECT * FROM hospital_beds WHERE id = :id""")
                .bind("id", bedId.value)
                .map { rs, _ -> rs.asBed() }
                .findOne()
                .orElse(null)
                ?.right()
                ?: BedNotFound.left()
        }

    override fun findAvailable(ward: Ward?, features: List<BedFeature>, limit: Int, offset: Int): List<Bed> {
        val params = mutableMapOf<String, Any>().apply { putAll(mapOf("limit" to limit, "offset" to offset)) }
        val extraClauses = mutableListOf<String>()
        if (ward != null) {
            params["ward"] = ward.name
            extraClauses.add("AND ward = :ward")
        }
        if (features.isNotEmpty()) {
            params["features"] = features.map { it.name }.toTypedArray()
            extraClauses.add(" AND features && :features::text[]")
        }
        return jdbi.open().use { handle ->
            handle.createQuery(
                """
                   SELECT * FROM hospital_beds 
                   WHERE status = 'FREE' ${extraClauses.joinToString(" ")} 
                   LIMIT :limit OFFSET :offset
                   """
            )
                .bindMap(params)
                .map { rs, _ -> rs.asBed() }
                .list()
        }
    }

    private fun ResultSet.asBed(): Bed =
        Bed(
            id = BedId(UUID.fromString(this.getString("id"))),
            roomId = RoomId(this.getString("room_id")),
            ward = Ward.valueOf(this.getString("ward")),
            status = when (this.getString("status")) {
                "FREE" -> Free
                "OCCUPIED" -> Occupied(
                    PatientId(UUID.fromString(this.getString("occupied_patient_id"))),
                    this.getDate("occupied_from").toLocalDate(),
                    this.getDate("occupied_to")?.toLocalDate()
                )

                else -> throw IllegalArgumentException("Unknown bed status")
            },
            features = (this.getArray("features").array as Array<String>).map { BedFeature.valueOf(it) },
            version = this.getLong("version"),
            events = emptyList()
        )
}

data class OptimisticLockException(val id: UUID) : Exception("Optimistic lock exception for bed $id")
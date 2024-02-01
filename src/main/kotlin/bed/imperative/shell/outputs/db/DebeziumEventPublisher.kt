package bed.imperative.shell.outputs.db

import bed.functional.core.BedStatus.Free
import bed.functional.core.BedStatus.Occupied
import bed.functional.core.DomainEvent
import bed.functional.core.Transaction
import bed.imperative.shell.outputs.db.ExternalHospitalBedEvent.HospitalBedCreatedEvent
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DebeziumEventPublisher(
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
    private val clock: Clock = Clock.systemUTC(),
    private val generateId: () -> UUID = { UUID.randomUUID() }
) {

    fun publish(event: DomainEvent, transaction: Transaction) {
        val externalHospitalBedEvent = event.toExternalHospitalBedEvent()
        if(transaction is JdbiTransaction) {
            transaction.handle.execute(
                """ INSERT INTO outbox (
                            id,
                            aggregateid,
                            aggregatetype,
                            aggregate_version,
                            event_type,
                            payload,
                            occurred_on
                        ) VALUES (?,?,?,?,?,?,?) """,
                externalHospitalBedEvent.eventId,
                externalHospitalBedEvent.hospitalBed.id,
                event.bed::class.simpleName,
                event.bed.version,
                event::class.simpleName,
                objectMapper.writeValueAsBytes(externalHospitalBedEvent),
                externalHospitalBedEvent.occurredOn
            )
            transaction.handle.execute("DELETE FROM outbox WHERE id = ?", externalHospitalBedEvent.eventId)
        } else {
            throw IllegalArgumentException("Unrecognized transaction type: ${transaction::class.simpleName}")
        }

    }

private fun DomainEvent.toExternalHospitalBedEvent(): ExternalHospitalBedEvent =
    HospitalBedCreatedEvent(
        hospitalBed = ExternalHospitalBedDto(
            id = bed.id.value,
            roomId = bed.roomId.value,
            ward = bed.ward.name,
            status = when(bed.status) {
                is Occupied -> "OCCUPIED"
                Free -> "FREE"
            },
            patientId = if(bed.status is Occupied) (bed.status as Occupied).by.value else null,
            occupiedFrom = if(bed.status is Occupied) (bed.status as Occupied).from else null,
            occupiedTo = if(bed.status is Occupied) (bed.status as Occupied).to else null,
            features = bed.features.map { it.name },
        ),
        occurredOn = LocalDateTime.now(clock),
        eventId = generateId()
    )
}

/*
External event: Event to share changes to other bounded contexts.
*/
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "event_type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HospitalBedCreatedEvent::class, name = "hospital_bed_created_event"),
)
sealed class ExternalHospitalBedEvent(
    @get:JsonProperty("event_type") val eventType: String
) {
    @get:JsonProperty("occurred_on")
    abstract val occurredOn: LocalDateTime

    @get:JsonProperty("event_id")
    abstract val eventId: UUID

    @get:JsonProperty("hospital_bed")
    abstract val hospitalBed: ExternalHospitalBedDto

    data class HospitalBedCreatedEvent(
        override val hospitalBed: ExternalHospitalBedDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
    ) : ExternalHospitalBedEvent("hospital_bed_created_event")
}

data class ExternalHospitalBedDto(
    val id: UUID,
    @get:JsonProperty("room_id")
    val roomId: String,
    val ward: String,
    val status: String,
    @get:JsonProperty("patient_id")
    val patientId: UUID?,
    @get:JsonProperty("occupied_from")
    val occupiedFrom: LocalDate?,
    @get:JsonProperty("occupied_to")
    val occupiedTo: LocalDate?,
    val features: List<String>,
)

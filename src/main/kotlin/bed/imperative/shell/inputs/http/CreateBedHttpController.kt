package bed.imperative.shell.inputs.http

import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.BedRepository
import bed.functional.core.EventPublisher
import bed.functional.core.RoomId
import bed.functional.core.Transactional
import bed.functional.core.Ward
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.created
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.serde.annotation.Serdeable
import java.util.UUID

@Controller("/beds")
class CreateBedHttpController(
    private val bedRepository: BedRepository,
    private val transactional: Transactional,
    private val eventPublisher: EventPublisher,
    private val generateId: () -> UUID,
    private val create: (BedId, RoomId, Ward, List<BedFeature>) -> Bed = Bed.Companion::create
) {

    @Post
    fun allocate(@Body request: CreateBedHttpRequest): HttpResponse<CreateBedResponse> =
        transactional { tx ->
            create(BedId(generateId()), RoomId(request.roomId), request.ward, request.features)
                .also { bedRepository.save(it, tx) }
                .also { eventPublisher.publish(it.events, tx) }
                .let { created(CreateBedResponse(it.id.value)) }
        }
}

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class CreateBedHttpRequest(val roomId: String, val ward: Ward, val features: List<BedFeature>)

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class CreateBedResponse(val bedId: UUID)

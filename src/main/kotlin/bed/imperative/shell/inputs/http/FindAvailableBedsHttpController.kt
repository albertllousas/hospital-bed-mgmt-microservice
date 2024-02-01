package bed.imperative.shell.inputs.http

import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedRepository
import bed.functional.core.Ward
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.ok
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.serde.annotation.Serdeable
import java.util.UUID

@Controller("/beds/available")
class FindAvailableBedsHttpController(private val bedRepository: BedRepository) {

    @Get
    fun findAvailable(
        ward: Ward?,
        features: List<BedFeature>,
        limit: Int?,
        offset: Int?
    ): HttpResponse<FindAvailableBedsResponse> =
        bedRepository.findAvailable(ward, features, limit ?: 50, offset ?: 0)
            .let { ok(it.asHttpResponse()) }
}

private fun List<Bed>.asHttpResponse(): FindAvailableBedsResponse =
    FindAvailableBedsResponse(
        data = this.map {
            AvailableBedHttpDto(id = it.id.value, roomId = it.roomId.value, features = it.features)
        }
    )

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class FindAvailableBedsResponse(val data: List<AvailableBedHttpDto>)

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class AvailableBedHttpDto(val id: UUID, val roomId: String, val features: List<BedFeature>)

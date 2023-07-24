package bed.mgmt.infra.`in`.http

import arrow.core.raise.recover
import bed.mgmt.application.service.CreateBedRequest
import bed.mgmt.application.service.CreateBedService
import bed.mgmt.application.service.HospitalBedFeatureRequest
import bed.mgmt.domain.model.BedAlreadyExists
import bed.mgmt.domain.model.BedType
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.*
import io.micronaut.http.HttpStatus.*
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.serde.annotation.Serdeable
import java.util.UUID

@Controller("/hospital-beds")
class HospitalBedsController(val createBed: CreateBedService) {

    @Post
    fun create(@Body request: CreateBedHttpDto): HttpResponse<BedCreatedHttpDto> = recover(
        block = {
            createBed(request.asUseCaseRequest()).let { created(BedCreatedHttpDto(it.bed.id.value)) }
        },
        recover = { status(CONFLICT) },
        catch = { serverError() }
    )

    private fun CreateBedHttpDto.asUseCaseRequest() = with(this) {
        CreateBedRequest(roomId, hospitalId, type, features, extraFeatures, manufacturerBedId, manufacturer, details)
    }
    // execute usecase?

//    bed create/update etc

//    bed allocations
//    create assign
//    bed assignment key
    //  future more than 6 months nope
    // past nope

    // bed assignment ->
    // bed agg + table, bedassignment (from to patient) agg + table, infra-bed-day-morning-table from to consistency by keeping consistency table with -- bed-day (unique)
}

@Serdeable
@JsonNaming(SnakeCaseStrategy::class)
data class CreateBedHttpDto(
    val roomId: UUID,
    val hospitalId: UUID,
    val type: BedType,
    val features: List<HospitalBedFeatureRequest>,
    val extraFeatures: List<String>,
    val manufacturerBedId: String,
    val manufacturer: String,
    val details: String?
)

@Serdeable
data class BedCreatedHttpDto(val id: UUID)
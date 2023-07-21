package bed.mgmt.application.service

import arrow.core.raise.Raise
import bed.mgmt.domain.model.BedAlreadyExists
import bed.mgmt.domain.model.BedCreated
import bed.mgmt.domain.model.BedType
import bed.mgmt.domain.model.HospitalDoesNotExists
import bed.mgmt.domain.model.RoomDoesNotExists
import io.micronaut.core.annotation.Introspected
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class CreateBedService {

    context(Raise<BedAlreadyExists>, Raise<HospitalDoesNotExists>, Raise<RoomDoesNotExists>)
    operator fun invoke(request: CreateBedRequest): BedCreated = TODO()
}

data class CreateBedRequest(
    val roomId: UUID,
    val hospitalId: UUID,
    val type: BedType,
    val features: List<HospitalBedFeatureRequest>,
    val extraFeatures: List<String>,
    val manufacturerId: UUID,
    val manufacturer: String,
    val details: String?
)

enum class HospitalBedFeatureRequest {
    WHEELS, ADJUSTABLE_HEIGHT, REMOVABLE_HEAD_AND_FOOTBOARDS, ELECTRIC, SEMI_ELECTRIC, SIDE_RAILS, SAFETY_BELDS, CPR
}

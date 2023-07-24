package bed.mgmt.application.service

import arrow.core.raise.Raise
import bed.mgmt.domain.model.BedAlreadyExists
import bed.mgmt.domain.model.BedCreated
import bed.mgmt.domain.model.BedType
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class CreateBedService() {

    context(Raise<BedAlreadyExists>)
    operator fun invoke(request: CreateBedRequest): BedCreated = TODO()
}

data class CreateBedRequest(
    val roomId: UUID,
    val hospitalId: UUID,
    val type: BedType,
    val features: List<HospitalBedFeatureRequest>,
    val extraFeatures: List<String>,
    val manufacturerBedId: String,
    val manufacturer: String,
    val details: String?
)

enum class HospitalBedFeatureRequest {
    WHEELS, ADJUSTABLE_HEIGHT, REMOVABLE_HEAD_AND_FOOTBOARDS, ELECTRIC, SEMI_ELECTRIC, SIDE_RAILS, SAFETY_BELDS, CPR
}

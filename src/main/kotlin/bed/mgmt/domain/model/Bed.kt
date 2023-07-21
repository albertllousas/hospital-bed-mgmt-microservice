package bed.mgmt.domain.model

import java.util.UUID

@JvmInline
value class HospitalBedId(val value: UUID)

data class HospitalBed(
    val id: HospitalBedId,
    val location: BedLocation,
    val type: BedType,
    val characteristics: List<HospitalBedFeature>,
    val manufacturerId: ManufacturerId,
    val manufacturer: String,
    val details: String?,
)

@JvmInline
value class ManufacturerId(val value: String)

@JvmInline
value class HospitalId(val value: UUID)

@JvmInline
value class RoomId(val value: UUID)

data class BedLocation(val hospitalId: HospitalId, val roomId: RoomId)

enum class BedType {
    STANDARD, ICU, PEDIATRIC, MATERNITY, BARIATRIC, CUSTOM
}

sealed class HospitalBedFeature {
    object Wheels : HospitalBedFeature()
    object AdjustableHeight : HospitalBedFeature()
    object RemovableHeadAndFootboards : HospitalBedFeature()
    object Electric : HospitalBedFeature()
    object SemiElectric : HospitalBedFeature()
    object SideRails : HospitalBedFeature()
    object SafetyBelds : HospitalBedFeature()
    object CPR : HospitalBedFeature()
    data class Other(val value: String) : HospitalBedFeature()
}

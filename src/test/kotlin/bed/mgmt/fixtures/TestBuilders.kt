package bed.mgmt.fixtures

import bed.mgmt.application.service.CreateBedRequest
import bed.mgmt.application.service.HospitalBedFeatureRequest
import bed.mgmt.domain.model.BedLocation
import bed.mgmt.domain.model.BedType
import bed.mgmt.domain.model.HospitalBed
import bed.mgmt.domain.model.HospitalBedFeature
import bed.mgmt.domain.model.HospitalBedId
import bed.mgmt.domain.model.HospitalId
import bed.mgmt.domain.model.ManufacturerId
import bed.mgmt.domain.model.RoomId
import com.github.javafaker.Faker
import java.util.UUID

val faker = Faker()

object TestBuilders {

    fun buildCreateBedRequest(
        roomId: UUID = UUID.randomUUID(),
        hospitalId: UUID = UUID.randomUUID(),
        type: BedType = faker.options().option(BedType::class.java),
        features: List<HospitalBedFeatureRequest> = listOf(
            faker.options().option(HospitalBedFeatureRequest::class.java)
        ),
        extraFeatures: List<String> = listOf(faker.beer().name()),
        manufacturerId: UUID = UUID.randomUUID(),
        manufacturer: String = faker.company().name(),
        details: String = faker.lorem().paragraph(),
    ): CreateBedRequest = CreateBedRequest(
        roomId = roomId,
        hospitalId = hospitalId,
        type = type,
        features = features,
        extraFeatures = extraFeatures,
        manufacturerId = manufacturerId,
        manufacturer = manufacturer,
        details = details
    )

    fun buildHospitalBed(
        id: HospitalBedId = HospitalBedId(UUID.randomUUID()),
        location: BedLocation = BedLocation(HospitalId(UUID.randomUUID()), RoomId(UUID.randomUUID())),
        type: BedType = faker.options().option(BedType::class.java),
        characteristics: List<HospitalBedFeature> = listOf(HospitalBedFeature.Electric),
        manufacturerId: ManufacturerId = ManufacturerId(faker.idNumber().valid()),
        manufacturer: String = faker.company().name(),
        details: String? = faker.lorem().paragraph(),
    ): HospitalBed = HospitalBed(
        id = id,
        location = location,
        type = type,
        characteristics = characteristics,
        manufacturerId = manufacturerId,
        manufacturer = manufacturer,
        details = details,
    )
}

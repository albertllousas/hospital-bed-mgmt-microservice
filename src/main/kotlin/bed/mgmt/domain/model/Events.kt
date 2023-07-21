package bed.mgmt.domain.model

sealed interface DomainEvent

data class BedCreated(val bed: HospitalBed): DomainEvent

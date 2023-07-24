package bed.mgmt.domain.model

sealed interface DomainError

object BedAlreadyExists : DomainError

package bed.functional.core

sealed interface DomainError

object BedNotFound : DomainError

object PatientNotFound : DomainError

interface AllocateError : DomainError

object PatientNotAdmitted : AllocateError

interface ReleaseError : DomainError

interface MovePatientError : DomainError

object BedAlreadyAllocated : AllocateError, MovePatientError

object InvalidDate : AllocateError

object BedAlreadyReleased : ReleaseError, MovePatientError

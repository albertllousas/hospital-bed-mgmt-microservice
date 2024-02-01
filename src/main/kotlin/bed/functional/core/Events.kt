package bed.functional.core

sealed interface DomainEvent {
    val bed: Bed
}

data class BedCreated(override val bed: Bed) : DomainEvent

data class BedAllocated(override val bed: Bed) : DomainEvent

data class BedReleased(override val bed: Bed) : DomainEvent

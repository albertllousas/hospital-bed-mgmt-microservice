package bed.functional.core

import arrow.core.Either
import kotlin.reflect.KClass

interface BedRepository {

    fun save(bed: Bed, transaction: Transaction)
    fun find(bedId: BedId): Either<BedNotFound, Bed>
    fun findAvailable(
        ward: Ward? = null,
        features: List<BedFeature> = emptyList(),
        limit: Int = 50,
        offset: Int = 0
    ): List<Bed>
}

interface PatientFinder {
    fun find(id: PatientId): Either<PatientNotFound, Patient>
}

interface Transactional {
    operator fun <T> invoke(block: (Transaction) -> T): T
}

interface Transaction

interface EventPublisher {
    fun publish(events: List<DomainEvent>, transaction: Transaction)
}

interface  ErrorReporter {

    fun <T: Any> report(error: DomainError, origin: KClass<T>)

    fun report(crash: Throwable)
}

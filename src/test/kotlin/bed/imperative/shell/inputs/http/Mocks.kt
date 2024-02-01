package bed.imperative.shell.inputs.http

import arrow.core.Either
import bed.functional.core.AllocateError
import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.BedRepository
import bed.functional.core.ErrorReporter
import bed.functional.core.EventPublisher
import bed.functional.core.MovePatientError
import bed.functional.core.Patient
import bed.functional.core.PatientFinder
import bed.functional.core.RoomId
import bed.functional.core.Ward
import bed.imperative.shell.config.DatabaseBeansFactory
import bed.fixtures.FakeTransactional
import bed.functional.core.ReleaseError
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.mockk.mockk
import jakarta.inject.Singleton
import java.time.Clock
import java.time.LocalDate

@Factory
@Requires(property = "mocks.http.factory.enabled", value = "true", defaultValue = "false")
class Mocks {

    @Singleton
    fun transactional() = FakeTransactional()

    @Singleton
    fun bedRepository() = mockk<BedRepository>(relaxed = true)

    @Singleton
    fun eventPublisher() = mockk<EventPublisher>(relaxed = true)

    @Singleton
    fun patientFinder() = mockk<PatientFinder>()

    @Singleton
    fun errorReporter() = mockk<ErrorReporter>(relaxed = true)

    @Singleton
    fun allocate() = mockk<(Bed, Patient, Clock, LocalDate?) -> Either<AllocateError, Bed>>()

//    @Replaces(named = "create")
    @Singleton
    fun create() = mockk<(BedId, RoomId, Ward, List<BedFeature>) -> Bed>(relaxed = true)

//    @Replaces(named = "move")
    @Singleton
    fun move() = mockk<(Bed, Bed, Clock) -> Either<MovePatientError, Pair<Bed,Bed>>>()

//    @Replaces(named = "release")
    @Singleton
    fun release() = mockk<(Bed) -> Either<ReleaseError, Bed>>()
}

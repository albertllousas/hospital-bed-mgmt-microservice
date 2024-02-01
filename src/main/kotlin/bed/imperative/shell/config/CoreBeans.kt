package bed.imperative.shell.config

import arrow.core.Either
import bed.functional.core.AllocateError
import bed.functional.core.Bed
import bed.functional.core.BedFeature
import bed.functional.core.BedId
import bed.functional.core.MovePatientError
import bed.functional.core.Patient
import bed.functional.core.ReleaseError
import bed.functional.core.RoomId
import bed.functional.core.Ward
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.Clock
import java.time.LocalDate
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4

@Factory
@Requires(property = "core.beans.factory.enabled", value = "true", defaultValue = "true")
class CoreBeans {

    @Singleton
    fun allocate(): (Bed, Patient, Clock, LocalDate?) -> Either<AllocateError, Bed> = { a,b,c,d -> Bed.allocate(a,b,c,d) }

    @Singleton
    fun create(): (BedId, RoomId, Ward, List<BedFeature>) -> Bed = { a,b,c,d -> Bed.create(a,b,c,d) }

    @Singleton
    fun movePatient():(Bed, Bed, Clock) -> Either<MovePatientError, Pair<Bed,Bed>> = { a,b,c -> Bed.movePatient(a,b,c) }

    @Singleton
    fun release(): (Bed) -> Either<ReleaseError, Bed> = { Bed.release(it) }
}

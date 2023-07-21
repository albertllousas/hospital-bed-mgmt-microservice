package bed.mgmt

import arrow.core.raise.Raise
import jakarta.inject.Singleton

@Singleton
class SomeService {

    context(Raise<String>)
    fun msg() = "Hello world!!!!!!"

}
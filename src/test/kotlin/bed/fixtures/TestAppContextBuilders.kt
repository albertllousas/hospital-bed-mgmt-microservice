package bed.fixtures

import io.micronaut.context.DefaultApplicationContextBuilder

class LazyApplicationContextBuilder : DefaultApplicationContextBuilder() {
    init {
        eagerInitSingletons(false)
//        eagerInitAnnotated(null)
        eagerInitConfiguration(false)
    }
}

class EagerApplicationContextBuilder : DefaultApplicationContextBuilder() {
    init {
        eagerInitSingletons(true)
        eagerInitConfiguration(true)
    }
}

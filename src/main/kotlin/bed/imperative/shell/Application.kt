package bed.imperative.shell

import io.micronaut.runtime.Micronaut.build

fun main(args: Array<String>) {
    build()
        .args(*args)
        .eagerInitSingletons(true)
        .start()
}

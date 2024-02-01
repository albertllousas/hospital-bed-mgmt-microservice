package bed.fixtures

import arrow.core.raise.Raise
import bed.fixtures.Stubs.MethodCallInterceptor.Crash
import bed.fixtures.Stubs.MethodCallInterceptor.PredefinedAnswer
import bed.fixtures.Stubs.MethodCallInterceptor.RaiseError
import bed.fixtures.Stubs.MethodCallInterceptor.Success
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility.PUBLIC
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.BindingPriority
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.matcher.ElementMatchers.*
import org.objenesis.ObjenesisHelper
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaMethod

object Stubs {

    class MethodCallInterceptor {

        sealed class PredefinedAnswer
        data class Success(val value: Any) : PredefinedAnswer()
        data class RaiseError(val error: Any) : PredefinedAnswer()
        data class Crash(val crash: Exception) : PredefinedAnswer()

        private val answers = mutableListOf<Pair<Method, PredefinedAnswer>>()

        context (Raise<Any>)
        @RuntimeType
        @BindingPriority(Int.MAX_VALUE)
        @Throws(Exception::class)
        fun intercept(@Origin method: Method, @AllArguments args: Array<Any>): Any? =
            answers.find { it.first.signature() == method.signature() }
                ?.let {
                    when (val value = it.second) {
                        is Crash -> throw value.crash
                        is RaiseError -> raise(value.error)
                        is Success -> value.value
                    }
                } ?: (throw Exception("Method: $method has not a canned answer for this stub"))

        private fun Method.signature() =
            this.name + this.parameterTypes.map { it.name } + this.returnType.name

        @RuntimeType
        fun addAnswer(method: Method, predefinedAnswer: PredefinedAnswer) {
            this.answers.add(Pair(method, predefinedAnswer))
        }

        @RuntimeType
        fun clearAnswers() = run { answers.clear() }
    }

    inline fun <reified T : Any> buildStub(): T {
        val methodCallInterceptor = MethodCallInterceptor()
        val make = ByteBuddy()
            .subclass(T::class.java)
            .let {
                T::class.declaredFunctions.fold(it) { builder, fn ->
                    builder.method(
                        named<MethodDescription?>(fn.name)
                            .and(isDeclaredBy(T::class.java))
                            .and(takesArguments(*(fn.javaMethod!!.parameterTypes)))
                    ).intercept(MethodDelegation.to(methodCallInterceptor))
                }
            }

            .defineMethod("addAnswer", Unit::class.java, PUBLIC)
            .withParameters(Method::class.java, PredefinedAnswer::class.java)
            .intercept(MethodDelegation.to(methodCallInterceptor))
            .defineMethod("clearAnswers", Unit::class.java, PUBLIC)
            .intercept(MethodDelegation.to(methodCallInterceptor))
            .make()
        val clazz = make
            .load(T::class.java.classLoader)
            .loaded
        return ObjenesisHelper.newInstance(clazz)
    }

    fun <T> givenStub(stub: T): StubAnswerBuilder<T> = StubAnswerBuilder(stub)

    fun clearStub(stub: Any) =
        stub::class.declaredFunctions.find { it.name == "clearAnswers" }
            ?.also { it.call(stub) }
            ?: throw Exception("Method: clearAnswers is not a declared function of ${stub!!::class}")

    class StubAnswerBuilder<T>(private val stub: T) {

        infix fun whenCallOn(fn: KFunction<*>): StubAnswerFinalBuilder<T> = StubAnswerFinalBuilder(this.stub, fn)

        inner class StubAnswerFinalBuilder<T>(private val stub: T, private val fn: KFunction<*>) {

            infix fun thenRaise(error: Any) = answerWith(RaiseError(error))

            infix fun thenReturn(value: Any) = answerWith(Success(value))

            infix fun thenThrow(exception: Exception) = answerWith(Crash(exception))

            private fun answerWith(answer: PredefinedAnswer) {
                stub!!::class.declaredFunctions.find { it.signature() == fn.signature() }
                    ?.also {
                        stub!!::class.declaredFunctions.find { m -> m.name == "addAnswer" }!!
                            .call(stub, it.javaMethod, answer)
                    } ?: throw Exception("Method: $fn is not a declared function of ${stub!!::class}")
            }

            @OptIn(ExperimentalStdlibApi::class)
            private fun KFunction<*>.signature() =
                this.name + this.javaMethod!!.parameterTypes.map { it } + this.returnType.javaType
        }
    }
}

package bed.mgmt

import arrow.core.raise.recover
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

@Controller("/hello")
class HelloController(private val someService: SomeService) {

    @Get
    @Produces(MediaType.TEXT_PLAIN)
    fun index() = recover(block = {someService.msg()}) {}
}
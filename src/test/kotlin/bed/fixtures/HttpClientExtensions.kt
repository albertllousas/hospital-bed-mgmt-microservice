package bed.fixtures

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException

fun <I, O> BlockingHttpClient.call(request: HttpRequest<I>, bodyType: Class<O>): HttpResponse<O> = try {
    this.exchange(request, bodyType)
} catch (exception: HttpClientResponseException) {
    exception.response as HttpResponse<O>
}
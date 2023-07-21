package bed.mgmt.infra.`in`.http

import arrow.core.raise.Raise
import bed.mgmt.application.service.CreateBedRequest
import bed.mgmt.application.service.CreateBedService
import bed.mgmt.domain.model.BedAlreadyExists
import bed.mgmt.domain.model.BedCreated
import bed.mgmt.domain.model.HospitalDoesNotExists
import bed.mgmt.domain.model.RoomDoesNotExists
import bed.mgmt.fixtures.TestBuilders
import io.kotest.matchers.shouldBe
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID


class CustomContextBuilder : DefaultApplicationContextBuilder() {
    init {
        eagerInitSingletons(false)
    }
}

@MicronautTest(contextBuilder = [CustomContextBuilder::class])
class HospitalBedsControllerTest(
    @Client("/") private val client: HttpClient,
    @Inject private val createBedService: CreateBedServiceStub
) {

    @Test
    fun `should create a hospital bed through an http endpoint`() {
        val bed = TestBuilders.buildHospitalBed()

        createBedService.stub = { BedCreated(bed) }
        val request: HttpRequest<Any> = HttpRequest.POST(
            "/hospital-beds",
            """
               {
                    "room_id":"${UUID.randomUUID()}",
                    "hospital_id":"${UUID.randomUUID()}",
                    "type":"STANDARD",
                    "features":["WHEELS"],
                    "extra_features":[],
                    "manufacturer_id":"${UUID.randomUUID()}",
                    "manufacturer":"ROLKO",
                    "details":"Some details"            
                }
            """
        )
        val response = client.toBlocking().call(request, String::class.java)

        response.status shouldBe HttpStatus.CREATED
        assertThat(response.body()).isEqualTo("""{"id":"${bed.id.value}"}""")
    }

    @Replaces(CreateBedService::class)
    @Singleton
    open class CreateBedServiceStub : CreateBedService() {
        var stub: () -> BedCreated = { throw Exception("Property stub Not initialized") }

        context(Raise<BedAlreadyExists>, Raise<HospitalDoesNotExists>, Raise<RoomDoesNotExists>)
        override operator fun invoke(request: CreateBedRequest): BedCreated = stub()
    }

}

fun <I, O> BlockingHttpClient.call(request: HttpRequest<I>, bodyType: Class<O>): HttpResponse<O> = try {
    this.exchange(request, bodyType)
} catch (exception: HttpClientResponseException) {
    exception.response as HttpResponse<O>
}

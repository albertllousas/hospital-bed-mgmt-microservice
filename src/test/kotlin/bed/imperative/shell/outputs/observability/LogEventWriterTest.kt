package bed.imperative.shell.outputs.observability

import bed.fixtures.TestBuilders
import bed.functional.core.BedAlreadyAllocated
import bed.functional.core.BedCreated
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger

@Tag("integration")
class LogEventWriterTest {

    private val logger = spyk(NOPLogger.NOP_LOGGER)

    private val logEventWriter = LogEventWriter(logger)

    @Test
    fun `log a domain event`() {
        val bedCreated = BedCreated(TestBuilders.buildBed())

        logEventWriter.log(bedCreated)

        verify { logger.info("domain-event: 'BedCreated', bed-id: '${bedCreated.bed.id.value}'") }
    }


    @Test
    fun `log a domain error`() {
        val error = BedAlreadyAllocated

        logEventWriter.log(error, this::class)

        verify { logger.warn("domain-error: 'BedAlreadyAllocated', origin: 'LogEventWriterTest'") }
    }

    @Test
    fun `log an application crash`() {
        val boom = Exception("boom")

        logEventWriter.log(boom)

        verify { logger.error("application crash", boom) }
    }
}

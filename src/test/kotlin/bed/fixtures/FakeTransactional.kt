package bed.fixtures

import bed.functional.core.Transaction
import bed.functional.core.Transactional

object FakeTransaction : Transaction

open class FakeTransactional : Transactional {
    override fun <T> invoke(block: (Transaction) -> T): T = block(FakeTransaction)
}
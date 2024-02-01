package bed.imperative.shell.outputs.db

import bed.functional.core.Transaction
import bed.functional.core.Transactional
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class JDBITransactional(private val jdbi: Jdbi) : Transactional {
    override fun <T> invoke(block: (Transaction) -> T): T =
        jdbi.inTransaction<T, Exception> { handle -> block(JdbiTransaction(handle)) }
}

data class JdbiTransaction(val handle: Handle) : Transaction

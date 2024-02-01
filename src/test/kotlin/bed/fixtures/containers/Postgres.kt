package bed.fixtures.containers

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer

class Postgres(network: Network? = null) {

    val container: KtPostgreSQLContainer = KtPostgreSQLContainer()
        .withNetwork(network?: Network.newNetwork())
        .withNetworkAliases("localhost")
        .withCommand("postgres -c wal_level=logical")
        .withUsername("hospitalbeds")
        .withPassword("hospitalbeds")
        .withDatabaseName("hospitalbeds")
        .also {
            it.start()
            Flyway(FluentConfiguration().dataSource(it.jdbcUrl, it.username, it.password)).migrate()
        }
}

class KtPostgreSQLContainer : PostgreSQLContainer<KtPostgreSQLContainer>("postgres:latest")

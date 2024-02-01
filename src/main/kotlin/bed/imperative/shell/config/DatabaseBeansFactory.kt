package bed.imperative.shell.config

import bed.imperative.shell.outputs.db.JDBITransactional
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.jdbi.v3.core.Jdbi
import javax.sql.DataSource

@Factory
@Requires(property = "database.beans.factory.enabled", value = "true", defaultValue = "true")
class DatabaseBeansFactory {

    @Singleton
    @Bean
    fun hikariDataSource(
        @Property(name = "datasource.password") datasourcePassword: String,
        @Property(name = "datasource.username") datasourceUsername: String,
        @Property(name = "datasource.jdbc-url") datasourceUrl: String,
        @Property(name = "datasource.driver-class-name") datasourceDriverClassName: String,
        @Property(name = "hikari.maximum-pool-size") hikariMaximumPoolSize: Int,
        @Property(name = "hikari.minimum-idle") hikariMinimumIdle: Int,
        @Property(name = "hikari.idle-timeout") hikariIdleTimeout: Long,
        @Property(name = "hikari.max-lifetime") hikariMaxLifetime: Long,
        @Property(name = "hikari.connection-timeout") hikariConnectionTimeout: Long,
        @Property(name = "hikari.pool-name") hikariPoolName: String
    ): DataSource = HikariConfig().apply {
        jdbcUrl = datasourceUrl
        username = datasourceUsername
        password = datasourcePassword
        driverClassName = datasourceDriverClassName
        maximumPoolSize = hikariMaximumPoolSize
        minimumIdle = hikariMinimumIdle
        idleTimeout = hikariIdleTimeout
        maxLifetime = hikariMaxLifetime
        connectionTimeout = hikariConnectionTimeout
        poolName = hikariPoolName
    }.let { HikariDataSource(it) }

    @Singleton
    @Bean
    fun jdbi(dataSource: DataSource): Jdbi = Jdbi.create(dataSource)

    @Singleton
    @Bean
    fun transactional(jdbi: Jdbi) = JDBITransactional(jdbi)

}
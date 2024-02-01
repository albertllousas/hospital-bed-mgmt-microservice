import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.22"
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
    id("io.micronaut.application") version "4.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

object Versions {
    const val POSTGRES = "42.2.23"
    const val JUNIT = "5.9.1"
    const val MOCKK = "1.13.5"
    const val ARROW = "1.2.1"
    const val ASSERTJ = "3.23.1"
    const val JDBI = "3.29.0"
    const val TESTCONTAINERS = "1.17.2"
    const val FLYWAY = "8.5.11"
    const val WIREMOCK = "2.27.2"
}

application {
    mainClass.set("bed.mgmt.ApplicationKt")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
//  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-core:${Versions.ARROW}")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime:4.2.0")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.jdbi:jdbi3-core:3.43.0")
//    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.kafka:micronaut-kafka")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut:micronaut-http-client")

//    implementation("io.micronaut.micrometer:micronaut-management")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-statsd")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.postgresql:postgresql:${Versions.POSTGRES}")
//    implementation("org.jdbi:jdbi3-core:${Versions.JDBI}")
    implementation("org.flywaydb:flyway-core:${Versions.FLYWAY}")
    runtimeOnly("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation(group = "io.mockk", name = "mockk", version = Versions.MOCKK)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.JUNIT}")
    testImplementation(group = "org.assertj", name = "assertj-core", version = Versions.ASSERTJ)
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("com.github.javafaker:javafaker:1.0.2") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    testImplementation(group =  "org.testcontainers", name = "testcontainers", version = Versions.TESTCONTAINERS)
    testImplementation(group =  "org.testcontainers", name = "kafka", version = Versions.TESTCONTAINERS)
    testImplementation("org.testcontainers:postgresql:${Versions.TESTCONTAINERS}")
    testImplementation("io.debezium:debezium-testing-testcontainers:2.4.1.Final")
    testImplementation("com.github.tomakehurst:wiremock:${Versions.WIREMOCK}")
    testImplementation("io.rest-assured:rest-assured:4.4.0")
    runtimeOnly("org.yaml:snakeyaml:2.0")
}

tasks.apply {
    test {
        maxParallelForks = 1
        enableAssertions = true
        useJUnitPlatform {}
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xinline-classes", "-Xcontext-receivers")
        }
    }
}
graalvmNative {
    toolchainDetection.set(false)
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("bed.mgmt.*")
    }
}

allOpen {
    annotation("jakarta.inject.Singleton")
}

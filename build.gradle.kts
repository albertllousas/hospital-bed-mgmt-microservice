import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.22"
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
    id("io.micronaut.application") version "4.0.0-M8"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

object Versions {
    const val JUNIT = "5.9.1"
    const val MOCKK = "1.13.5"
    const val ARROW = "1.2.0-RC"
    const val ASSERTJ = "3.23.1"
}

application {
    mainClass.set("bed.mgmt.ApplicationKt")
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
//  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-core:${Versions.ARROW}")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

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
    testImplementation("org.yaml:snakeyaml:2.0")
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

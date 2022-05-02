import kotlin.text.Charsets.UTF_8

plugins {
    // kotlin support
    // kotlin support
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    // linters
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)

    application
}

group = "aikisib"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap/")
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("aikisib.contact7.MainContact7Kt")
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
    systemProperty("file.encoding", UTF_8.toString())
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.bundles.kotlin.serialization)
    implementation(libs.log4j.api)
    implementation(libs.slf4j.api)
    implementation(libs.coroutines.core)

    // configuration
    implementation(libs.owner)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client.base)
    implementation(libs.ktor.client.content.negotiation)

    // рантаймовая зависимость на реализацию логирования slf4j для прода
    // logging facade
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.jul.to.slf4j)
    runtimeOnly(libs.log4j.to.slf4j)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.jansi)

    testImplementation(libs.assertj)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)

    // рантаймовая зависимость на реализацию логирования slf4j для тестов
    testRuntimeOnly(libs.jul.to.slf4j)
    testRuntimeOnly(libs.log4j.to.slf4j)
    testRuntimeOnly(libs.logback)
    testRuntimeOnly(libs.jansi)
}

plugins {
    // kotlin support
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    // linters
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)

    application
}

application {
    // Define the main class for the application.
    mainClass.set("aikisib.contact7.MainContact7Kt")

    applicationDistribution.from("src/main/start") {
        include("start.sh", "contact7handler.service")
        into("bin")
    }
    applicationDistribution.from("src/main/resources") {
        include("contact7.properties")
        into("etc")
    }
}

tasks.jar {
    exclude("**/*.properties", "**/*.example", "**/*.gitignore")
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    applicationDistribution.from("src/main/resources/apply/static") {
        into("static")
    }
}

tasks.jar {
    exclude("**/*.properties", "**/*.example", "**/*.gitignore")
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.netty.bom))
    implementation(libs.bundles.kotlin.serialization)
    implementation(libs.slf4j.api)
    implementation(libs.coroutines.core)
    implementation(libs.ktor.thymeleaf)

    // configuration
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

    ktlint(libs.logback)

    testImplementation(libs.assertj)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)

    // рантаймовая зависимость на реализацию логирования slf4j для тестов
    testRuntimeOnly(libs.jul.to.slf4j)
    testRuntimeOnly(libs.log4j.to.slf4j)
    testRuntimeOnly(libs.logback)
    testRuntimeOnly(libs.jansi)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }
}

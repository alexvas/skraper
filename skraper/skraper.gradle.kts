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
    mainClass.set("aikisib.MainKt")
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.netty.bom))

    implementation(libs.kotlin.serialization.hocon)
    implementation(libs.bundles.web.driver.manager)
    implementation(libs.slf4j.api)

    implementation(libs.selenium.java)
    implementation(libs.jsoup)
    implementation(libs.coroutines.core)

    implementation(libs.bundles.ktor.client.base)
    implementation(libs.jsitemapgenerator)

    // рантаймовая зависимость на реализацию логирования slf4j для прода
    // logging facade
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.jul.to.slf4j)
    runtimeOnly(libs.log4j.to.slf4j)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.jansi)

    ktlint(libs.logback)

    testImplementation(libs.selenium.chrome.driver)
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

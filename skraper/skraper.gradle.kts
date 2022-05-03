plugins {
    // kotlin support
    alias(libs.plugins.kotlin.jvm)
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

    implementation(libs.bundles.web.driver.manager)
    implementation(libs.log4j.api)
    implementation(libs.slf4j.api)

    implementation(libs.selenium.java)
    implementation(libs.jsoup)
    implementation(libs.coroutines.core)

    // configuration
    implementation(libs.owner)

    implementation(libs.bundles.ktor.client.base)
    implementation(libs.jsitemapgenerator)

    // рантаймовая зависимость на реализацию логирования slf4j для прода
    // logging facade
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.jul.to.slf4j)
    runtimeOnly(libs.log4j.to.slf4j)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.jansi)

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

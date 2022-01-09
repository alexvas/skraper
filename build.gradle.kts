import kotlin.text.Charsets.UTF_8

plugins {
    // kotlin support
    kotlin("jvm") version "1.6.10"
    // linters
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.18.1"

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
    mainClass.set("aikisib.MainKt")
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
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    val webDriverManagerVersion = "5.0.3"
    implementation("io.github.bonigarcia:webdrivermanager:$webDriverManagerVersion")
    implementation("org.apache.logging.log4j:log4j-api:2.17.0")
    implementation("org.slf4j:slf4j-api:1.7.32")
    val seleniumVersion = "4.1.1"
    implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // configuration
    implementation("org.aeonbits.owner:owner:1.0.12")

    val ktorVersion = "2.0.0-eap-283"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("cz.jiripinkas:jsitemapgenerator:4.5")

    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")

    testImplementation("org.assertj:assertj-core:3.21.0")

    // рантаймовая зависимость на реализацию логирования slf4j для прода
    // logging facade
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    runtimeOnly("org.slf4j:jul-to-slf4j:1.7.32")
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.17.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.10")
    runtimeOnly("org.fusesource.jansi:jansi:2.4.0")

    val junitVersion = "5.8.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    // рантаймовая зависимость на реализацию логирования slf4j для тестов
    testRuntimeOnly("org.slf4j:jul-to-slf4j:1.7.32")
    testRuntimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.17.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.9")
    testRuntimeOnly("org.fusesource.jansi:jansi:2.4.0")
}

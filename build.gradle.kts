import kotlin.text.Charsets.UTF_8

plugins {
    // kotlin support
    kotlin("jvm") version "1.6.0"
    // linters
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.18.1"

    application
}

group="aikisib"
version="1.0-SNAPSHOT"

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
    implementation("org.apache.logging.log4j:log4j-api:2.16.0")
    implementation("org.slf4j:slf4j-api:1.7.32")
    val seleniumVersion = "4.1.0"
    implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("commons-validator:commons-validator:1.7")

    // configuration
    implementation("org.aeonbits.owner:owner:1.0.12")

    val ktorVersion = "2.0.0-eap-283"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    val kotestVersion = "5.0.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")

    testImplementation("org.assertj:assertj-core:3.21.0")

    // рантаймовая зависимость на реализацию логирования slf4j для прода
    // logging facade
    implementation("io.github.microutils:kotlin-logging:2.1.16")
    runtimeOnly("org.slf4j:jul-to-slf4j:1.7.32")
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.14.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // рантаймовая зависимость на реализацию логирования slf4j для тестов
    testRuntimeOnly("org.slf4j:jul-to-slf4j:1.7.32")
    testRuntimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.14.1")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.8")
}

rootProject.name = "skraper"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {

            version("kotlin", "1.6.21")
            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            library("kotlin.bom", "org.jetbrains.kotlin", "kotlin-bom").versionRef("kotlin")

            version("kotlin.serialization", "1.3.1")
            library(
                "kotlin.serialization.core",
                "org.jetbrains.kotlinx",
                "kotlinx-serialization-core",
            ).versionRef("kotlin.serialization")
            library(
                "kotlin.serialization.json",
                "org.jetbrains.kotlinx",
                "kotlinx-serialization-json",
            ).versionRef("kotlin.serialization")
            bundle("kotlin.serialization", listOf("kotlin.serialization.core", "kotlin.serialization.json"))

            version("coroutines", "1.6.1")
            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")

            version("webDriverManager", "5.1.1")
            version("bouncyCastle", "1.70")
            library("web.driver.manager", "io.github.bonigarcia", "webdrivermanager").versionRef("webDriverManager")
            library("jackson.databind", "com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
            library("commons.io", "commons-io:commons-io:2.11.0")
            library("bouncyCastle.prov", "org.bouncycastle", "bcprov-jdk15on").versionRef("bouncyCastle")
            library("bouncyCastle.pkix", "org.bouncycastle", "bcpkix-jdk15on").versionRef("bouncyCastle")
            bundle(
                "web.driver.manager",
                listOf(
                    "web.driver.manager",
                    "jackson.databind",
                    "commons.io",
                    "bouncyCastle.prov",
                    "bouncyCastle.pkix",
                ),
            )

            version("log4j", "2.17.2")
            library("log4j.api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
            library("log4j.to.slf4j", "org.apache.logging.log4j", "log4j-to-slf4j").versionRef("log4j")

            version("slf4j", "1.7.36")
            library("slf4j.api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("jul.to.slf4j", "org.slf4j", "jul-to-slf4j").versionRef("slf4j")

            version("selenium", "4.1.4")
            library("selenium.java", "org.seleniumhq.selenium", "selenium-java").versionRef("selenium")
            library(
                "selenium.chrome.driver",
                "org.seleniumhq.selenium",
                "selenium-chrome-driver",
            ).versionRef("selenium")
            library("jsoup", "org.jsoup:jsoup:1.14.3")
            library("owner", "org.aeonbits.owner:owner:1.0.12")

            version("ktor", "2.0.1")
            library("ktor.client.core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor.client.cio", "io.ktor", "ktor-client-cio").versionRef("ktor")
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            library("ktor.client.content.negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            bundle(
                "ktor.client.base",
                listOf(
                    "ktor.client.core",
                    "ktor.client.cio",
                    "ktor.client.logging",
                ),
            )
            library("ktor.server.core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor.server.cio", "io.ktor", "ktor-server-cio").versionRef("ktor")
            library("ktor.server.content.negotiation", "io.ktor", "ktor-server-content-negotiation").versionRef("ktor")
            library("ktor.server.locations", "io.ktor", "ktor-server-locations").versionRef("ktor")
            library("ktor.server.status.pages", "io.ktor", "ktor-server-status-pages").versionRef("ktor")
            library("ktor.server.serialization.json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            bundle(
                "ktor.server",
                listOf(
                    "ktor.server.core",
                    "ktor.server.cio",
                    "ktor.server.content.negotiation",
                    "ktor.server.locations",
                    "ktor.server.status.pages",
                    "ktor.server.serialization.json",
                ),
            )

            library("jsitemapgenerator", "cz.jiripinkas:jsitemapgenerator:4.5")
            library("kotlin.logging", "io.github.microutils:kotlin-logging:2.1.21")
            library("logback", "ch.qos.logback:logback-classic:1.2.11")
            library("jansi", "org.fusesource.jansi:jansi:2.4.0")

            // test
            library("assertj", "org.assertj:assertj-core:3.22.0")
            version("junit", "5.8.2")
            library("jupiter.api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("jupiter.engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")

            // plugins
            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("10.2.1")
            plugin("detekt", "io.gitlab.arturbosch.detekt").version("1.21.0")
        }
    }
}

include(
    "contact7handler",
    "skraper",
)

// назначаем сборочным скриптам понятные имена
fun ProjectDescriptor.configureBuildFile() {
    buildFileName = "$name.gradle.kts"
    require(buildFile.isFile) {
        "$buildFile must exist"
    }
    children.forEach {
        it.configureBuildFile()
    }
}

rootProject.children.forEach { it.configureBuildFile() }

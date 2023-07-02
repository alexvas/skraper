rootProject.name = "skraper"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {

            version("kotlinVersion", "1.8.22")
            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlinVersion")
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlinVersion")
            library("kotlin.bom", "org.jetbrains.kotlin", "kotlin-bom").versionRef("kotlinVersion")

            val serialization = "1.5.1"
            library("kotlin.serialization.core", "org.jetbrains.kotlinx", "kotlinx-serialization-core")
                .version(serialization)
            library("kotlin.serialization.json", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
                .version(serialization)
            library("kotlin.serialization.hocon", "org.jetbrains.kotlinx", "kotlinx-serialization-hocon")
                .version(serialization)
            bundle("kotlin.serialization", listOf("kotlin.serialization.core", "kotlin.serialization.json", "kotlin.serialization.hocon"))

            val coroutines = "1.7.1"
            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version(coroutines)

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

            val slf4j = "2.0.7"
            library("slf4j.api", "org.slf4j", "slf4j-api").version(slf4j)
            library("jul.to.slf4j", "org.slf4j", "jul-to-slf4j").version(slf4j)
            library("log4j.to.slf4j", "org.slf4j", "log4j-over-slf4j").version(slf4j)

            version("selenium", "4.1.4")
            library("selenium.java", "org.seleniumhq.selenium", "selenium-java").versionRef("selenium")
            library(
                "selenium.chrome.driver",
                "org.seleniumhq.selenium",
                "selenium-chrome-driver",
            ).versionRef("selenium")
            library("jsoup", "org.jsoup:jsoup:1.14.3")

            val ktor = "2.3.1"
            library("ktor.client.core", "io.ktor", "ktor-client-core").version(ktor)
            library("ktor.client.cio", "io.ktor", "ktor-client-cio").version(ktor)
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").version(ktor)
            library("ktor.client.content.negotiation", "io.ktor", "ktor-client-content-negotiation").version(ktor)
            bundle(
                "ktor.client.base",
                listOf(
                    "ktor.client.core",
                    "ktor.client.cio",
                    "ktor.client.logging",
                ),
            )
            library("ktor.server.core", "io.ktor", "ktor-server-core").version(ktor)
            library("ktor.server.cio", "io.ktor", "ktor-server-cio").version(ktor)
            library("ktor.server.content.negotiation", "io.ktor", "ktor-server-content-negotiation").version(ktor)
            library("ktor.server.locations", "io.ktor", "ktor-server-locations").version(ktor)
            library("ktor.server.status.pages", "io.ktor", "ktor-server-status-pages").version(ktor)
            library("ktor.server.serialization.json", "io.ktor", "ktor-serialization-kotlinx-json").version(ktor)
            library("ktor.thymeleaf", "io.ktor", "ktor-server-thymeleaf").version(ktor)
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
            library("kotlin.logging", "io.github.microutils:kotlin-logging:3.0.5")
            library("logback", "ch.qos.logback:logback-classic:1.4.7")
            library("jansi", "org.fusesource.jansi:jansi:2.4.0")

            // test
            library("assertj", "org.assertj:assertj-core:3.22.0")
            version("junit", "5.8.2")
            library("jupiter.api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("jupiter.engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")

            // plugins
            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("11.4.0")
            plugin("detekt", "io.gitlab.arturbosch.detekt").version("1.23.0")
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

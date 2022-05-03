import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.text.Charsets.UTF_8

plugins {
    // kotlin support
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // linters
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    group = "aikisib"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configurations.all {
        /* настройка логирования log4j[2] -> slf4j: начало */
        exclude(group = "org.slf4j", module = "slf4j-log4j12") // бридж в обратную сторону не нужен
        exclude(group = "org.slf4j", module = "log4j-over-slf4j") // потому что пользуемся бриджем log4j-to-slf4j
        exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl") // реализация log4j2 не нужна
        exclude(group = "log4j", module = "log4j") // / реализация log4j не нужна
    }

    afterEvaluate {
        extensions.apply {
            findByType(KotlinJvmProjectExtension::class)?.apply {
                jvmToolchain {
                    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("11"))
                }
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            reports {
                junitXml.required.set(true)
                html.required.set(true)
            }
            systemProperty("file.encoding", UTF_8.toString())
        }
    }
}

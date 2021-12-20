package aikisib

import aikisib.mirror.RecursiveScraper
import aikisib.mirror.RecursiveScraperImpl
import aikisib.slider.SliderRepo
import aikisib.slider.SliderRevolutionScraper
import aikisib.slider.SliderRevolutionScraperImpl
import mu.KLoggable
import org.aeonbits.owner.Config
import org.aeonbits.owner.ConfigFactory
import java.io.File
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import kotlin.reflect.KClass
import kotlin.system.exitProcess

suspend fun main() {
    setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val mainConfig: MainConfig = createConfig(MainConfig::class)

//    exportSliderRevolutionModules()
    mirrorSite(mainConfig)
}

suspend fun mirrorSite(sliderRepo: MainConfig) {
    val recursiveScraper: RecursiveScraper = RecursiveScraperImpl()
    recursiveScraper.mirror(sliderRepo.publicUrl(), File("/tmp/stockDir"))
}

@Suppress("UnusedPrivateMember")
private suspend fun exportSliderRevolutionModules(sliderRepo: SliderRepo) {
    val vault: Vault = createConfig(Vault::class)
    val sliderRevolutionScraper: SliderRevolutionScraper = SliderRevolutionScraperImpl(sliderRepo.adminUrl())

    val success = sliderRevolutionScraper.loginIntoWordpress(
        wordpressLoginPath = vault.wordpressLoginPath(),
        username = vault.username(),
        password = vault.password(),
    )

    if (!success) {
        println("Не удалось залогиниться в Вордпресс")
        exitProcess(1)
    }

    sliderRepo.sliderIds().forEach {
        val nonce = sliderRevolutionScraper.navigateToSliderRevolutionPage()
        exportModuleToHtml(sliderRevolutionScraper, it, nonce)
    }
}

private suspend fun exportModuleToHtml(
    sliderRevolutionScraper: SliderRevolutionScraper,
    id: Int,
    nonce: String,
) {
    val zip = sliderRevolutionScraper.downloadModuleZip(id, nonce)
    val target = File("/tmp/module$id").also { it.mkdirs() }
    UnzipFile.unzip(target, zip)
    println("module $id successfully exported to $target")
}

private fun <T : Config> createConfig(kClass: KClass<T>): T =
    ConfigFactory.create(kClass.java)

/**
 * Last resort exception handler
 */
internal class DefaultUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, KLoggable {
    override val logger = logger()

    override fun uncaughtException(t: Thread, e: Throwable) =
        logger.error(e) { "Unhandled exception" }
}

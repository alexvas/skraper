package aikisib

import aikisib.mirror.ContentTransformerFactory
import aikisib.mirror.ContentTransformerFactoryImpl
import aikisib.mirror.Downloader
import aikisib.mirror.DownloaderImpl
import aikisib.mirror.FromLinkFilter
import aikisib.mirror.FromLinkFilterImpl
import aikisib.mirror.LinkExtractor
import aikisib.mirror.LinkExtractorImpl
import aikisib.mirror.RecursiveScraper
import aikisib.mirror.RecursiveScraperImpl
import aikisib.slider.SliderConfig
import aikisib.slider.SliderRevolutionScraper
import aikisib.slider.SliderRevolutionScraperImpl
import aikisib.url.UrlCanonicolizer
import aikisib.url.UrlCanonicolizerImpl
import aikisib.url.UrlRelativizer
import aikisib.url.UrlRelativizerImpl
import aikisib.url.UrlTransformer
import aikisib.url.UrlTransformerImpl
import mu.KLoggable
import org.aeonbits.owner.Config
import org.aeonbits.owner.ConfigFactory
import java.io.File
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import java.net.URI
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.system.exitProcess

suspend fun main() {
    setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val mainConfig = createConfig(MainConfig::class)
    val vault = createConfig(Vault::class)

//    exportSliderRevolutionModules()
    mirrorSite(mainConfig, vault)
}

suspend fun mirrorSite(mainConfig: MainConfig, vault: Vault) {
    // Инжекция зависимостей для бедных.
    val downloader: Downloader = DownloaderImpl()
    val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl
    val rootUri = canonicolizer.canonicalize(URI("."), mainConfig.publicUrl().toString())
    val relativizer: UrlRelativizer = UrlRelativizerImpl
    val transformer: UrlTransformer = UrlTransformerImpl
    val adminUri = canonicolizer.canonicalize(mainConfig.publicUrl().toURI(), vault.wordpressLoginPath())
    val linkExtractor: LinkExtractor = LinkExtractorImpl(
        uriCanonicolizer = canonicolizer,
        forbiddenPrefixes = setOf(adminUri.toString()),
    )
    val fromLinkFilter: FromLinkFilter = FromLinkFilterImpl(rootUri)
    val contentTransformerFactory: ContentTransformerFactory = ContentTransformerFactoryImpl
    val recursiveScraper: RecursiveScraper = RecursiveScraperImpl(
        fromRoot = rootUri,
        toRoot = Path("/tmp/stockDir"),
        downloader = downloader,
        relativizer = relativizer,
        urlTransformer = transformer,
        linkExtractor = linkExtractor,
        fromLinkFilter = fromLinkFilter,
        contentTransformerFactory = contentTransformerFactory,
    )
    // закончили инжектировать зависимости.

    recursiveScraper.mirror()
}

@Suppress("UnusedPrivateMember")
private suspend fun exportSliderRevolutionModules(sliderConfig: SliderConfig) {
    val vault: Vault = createConfig(Vault::class)
    val sliderRevolutionScraper: SliderRevolutionScraper = SliderRevolutionScraperImpl(sliderConfig.adminUrl())

    val success = sliderRevolutionScraper.loginIntoWordpress(
        wordpressLoginPath = vault.wordpressLoginPath(),
        username = vault.username(),
        password = vault.password(),
    )

    if (!success) {
        println("Не удалось залогиниться в Вордпресс")
        exitProcess(1)
    }

    sliderConfig.sliderIds().forEach {
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

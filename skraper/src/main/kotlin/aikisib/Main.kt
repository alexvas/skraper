package aikisib

import aikisib.mirror.Athropos
import aikisib.mirror.AthroposImpl
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
import aikisib.mirror.SitemapGenerator
import aikisib.mirror.SitemapGeneratorImpl
import aikisib.mirror.WebpEncoder
import aikisib.mirror.WebpEncoderImpl
import aikisib.slider.SliderRevolutionScraper
import aikisib.slider.SliderRevolutionScraperImpl
import aikisib.url.UrlCanonicalizer
import aikisib.url.UrlCanonicalizerImpl
import aikisib.url.UrlRelativizer
import aikisib.url.UrlRelativizerImpl
import aikisib.url.UrlStandardizer
import aikisib.url.UrlStandardizerImpl
import com.typesafe.config.ConfigFactory
import io.ktor.http.ContentType
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import mu.KLoggable
import java.io.File
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import java.net.URI
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.system.exitProcess

suspend fun main() {
    setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val config = ConfigFactory.load("app.hocon.conf")
    val appConfig: AppConfig = Hocon.decodeFromConfig(config)

//    exportSliderRevolutionModules()
    mirrorSite(appConfig)
}

@Suppress("LongMethod")
suspend fun mirrorSite(appConfig: AppConfig) {
    val mainConfig = appConfig.main
    val mirrorDir = mainConfig.mirrorDir
    if (mirrorDir.exists()) {
        mirrorDir.deleteRecursively()
    }
    val mirrorPath = mirrorDir.toPath()
    mirrorPath.deleteIfExists()
    mirrorPath.createDirectories()
    val toRoot = mirrorPath.resolve(mainConfig.rootMain.host.toString().replace('.', '_'))
    toRoot.createDirectories()
    val toRootWebp = toRoot.resolve("webp")
    toRootWebp.createDirectories()
    val tempPath = mainConfig.tempDir.toPath()
    tempPath.createDirectories()

    // Инжекция зависимостей для бедных.
    val downloader: Downloader = DownloaderImpl(
        tempPath = tempPath,
        ignoredContentTypes = mainConfig.ignoredContentTypes.map { ContentType.parse(it) }.toSet(),
    )
    val standardizer: UrlStandardizer = UrlStandardizerImpl
    val rootMain = standardizer.standardize(URI("."), mainConfig.rootMain.toString())
    val rootAliases: List<URI> = mainConfig.rootAliases.map { standardizer.standardize(URI("."), it.toString()) }
    val canonicalHref = standardizer.standardize(URI("."), mainConfig.canonicalHref.toString())
    val relativizer: UrlRelativizer = UrlRelativizerImpl
    val vault = appConfig.vault
    val ignoredPrefixes = (mainConfig.ignoredPrefixes + vault.wordpressLoginPath)
        .map { standardizer.standardize(rootMain, it.trim()).toString() }
        .toSet()
    val ignoredSuffixes = mainConfig.ignoredSuffixes
        .map { standardizer.standardize(rootMain, it.trim()).toString() }
        .toSet()

    val linkExtractor: LinkExtractor = LinkExtractorImpl(
        urlStandardizer = standardizer,
        ignoredPrefixes = ignoredPrefixes,
        ignoredSuffixes = ignoredSuffixes,
    )
    val fromLinkFilter: FromLinkFilter = FromLinkFilterImpl(rootMain, rootAliases)
    val canonicalizer: UrlCanonicalizer = UrlCanonicalizerImpl
    val contentTransformerFactory: ContentTransformerFactory = ContentTransformerFactoryImpl(
        toRoot, rootMain, canonicalHref, canonicalizer,
    )
    val webpEncoder: WebpEncoder = WebpEncoderImpl(mainConfig.cwebpExecutable)
    val athropos: Athropos = AthroposImpl
    val sitemapGenerator: SitemapGenerator = SitemapGeneratorImpl(
        canonicalHref = canonicalHref,
        targerRootPath = toRoot,
    )
    val recursiveScraper: RecursiveScraper = RecursiveScraperImpl(
        fromRoot = rootMain,
        fromAliases = rootAliases,
        toRoot = toRoot,
        toRootWebp = toRootWebp,
        downloader = downloader,
        relativizer = relativizer,
        linkExtractor = linkExtractor,
        fromLinkFilter = fromLinkFilter,
        contentTransformerFactory = contentTransformerFactory,
        webpEncoder = webpEncoder,
        athropos = athropos,
        sitemapGenerator = sitemapGenerator,
    )
    // закончили инжектировать зависимости.

    recursiveScraper.mirror()
}

@Suppress("UnusedPrivateMember")
private suspend fun exportSliderRevolutionModules(appConfig: AppConfig) {
    val sliderRevolutionScraper: SliderRevolutionScraper = SliderRevolutionScraperImpl(appConfig.slider.adminUrl)

    val vault = appConfig.vault
    val success = sliderRevolutionScraper.loginIntoWordpress(
        wordpressLoginPath = vault.wordpressLoginPath,
        username = vault.username,
        password = vault.password,
    )

    if (!success) {
        println("Не удалось залогиниться в Вордпресс")
        exitProcess(1)
    }

    appConfig.slider.sliderIds.forEach {
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

/**
 * Last resort exception handler
 */
internal class DefaultUncaughtExceptionHandler : Thread.UncaughtExceptionHandler, KLoggable {
    override val logger = logger()

    override fun uncaughtException(t: Thread, e: Throwable) =
        logger.error(e) { "Unhandled exception" }
}

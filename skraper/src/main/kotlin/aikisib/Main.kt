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
import aikisib.slider.SliderConfig
import aikisib.slider.SliderRevolutionScraper
import aikisib.slider.SliderRevolutionScraperImpl
import aikisib.url.UrlCanonicolizer
import aikisib.url.UrlCanonicolizerImpl
import aikisib.url.UrlRelativizer
import aikisib.url.UrlRelativizerImpl
import aikisib.url.UrlTransformer
import aikisib.url.UrlTransformerImpl
import io.ktor.http.ContentType
import mu.KLoggable
import org.aeonbits.owner.Config
import org.aeonbits.owner.ConfigFactory
import java.io.File
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import java.net.URI
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.reflect.KClass
import kotlin.system.exitProcess

suspend fun main() {
    setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler())
    val mainConfig = createConfig(MainConfig::class)
    val vault = createConfig(Vault::class)

//    exportSliderRevolutionModules()
    mirrorSite(mainConfig, vault)
}

@Suppress("LongMethod")
suspend fun mirrorSite(mainConfig: MainConfig, vault: Vault) {
    val mirrorDir = mainConfig.mirrorDir()
    if (mirrorDir.exists()) {
        mirrorDir.deleteRecursively()
    }
    val mirrorPath = mirrorDir.toPath()
    mirrorPath.deleteIfExists()
    mirrorPath.createDirectories()
    val toRoot = mirrorPath.resolve(mainConfig.rootMain().host.toString().replace('.', '_'))
    toRoot.createDirectories()
    val toRootWebp = toRoot.resolve("webp")
    toRootWebp.createDirectories()
    val tempPath = mainConfig.tempDir().toPath()
    tempPath.createDirectories()

    // Инжекция зависимостей для бедных.
    val downloader: Downloader = DownloaderImpl(
        tempPath = tempPath,
        ignoredContentTypes = mainConfig.ignoredContentTypes().map { ContentType.parse(it) }.toSet(),
    )
    val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl
    val rootMain = canonicolizer.canonicalize(URI("."), mainConfig.rootMain().toString())
    val rootAliases: List<URI> = mainConfig.rootAliases().map { canonicolizer.canonicalize(URI("."), it.toString()) }
    val canonicalHref = canonicolizer.canonicalize(URI("."), mainConfig.canonicalHref().toString())
    val relativizer: UrlRelativizer = UrlRelativizerImpl
    val transformer: UrlTransformer = UrlTransformerImpl
    val ignoredPrefixes = (mainConfig.ignoredPrefixes() + vault.wordpressLoginPath())
        .map { canonicolizer.canonicalize(rootMain, it.trim()).toString() }
        .toSet()
    val ignoredSuffixes = mainConfig.ignoredSuffixes()
        .map { canonicolizer.canonicalize(rootMain, it.trim()).toString() }
        .toSet()

    val linkExtractor: LinkExtractor = LinkExtractorImpl(
        uriCanonicolizer = canonicolizer,
        ignoredPrefixes = ignoredPrefixes,
        ignoredSuffixes = ignoredSuffixes,
    )
    val fromLinkFilter: FromLinkFilter = FromLinkFilterImpl(rootMain, rootAliases)
    val contentTransformerFactory: ContentTransformerFactory = ContentTransformerFactoryImpl(rootMain, canonicalHref)
    val webpEncoder: WebpEncoder = WebpEncoderImpl(mainConfig.cwebpExecutable())
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
        urlTransformer = transformer,
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

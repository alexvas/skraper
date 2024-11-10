package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.LocalResource
import aikisib.url.UrlRelativizer
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KLogging
import java.net.URI
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.setLastModifiedTime

interface RecursiveScraper {

    /**
     * Рекурсивно обойти сайт и сохранить в виде статических файлов.
     * Эквивалент `wget -mEpnp <нужный сайт>`
     */
    suspend fun mirror()
}

@Suppress("LongParameterList", "TooManyFunctions")
internal class RecursiveScraperImpl(
    /**
     * Корневой адрес динамического сайта-источника.
     */
    private val fromRoot: URI,

    /**
     * Корневой адрес псевдонимов динамического сайта-источника.
     */
    private val fromAliases: List<URI>,

    /**
     * Корневая директория, куда складываем статические странички.
     */
    private val toRoot: Path,

    /**
     * Корневая директория, куда складываем трансформированные в webp изображения.
     */
    private val toRootWebp: Path,

    private val downloader: Downloader,
    private val relativizer: UrlRelativizer,
    private val linkExtractor: LinkExtractor,
    private val fromLinkFilter: FromLinkFilter,
    private val contentTransformerFactory: ContentTransformerFactory,
    private val webpEncoder: WebpEncoder,
    private val athropos: Athropos,
    private val sitemapGenerator: SitemapGenerator,
) : RecursiveScraper {
    private val linkRepo: MutableMap<URI, Unit> = ConcurrentHashMap()

    private val transformCache: MutableMap<OriginalDescription, LocalResource> = ConcurrentHashMap()
    private val descriptionRepo: MutableMap<URI, OriginalDescription> = ConcurrentHashMap()
    private val fanOutRepo: MutableMap<OriginalDescription, Map<String, URI>> = ConcurrentHashMap()

    override suspend fun mirror() {
        logger.info { "Скачиваем копию сайта локально на диск $toRoot" }
        coroutineScope {
            downloadItem(this, fromRoot)
        }
        logger.info { "Трансформируем содержимое: меняем ссылки на локальные." }
        moveContentTransforming()
        logger.info { "Генерируем sitemap.xml и robots.txt" }
        sitemapGenerator.generate(descriptionRepo.values.asSequence())
        logger.info { "Генерируем копии изображений в формате WebP." }
        makeWebPCopy(toRootWebp)
    }

    private suspend fun downloadItem(scope: CoroutineScope, from: URI) {
        scope.launch(Dispatchers.Default) {
            doDownloadItem(scope, from)
        }
    }

    private suspend fun doDownloadItem(scope: CoroutineScope, from: URI) {
        val deAliased: URI = from.deAlias()
        val originalDescription = downloader.download(deAliased) ?: return
        descriptionRepo[originalDescription.remoteUri.norm()] = originalDescription
        transformCache[originalDescription] = transform(originalDescription)
        val links: Map<String, URI> = linkExtractor.extractLinks(originalDescription)
        val filteredLinks: Map<String, URI> = links.mapNotNull { fromLinkFilter.filter(it.key, it.value) }.toMap()
        if (filteredLinks.isEmpty()) {
            // страничка не требует трансформации, достаточно просто её скопировать.
            copyFile(originalDescription)
            return
        }
        fanOutRepo[originalDescription] = filteredLinks

        for ((_, uri) in filteredLinks) {
            val prev = linkRepo.put(uri.norm(), Unit)
            val notSeen = prev == null
            if (notSeen) {
                downloadItem(scope, uri)
            }
        }
    }

    private fun copyFile(originalDescription: OriginalDescription) {
        val target = toRoot.resolve(resolveResource(originalDescription).target)
        target.parent.createDirectories()
        originalDescription.localPath.copyTo(target, overwrite = true)
        val lastModified = originalDescription.lastModified ?: return
        target.setLastModifiedTime(FileTime.fromMillis(lastModified.time))
    }

    private fun resolveResource(originalDescription: OriginalDescription) =
        transformCache[originalDescription]
            ?: error("трансформация для $originalDescription не закэширована.")

    private fun transform(originalDescription: OriginalDescription): LocalResource {
        val source = originalDescription.remoteUri
        val type = originalDescription.type
        return when (type) {
            ContentType.Text.Html -> LocalResource.fromHtmlPage(source)
            else -> LocalResource.fromEtc(source, originalDescription.type)
        }
    }

    private suspend fun moveContentTransforming() {
        coroutineScope {
            for ((originalDescription, filteredLinks: Map<String, URI>) in fanOutRepo) {
                val itemResource = transformCache[originalDescription]
                    ?: error("трансформация для $originalDescription не закэширована.")

                launch(Dispatchers.Default) {
                    transformingMoveFileInPlace(
                        originalDescription = originalDescription,
                        relativeLinks = buildRelativeLinks(filteredLinks, itemResource),
                        resolveResource = resolveResource(originalDescription),
                    )
                }
            }
        }
    }

    private fun buildRelativeLinks(filteredLinks: Map<String, URI>, itemResource: LocalResource) =
        filteredLinks.asSequence()
            .map { (link, uri) ->
                val targetDescription = descriptionRepo[uri.norm()] ?: return@map null
                val targetResource = transformCache[targetDescription]
                    ?: error("целевая трансформация для $targetDescription не закэширована.")
                val relative = relativizer.maybeRelativize(
                    itemResource.contentType,
                    itemResource.reference,
                    targetResource.reference,
                    uri.rawFragment
                ) ?: return@map null
                link to relative
            }
            .filterNotNull()
            .toMap()

    private fun transformingMoveFileInPlace(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        resolveResource: LocalResource,
    ) =
        contentTransformerFactory.create(originalDescription, relativeLinks, resolveResource)
            .transformingMoveFileInPlace()

    /**
     * Приводим path и query к нижнему регистру и зануляем фрагмент.
     */
    private fun URI.norm() =
        URI(
            scheme,
            userInfo,
            host,
            port,
            path?.lowercase(),
            query?.lowercase(),
            /* зануляем фрагмент */
            null,
        )

    private suspend fun makeWebPCopy(toRoot: Path) {
        coroutineScope {
            for ((originalDescription, transformed) in transformCache) {
                if (originalDescription.type !in IMAGES_TO_BE_CONVERTED) {
                    continue
                }
                val targetPath = toRoot.resolve(transformed.target)
                targetPath.parent.createDirectories()
                val targetPathWithExtension = targetPath.parent.resolve(targetPath.name + ".webp")
                launch(Dispatchers.Default) {
                    webpEncoder.encode(originalDescription, targetPathWithExtension)
                    athropos.removeIfLarger(originalDescription.localPath, targetPathWithExtension)
                }
            }
        }
    }

    @Suppress("ComplexCondition")
    private fun URI.deAlias(): URI {
        fromAliases.forEach { alias ->
            // различия в схеме игнорируем
            if (host == alias.host && port == alias.port && userInfo == alias.userInfo) {
                // подменяем псевдоним на основной URI
                return sameOrigin(fromRoot)
            }
        }
        return this
    }

    companion object : KLogging() {
        private val IMAGES_TO_BE_CONVERTED = listOf(
            ContentType.Image.JPEG,
            ContentType.Image.PNG,
        )
    }
}

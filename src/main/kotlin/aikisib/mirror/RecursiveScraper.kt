package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.UrlRelativizer
import aikisib.url.UrlTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KLogging
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.moveTo

interface RecursiveScraper {

    /**
     * Рекурсивно обойти сайт и сохранить в виде статических файлов.
     * Эквивалент `wget -mEpnp <нужный сайт>`
     */
    suspend fun mirror()
}

@Suppress("LongParameterList")
internal class RecursiveScraperImpl(
    private val fromRoot: URI,
    private val toRoot: Path,
    private val downloader: Downloader,
    private val relativizer: UrlRelativizer,
    private val urlTransformer: UrlTransformer,
    private val linkExtractor: LinkExtractor,
    private val fromLinkFilter: FromLinkFilter,
    private val contentTransformerFactory: ContentTransformerFactory,
) : RecursiveScraper {
    private val linkRepo: MutableMap<URI, Unit> = ConcurrentHashMap()

    private val transformCache: MutableMap<OriginalDescription, URI> = ConcurrentHashMap()
    private val descriptionRepo: MutableMap<URI, OriginalDescription> = ConcurrentHashMap()
    private val fanOutRepo: MutableMap<OriginalDescription, Map<String, URI>> = ConcurrentHashMap()

    override suspend fun mirror() {
        coroutineScope {
            downloadItem(this, fromRoot)
        }
        moveContentTransforming()
    }

    private suspend fun downloadItem(scope: CoroutineScope, from: URI) {
        scope.launch(Dispatchers.Default) {
            doDownloadItem(scope, from)
        }
    }

    private suspend fun doDownloadItem(scope: CoroutineScope, from: URI) {
        val originalDescription = downloader.download(from) ?: return
        descriptionRepo[originalDescription.remoteUri] = originalDescription
        transformCache[originalDescription] = transform(originalDescription)
        val links = linkExtractor.extractLinks(originalDescription)
        val filteredLinks = links.filter { fromLinkFilter.filter(it.value) }
        if (filteredLinks.isEmpty()) {
            moveFileInPlace(originalDescription)
            return
        }
        fanOutRepo[originalDescription] = filteredLinks

        for ((_, uri) in filteredLinks) {
            val prev = linkRepo.put(uri, Unit)
            val notSeen = prev == null
            if (notSeen)
                downloadItem(scope, uri)
        }
    }

    private fun moveFileInPlace(originalDescription: OriginalDescription) {
        val target = resolveTarget(originalDescription)
        target.parent.createDirectories()
        originalDescription.localPath.moveTo(target, overwrite = true)
    }

    private fun resolveTarget(originalDescription: OriginalDescription): Path {
        val transformed = transformCache[originalDescription] ?: error("трансформация для $originalDescription не закэширована.")
        val relative = relativizer.relativize(fromRoot, transformed)
        return toRoot.resolve(relative.toString())
    }

    private fun transform(originalDescription: OriginalDescription) =
        urlTransformer.transform(originalDescription.type, originalDescription.remoteUri)

    private fun moveContentTransforming() {
        for ((originalDescription, filteredLinks: Map<String, URI>) in fanOutRepo) {
            val transformedUri = transformCache[originalDescription] ?: error("трансформация для $originalDescription не закэширована.")

            transformingMoveFileInPlace(
                originalDescription = originalDescription,
                relativeLinks = filteredLinks.mapValues { (_, uri) ->
                    val targetDescription = descriptionRepo[uri] ?: return@mapValues uri
                    val transformedLink = transformCache[targetDescription] ?: error("целевая трансформация для $targetDescription не закэширована.")
                    relativizer.relativize(transformedUri, transformedLink)
                },
                resolveTarget = resolveTarget(originalDescription),
            )
        }
    }

    private fun transformingMoveFileInPlace(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        resolveTarget: Path,
    ) =
        contentTransformerFactory.create(originalDescription, relativeLinks, resolveTarget)
            .transformingMoveFileInPlace()

    companion object : KLogging()
}
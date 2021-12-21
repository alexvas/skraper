package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.UrlRelativizer
import aikisib.url.UrlTransformer
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

    override suspend fun mirror() {
        downloadItem(fromRoot)
    }

    private suspend fun downloadItem(from: URI) {
        coroutineScope {
            launch(Dispatchers.Default) {
                doDownloadItem(from)
            }
        }
    }

    private suspend fun doDownloadItem(from: URI) {
        val originalDescription = downloader.download(from) ?: return
        val links = linkExtractor.extractLinks(originalDescription)
        val filteredLinks = links.filter { fromLinkFilter.filter(it.value) }
        if (filteredLinks.isEmpty()) {
            moveFileInPlace(originalDescription)
            return
        }

        for ((_, uri) in filteredLinks) {
            val prev = linkRepo.put(uri, Unit)
            val notSeen = prev == null
            if (notSeen)
                downloadItem(uri)
        }

        transformingMoveFileInPlace(
            originalDescription = originalDescription,
            relativeLinks = filteredLinks.mapValues { (_, uri) ->
                relativizer.relativize(from, uri)
            },
            resolveTarget = resolveTarget(originalDescription),
        )
    }

    private fun moveFileInPlace(originalDescription: OriginalDescription) {
        val target = resolveTarget(originalDescription)
        target.parent.createDirectories()
        originalDescription.localPath.moveTo(target, overwrite = true)
    }

    private fun resolveTarget(originalDescription: OriginalDescription): Path {
        val transformed = urlTransformer.transform(originalDescription.type, originalDescription.remoteUri)
        val relative = relativizer.relativize(fromRoot, transformed)
        return toRoot.resolve(relative.toString())
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

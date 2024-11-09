package aikisib.mirror

import aikisib.model.OriginalDescription
import aikisib.url.LocalResource
import io.ktor.http.ContentType
import mu.KLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText

interface ContentTransformer {
    fun transformingMoveFileInPlace()
}

interface ContentTransformerFactory {
    fun create(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        target: LocalResource,
    ): ContentTransformer
}

internal class ContentTransformerFactoryImpl(
    private val toRoot: Path,
    private val rootMain: URI,
    private val canonicalHref: URI,
) : ContentTransformerFactory {
    override fun create(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        target: LocalResource,
    ): ContentTransformer =
        ContentTransformerImpl(toRoot, rootMain, originalDescription, relativeLinks, target, canonicalHref)
}

private class ContentTransformerImpl(
    private val toRoot: Path,
    rootMain: URI,
    private val originalDescription: OriginalDescription,
    private val relativeLinks: Map<String, URI>,
    private val item: LocalResource,
    private val canonicalHref: URI,
) : ContentTransformer {

    private val rootMainStr = rootMain.toString().removeSuffix("/")
    private val ajaxEscapedRootMain = rootMainStr.replace("/", "\\/")

    override fun transformingMoveFileInPlace() {
        var content = originalDescription.localPath.readText()
        for ((fromLink, toLink) in relativeLinks) {
            content = when (originalDescription.type) {
                ContentType.Text.CSS -> replaceCss(content, fromLink, toLink)
                ContentType.Text.Html -> {
                    val cssReplaced = replaceCss(content, fromLink, toLink)
                    replaceEtc(cssReplaced, fromLink, toLink)
                }
                else -> replaceEtc(content, fromLink, toLink)
            }
        }
        when (originalDescription.type) {
            ContentType.Text.Html -> {
                content = fixDom(content, relativeLinks)
                content = content
                    .replace(ajaxEscapedRootMain, "")
                    .replace(rootMainStr, "")
                content = fixCanonicalLink(content, item.reference)
            }
            else -> {}
        }
        val target = toRoot.resolve(item.target)
        target.parent.createDirectories()
        try {
            target.writeText(content, options = arrayOf(StandardOpenOption.CREATE))
            val lastModified = originalDescription.lastModified ?: return
            target.setLastModifiedTime(FileTime.fromMillis(lastModified.time))
        } catch (e: FileSystemException) {
            logger.warn { "не удалось переместить ресурс $item: ${e.message}" }
        }
    }

    private fun fixDom(input: String, relativeLinks: Map<String, URI>): String {
        val doc: Document = Jsoup.parse(input)
        doc.getElementsByTag("link")
            .asSequence()
            .filter {
                it.attr("rel") in listOf("shortlink", "alternate", "pingback", "EditURI")
            }
            .forEach { it.remove() }
        doc.getElementsByTag("form")
            .asSequence()
            .filter { it.attr("name") == "login_form" }
            .forEach { it.remove() }
        doc.getElementsByTag("div")
            .asSequence()
            .filter { it.attr("class") == "login" }
            .forEach { it.remove() }
        doc.getElementsByTag("img")
            .asSequence()
            .filter {
                val srcset = it.attr("srcset")
                !srcset.isNullOrBlank()
            }
            .forEach { img ->
                val srcset = img.attr("srcset")
                val result = srcset.splitToSequence(',')
                    .map { chunk -> replaceSrcSetChunkLinks(img, chunk.trim(), relativeLinks) }
                    .joinToString(separator = ", ")
                img.attr("srcset", result)
            }

        return doc.toString()
    }

    private fun fixCanonicalLink(input: String, remoteUri: URI): String {
        val doc: Document = Jsoup.parse(input)
        val canonical = doc.getElementsByTag("link")
            .firstOrNull { it.attr("rel") == "canonical" }
        canonical?.attr("href", remoteUri.sameOrigin(canonicalHref).toString())
        return doc.toString()
    }

    private fun replaceSrcSetChunkLinks(
        img: Element,
        srcSetChunk: String,
        relativeLinks: Map<String, URI>,
    ): String {
        val chunks = srcSetChunk.split(' ')
        val currentLink = chunks[0]
        val marker = chunks[1]
        var target: URI? = null
        for ((sourceLink, targetUri) in relativeLinks) {
            if (sourceLink == currentLink) {
                target = targetUri
                break
            }
        }
        check(target != null) { "не найдена ссылка на замену $currentLink в srcset для $img" }
        return "$target $marker"
    }

    private fun replaceCss(content: String, fromLink: String, toLink: URI): String = content.replace(
        "url('$fromLink')",
        "url('$toLink')",
    ).replace(
        """url("$fromLink")""",
        """url("$toLink")""",
    ).replace(
        """url($fromLink)""",
        """url($toLink)""",
    )

    private fun replaceEtc(content: String, fromLink: String, toLink: URI): String {
        return content.replace(
            "'$fromLink'",
            "'$toLink'",
        ).replace(
            """"$fromLink"""",
            """"$toLink"""",
        )
    }

    companion object : KLogging()
}

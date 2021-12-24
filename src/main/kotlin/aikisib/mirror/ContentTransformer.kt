package aikisib.mirror

import aikisib.model.OriginalDescription
import io.ktor.http.ContentType
import mu.KLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

interface ContentTransformer {
    fun transformingMoveFileInPlace()
}

interface ContentTransformerFactory {
    fun create(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        target: Path,
    ): ContentTransformer
}

internal object ContentTransformerFactoryImpl : ContentTransformerFactory {
    override fun create(
        originalDescription: OriginalDescription,
        relativeLinks: Map<String, URI>,
        target: Path,
    ): ContentTransformer =
        ContentTransformerImpl(originalDescription, relativeLinks, target)
}

private class ContentTransformerImpl(
    private val originalDescription: OriginalDescription,
    private val relativeLinks: Map<String, URI>,
    private val target: Path,
) : ContentTransformer {

    override fun transformingMoveFileInPlace() {
        var content = originalDescription.localPath.readText()
        for ((fromLink, toLink) in relativeLinks) {
            content = when (originalDescription.type) {
                ContentType.Text.CSS -> replaceCss(content, fromLink, toLink)
                ContentType.Text.Html -> {
                    val cssReplaced = replaceCss(content, fromLink, toLink)
                    val etcReplaced = replaceEtc(cssReplaced, fromLink, toLink)
                    fixDom(etcReplaced)
                }
                else -> replaceEtc(content, fromLink, toLink)
            }
        }
        target.parent.createDirectories()
        try {
            target.writeText(content, options = arrayOf(StandardOpenOption.CREATE))
        } catch (e: FileSystemException) {
            logger.warn { "не удалось переместить файл $target: ${e.message}" }
        }
    }

    private fun fixDom(input: String): String {
        val doc: Document = Jsoup.parse(input)
        doc.getElementsByTag("link")
            .asSequence()
            .filter {
                it.attr("rel") in listOf("canonical", "shortlink", "alternate", "pingback", "EditURI")
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

        return doc.toString()
    }

    private fun replaceCss(content: String, fromLink: String, toLink: URI): String {
        return content.replace(
            "url('$fromLink')",
            "url('$toLink')",
        ).replace(
            """url("$fromLink")""",
            """url("$toLink")""",
        ).replace(
            """url($fromLink)""",
            """url($toLink)""",
        )
    }

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

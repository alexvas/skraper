package aikisib.mirror

import aikisib.model.OriginalDescription
import mu.KLogging
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
            content = content.replace(fromLink, toLink.toString())
        }
        target.parent.createDirectories()
        try {
            target.writeText(content, options = arrayOf(StandardOpenOption.CREATE))
        } catch (e: FileSystemException) {
            logger.warn { "не удалось переместить файл $target: ${e.message}" }
        }
    }

    companion object : KLogging()
}

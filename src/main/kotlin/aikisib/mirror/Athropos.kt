package aikisib.mirror

import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.fileSize

interface Athropos {
    fun removeIfLarger(sourcePath: Path, targetPath: Path)
}

internal object AthroposImpl : Athropos {

    override fun removeIfLarger(sourcePath: Path, targetPath: Path) {
        val sourceSize = sourcePath.fileSize()
        val targetSize = targetPath.fileSize()
        if (targetSize >= sourceSize)
            targetPath.deleteExisting()
    }
}

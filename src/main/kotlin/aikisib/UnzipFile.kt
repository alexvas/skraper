package aikisib

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object UnzipFile {

    @Throws(IOException::class)
    fun unzip(destDir: File, bytes: ByteArray) {
        require(destDir.canonicalFile.isDirectory) { "destination '$destDir' not a directory!" }
        ZipInputStream(bytes.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = newFile(destDir, zipEntry)
                if (zipEntry.isDirectory) {
                    newFile.maybeCreateDir()
                } else {
                    // fix for Windows-created archives
                    newFile.parentFile.maybeCreateDir()

                    writeFileContent(newFile, zis)
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }
    }

    private fun File.maybeCreateDir() {
        if (!isDirectory && !mkdirs()) {
            throw IOException("Failed to create directory $this")
        }
    }

    private fun writeFileContent(newFile: File, zis: InputStream) {
        Files.copy(zis, newFile.toPath())
    }

    @Throws(IOException::class)
    fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }
}

package aikisib.mirror

import aikisib.model.OriginalDescription
import io.ktor.http.ContentType
import mu.KLogging
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

interface WebpEncoder {
    fun encode(originalDescription: OriginalDescription, targetPath: Path): Boolean
}

class WebpEncoderImpl(
    private val cwebpExecutablePath: String,
) : WebpEncoder {

    override fun encode(originalDescription: OriginalDescription, targetPath: Path): Boolean {
        val args: List<String> = listOf(cwebpExecutablePath, "-quiet", "-m", "6", "-mt") +
            when (originalDescription.type) {
                ContentType.Image.PNG -> listOf("-near_lossless", "95")
                else -> listOf()
            } +
            listOf(originalDescription.localPath.toString(), "-o", targetPath.toString())

        return runCommand(arguments = args.toTypedArray())
    }

    @Suppress("ReturnCount")
    private fun runCommand(
        workingDir: File = File("."),
        timeoutAmount: Long = 60,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS,
        vararg arguments: String,
    ): Boolean {
        val processWrapper = runCatching {
            ProcessBuilder(*arguments)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
        }
            .onFailure {
                logger.error(it) { "Не удалось запустить ${arguments.asList()}" }
            }
        if (processWrapper.isFailure) {
            return false
        }
        val process = processWrapper.getOrThrow()
        if (process.exitValue() != 0) {
            logger.error { "Ошибка запуска ${arguments.asList()}:" + process.errorStream.bufferedReader().readText() }
            return false
        }
        return true
    }

    companion object : KLogging()
}

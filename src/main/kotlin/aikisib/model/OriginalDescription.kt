package aikisib.model

import io.ktor.http.ContentType
import java.net.URI
import java.nio.file.Path

/**
 * Описание скачанного локально ресурса,
 * содержимое которого ещё не модифицировалось.
 */
data class OriginalDescription(

    /**
     * Ссылка, которая указывала на ресурс.
     */
    val remoteUri: URI,

    /**
     * Тип ресурса.
     */
    val type: ContentType?,

    /**
     * Размер ресурса в килобайтах.
     */
    val size: Int,

    /**
     * Путь ко временному файлу, куда сохранили содержимое.
     */
    val localPath: Path,
)

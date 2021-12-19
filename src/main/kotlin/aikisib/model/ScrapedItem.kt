package aikisib.model

import java.io.File

data class ScrapedItem(
    /**
     * Описание ресурса.
     */
    val description: ResourceDescription,

    /**
     * Содержимое ресурса,
     * сохранённое локально, в файле.
     */
    val content: File,
)

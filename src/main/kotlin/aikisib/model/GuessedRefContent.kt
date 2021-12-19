package aikisib.model

/**
 * Угаданное содержимое ссылки.
 */
data class GuessedRefContent(

    /**
     * Тип ресурса по ссылке.
     */
    val type: ResourceType,

    /**
     * Надо ли скачивать содержимое ресурса,
     * чтобы искать внутри ссылки.
     */
    val needFollow: Boolean,
)

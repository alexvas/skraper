package aikisib.model

import java.net.URI

/**
 * Описание того или иного ресурса,
 * подлежащего зеркалированию или нет.
 */
data class ResourceDescription(

    /**
     * Откуда сослались на объект.
     */
    val parent: URI,

    /**
     * Ссылка, которая указывала на ресурс.
     */
    val originalUrl: String,

    /**
     * Тип ресурса.
     */
    val type: ResourceType,

    /**
     * Ссылка на ресурс в канонической форме.
     */
    val canonicalUri: URI,
)

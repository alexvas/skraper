package aikisib.model

enum class ResourceType(
    /**
     * Может ли ресурс содержать в себе ссылки на внешние ресурсы.
     */
    val mayContainRefs: Boolean,

    /**
     * Известные для данного типа ресурса расширения.
     */
    internal vararg val extensions: String,
) {

    /**
     * Собственно страничка с HTML-содержимым.
     */
    PAGE(
        mayContainRefs = true,
        "html",
        "php",
    ),

    /**
     * Стиль странички.
     */
    CSS(
        mayContainRefs = true,
        "css",
    ),

    /**
     * Векторное изображение.
     * Для простоты считаем, что векторное изображение не может ссылаться на другие ресурсы.
     */
    VECTOR_IMAGE(
        mayContainRefs = false,
        "svg",
    ),

    /**
     * Растровое изображение.
     */
    BITMAP_IMAGE(
        mayContainRefs = false,
        "webp",
        "png",
        "jpeg",
        "jpg",
        "gif",
        "bmp",
    ),

    /**
     * Видео
     */
    VIDEO(
        mayContainRefs = false,
        "avi",
        "mpeg",
        "mpeg4",
        "webm",
    ),

    /**
     * Шрифт
     */
    FONT(
        mayContainRefs = false,
        "font",
    ),

    /**
     * Javascript
     */
    JS(
        mayContainRefs = true,
        "js",
    );

    private val prefixedExtensions: List<String>
        get() = extensions.map { ".$it" }

    private fun hasExtension(input: String): Boolean {
        val lowerInput = input.lowercase()
        return prefixedExtensions.any { lowerInput.endsWith(it) }
    }

    companion object {

        /**
         * Находим тип ресурса по пути к нему:
         * либо имени файла, либо имени файла вместе с путём.
         */
        fun findOrNull(path: String): ResourceType? =
            values().firstOrNull { it.hasExtension(path) }
    }
}

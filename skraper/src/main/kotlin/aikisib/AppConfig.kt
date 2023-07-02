@file:UseSerializers(URLSerializer::class, FileByNameSerializer::class)

package aikisib

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.net.URL

@Serializable
data class AppConfig(
    val main: MainConfig,
    val slider: SliderConfig,
    val vault: Vault,
)

@Serializable
data class MainConfig(

    /**
     * Корневой URL публичного сайта, который надо отзеркалить.
     */
    val rootMain: URL,

    /**
     * Альтернативные псевдонимы сайта, куда могут ссылаться ресурсы.
     */
    val rootAliases: List<URL>,

    /**
     * Целевой URL, который применяется для
     * ```
     * <link rel="canonical" href="bla-bla-bla">,
     * ```
     * где href должен быть абсолютной ссылкой.
     */
    val canonicalHref: URL,

    /**
     * Ненужные для зеркалирования префиксы.
     */
    val ignoredPrefixes: List<String>,

    /**
     * Ненужные для зеркалирования суффиксы.
     */
    val ignoredSuffixes: List<String>,

    /**
     * Ненужные для зеркалирования типы содержимого.
     */
    val ignoredContentTypes: List<String>,

    /**
     * Путь к утилите cwebp.
     */
    val cwebpExecutable: String,

    /**
     * Путь ко временной директории для скачиваемых файлов.
     */
    val tempDir: File,

    /**
     * Путь к целевой директории, куда сохранять зеркалируемые файлы.
     * Директория очищается вначале работы приложения.
     */
    val mirrorDir: File,
)

@Serializable
data class SliderConfig(

    /**
     * Корневой URL сайта, где доступна админка
     */
    val adminUrl: URL,

    /**
     * Идентификаторы слайдеров, которые хочется экспортировать.
     * Если идентификаторов несколько, перечисляем их через запятую.
     */
    val sliderIds: List<Int>,
)

@Serializable
data class Vault(
    /**
     * Путь к страничке входа в админку вордпресса.
     */
    val wordpressLoginPath: String,

    /**
     * Имя пользователя с правами администратора.
     */
    val username: String,

    /**
     * Пароль пользователя с правами администратора.
     */
    val password: String,
)

@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URL {
        return URL(decoder.decodeString())
    }
}

@Serializer(forClass = File::class)
object FileByNameSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("FileByName", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}

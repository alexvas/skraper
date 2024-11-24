@file:Suppress("NonAsciiCharacters", "ClassName")

package aikisib.url

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class UrlCanonicalizerTest {

    // Корневой URI для всех абсолютных
    private val root = URI.create("https://aikido.nsk.su")
    private val sut: UrlCanonicalizer = UrlCanonicalizerImpl

    @Test
    fun `корректно кодирует кириллическую ссылку`() {
        // given
        val input = URI("$root/сервисы/здоровье")

        // when
        val canonical = sut.encode(input)

        // then
        val expected = URI("$root/${"сервисы".urlEncode()}/${"здоровье".urlEncode()}")
        assertThat(canonical).isEqualTo(expected)
    }

    @Test
    fun `не меняет ASCII текста`() {
        // given
        val input = URI("$root/services/health")

        // when
        val canonical = sut.encode(input)

        // then
        assertThat(canonical).isEqualTo(input)
    }

    @Test
    fun `не меняет кодированный слэш`() {
        // given
        val input = URI("$root/some${SLASH}thing")

        // when
        val canonical = sut.encode(input)

        // then
        assertThat(canonical).isEqualTo(input)
    }

    @Test
    fun `корректно кодирует кириллицу вперемешку с ASCII`() {
        // given
        val input = URI("$root/some/я/thing")

        // when
        val canonical = sut.encode(input)

        // then
        val expected = URI("$root/some/${"я".urlEncode()}/thing")
        assertThat(canonical).isEqualTo(expected)
    }

    companion object {
        val SLASH = "/".urlEncode()
    }
}

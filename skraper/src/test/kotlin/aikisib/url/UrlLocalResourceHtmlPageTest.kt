@file:Suppress("NonAsciiCharacters", "ClassName", "LocalVariableName", "ObjectPropertyName")

package aikisib.url

import io.ktor.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path

class UrlLocalResourceHtmlPageTest {

    private val root = URI("https://aikisib.ru/")
    private val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl

    private fun transform(input: URI) =
        LocalResource.fromHtmlPage(input)

    @Nested
    inner class `корневой УРЛ` {

        @Test
        fun `корневой URI`() {
            // given
            val input = root
            // when, then
            whenThen(input)
        }

        @Test
        fun `просто слэш`() {
            // given
            val input = URI("/")
            // when, then
            whenThen(input)
        }

        @Test
        fun `сформированный УРЛ`() {
            // given
            val input = URI("/index.html")
            // when, then
            whenThen(input)
        }

        private fun whenThen(input: URI) {
            // when
            val result = transform(input)

            // then
            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = input,
                reference = URI("/"),
                target = Path("index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class ASCII {

        @Test
        fun `HTML страничка ложится в свою директорию`() {
            // given
            val input = canonicolizer.canonicalize(root, "/page.html")
            val expectedPath = "page"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `игнорируем index_dot_html`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i/index.html")
            val expectedPath = "i"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `ЧПУ ложится в свою директорию`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i")
            val expectedPath = "i"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = URI("$root?y")
            val expectedPath = "${_QUESTION}y".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/z?y")
            val expectedPath = "z${_QUESTION}y".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/z?y&w")
            val expectedPath = "z${_QUESTION}y${_AMPERSAND}w".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/z?y=t&w=r")
            val expectedPath = "z${_QUESTION}y=t${_AMPERSAND}w=r".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        private fun whenThen(input: URI, expectedPath: String) {
            val result = transform(input)

            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = input,
                reference = URI("/$expectedPath"),
                target = Path(expectedPath, "index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллической query` {

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = canonicolizer.canonicalize(URI("."), "$root?ы")
            val expectedPath = "${_QUESTION}ы".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы")
            val expectedPath = "i${_QUESTION}ы".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы&э")
            val expectedPath = "i${_QUESTION}ы${_AMPERSAND}э".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы=w&э=z")
            val expectedPath = "i${_QUESTION}ы=w${_AMPERSAND}э=z".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        private fun whenThen(input: URI, expectedPath: String) {
            val result = transform(input)

            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = input,
                reference = URI("/$expectedPath"),
                target = Path(expectedPath, "index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллическим путём` {

        @Test
        fun `HTML страничка ложится в свою директорию`() {
            // given
            val input = URI("${root}ы.html")
            val expectedPath = "ы"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `ЧПУ ложится в свою директорию`() {
            // given
            val input = URI("${root}ы")
            val expectedPath = "ы"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = URI("${root}ы?z")
            val expectedPath = "ы${_QUESTION}z".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = URI("${root}ы?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = URI("${root}ы?f=w&g=z")
            val expectedPath = "ы${_QUESTION}f=w${_AMPERSAND}g=z".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        private fun whenThen(input: URI, expectedPath: String) {
            val result = transform(input)

            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = input,
                reference = URI("/$expectedPath"),
                target = Path(expectedPath, "index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллическим кодированным путём` {

        @Test
        fun `кодированный ЧПУ ложится в свою директорию`() {
            // given
            val yi = "ы".urlEncode()
            val input = canonicolizer.canonicalize(root, "/ы")
            assertThat(input.rawPath).isEqualTo("/$yi")
            val expectedPath = "ы"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?z")
            val expectedPath = "ы${_QUESTION}z".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?f=z&g=w")
            val expectedPath = "ы${_QUESTION}f=z${_AMPERSAND}g=w".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        private fun whenThen(input: URI, expectedPath: String) {
            val result = transform(input)

            // then
            val norm = input.norm()

            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = norm,
                reference = URI("/$expectedPath"),
                target = Path(expectedPath, "index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллическими кодированными query и путём` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы")
            val expectedPath = "ф${_QUESTION}ы".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы&э")
            val expectedPath = "ф${_QUESTION}ы${_AMPERSAND}э".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы=z&э=w")
            val expectedPath = "ф${_QUESTION}ы=z${_AMPERSAND}э=w".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }


        private fun whenThen(input: URI, expectedPath: String) {
            val result = transform(input)

            // then
            val norm = input.norm()

            val expected = LocalResource(
                source = input,
                contentType = ContentType.Text.Html,
                normalizedSource = norm,
                reference = URI("/$expectedPath"),
                target = Path(expectedPath, "index.html"),
            )
            assertThat(result).isEqualTo(expected)
        }

    }

    companion object {
        private val QUESTION = "?".urlEncode()
        private val _QUESTION = QUESTION.replace('%', '_')
        private val AMPERSAND = "&".urlEncode()
        private val _AMPERSAND = AMPERSAND.replace('%', '_')
    }
}

@file:Suppress("NonAsciiCharacters", "ClassName", "LocalVariableName", "ObjectPropertyName")

package aikisib.url

import io.ktor.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class UrlTransformerHtmlTest {

    private val root = URI("https://aikisib.ru/")
    private val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl
    private val sut: UrlTransformer = UrlTransformerImpl

    private fun transform(input: URI) =
        sut.transform(ContentType.Text.Html, input)

    @Nested
    inner class ASCII {

        @Test
        fun `корневой URI`() {
            // when
            val result = transform(root)

            // then
            assertThat(result.toString()).isEqualTo("${root}index.html")
        }

        @Test
        fun `когда ничего делать не надо`() {
            // given
            val input = canonicolizer.canonicalize(root, "/page.html")

            // when
            val result = transform(input)

            // then
            assertThat(result.toString()).isEqualTo("$input")
            assertThat(result).isEqualTo(input)
        }

        @Test
        fun `добавляем расширение html`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/index.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = URI("$root?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}${_QUESTION}a.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}a.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?f&g")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}f${_AMPERSAND}g.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?foo=t&bar=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}foo=t${_AMPERSAND}bar=u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллической query` {

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = canonicolizer.canonicalize(URI("."), "$root?ы")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}${_QUESTION}ы.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}ы.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы&э")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}ы${_AMPERSAND}э.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?ы=t&э=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}i/${_QUESTION}ы=t${_AMPERSAND}э=u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллическим путём` {

        @Test
        fun `когда ничего делать не надо`() {
            // given
            val input = URI("${root}ы.html")

            // when
            val result = transform(input)

            // then
            assertThat(result.toString()).isEqualTo("${root}ы.html")
            assertThat(result).isEqualTo(input)
        }

        @Test
        fun `добавляем расширение html`() {
            // given
            val yi = "ы".urlEncode()
            val input = canonicolizer.canonicalize(root, "/ы")
            assertThat(input.rawPath).isEqualTo("/$yi")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы/index.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы/${_QUESTION}a.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?f&g")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы/${_QUESTION}f${_AMPERSAND}g.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы?foo=t&bar=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы/${_QUESTION}foo=t${_AMPERSAND}bar=u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллическими query и путём` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф/${_QUESTION}ы.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы&э")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф/${_QUESTION}ы${_AMPERSAND}э.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф?ы=t&э=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф/${_QUESTION}ы=t${_AMPERSAND}э=u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    companion object {
        private val QUESTION = "?".urlEncode()
        private val _QUESTION = QUESTION.replace('%', '_')
        val AMPERSAND = "&".urlEncode()
        private val _AMPERSAND = AMPERSAND.replace('%', '_')
    }
}

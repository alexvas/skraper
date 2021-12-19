@file:Suppress("NonAsciiCharacters", "ClassName")

package aikisib.url

import io.ktor.http.*
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
            val expected = URI("${root}${QUESTION}a.html")
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
            val expected = URI("${root}i/${QUESTION}a.html")
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
            val expected = URI("${root}i/${QUESTION}f${AMPERSAND}g.html")
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
            val expected = URI("${root}i/${QUESTION}foo${EQUALS}t${AMPERSAND}bar${EQUALS}u.html")
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
            val expected = URI("${root}${QUESTION}ы.html")
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
            val expected = URI("${root}i/${QUESTION}ы.html")
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
            val expected = URI("${root}i/${QUESTION}ы${AMPERSAND}э.html")
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
            val expected = URI("${root}i/${QUESTION}ы${EQUALS}t${AMPERSAND}э${EQUALS}u.html")
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
            val input = canonicolizer.canonicalize(root, "/ы")

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
            val expected = URI("${root}ы/${QUESTION}a.html")
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
            val expected = URI("${root}ы/${QUESTION}f${AMPERSAND}g.html")
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
            val expected = URI("${root}ы/${QUESTION}foo${EQUALS}t${AMPERSAND}bar${EQUALS}u.html")
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
            val expected = URI("${root}ф/${QUESTION}ы.html")
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
            val expected = URI("${root}ф/${QUESTION}ы${AMPERSAND}э.html")
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
            val expected = URI("${root}ф/${QUESTION}ы${EQUALS}t${AMPERSAND}э${EQUALS}u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    companion object {
        val QUESTION = "?".urlEncode()
        val EQUALS = "=".urlEncode()
        val SLASH = "/".urlEncode()
        val AMPERSAND = "&".urlEncode()
    }
}

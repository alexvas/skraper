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

        private val encoded1 = "ы".urlEncode()
        private val encoded2 = "э".urlEncode()

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = canonicolizer.canonicalize(URI("."), "$root?ы")

            // when
            val result = transform(input)

            // then
            val expected = canonicolizer.canonicalize(root, "/${QUESTION}$encoded1.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/i?$encoded1")

            // when
            val result = transform(input)

            // then
            val expected = canonicolizer.canonicalize(root, "/i/${QUESTION}$encoded1.html")
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
            val expected = URI("${root}i/${QUESTION}${encoded1}${AMPERSAND}$encoded2.html")
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
            val expected = URI("${root}i/${QUESTION}${encoded1}${EQUALS}t${AMPERSAND}${encoded2}${EQUALS}u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }


    @Nested
    inner class `Html с кириллическим путём` {
        private val encoded1 = "ы".urlEncode()

        @Test
        fun `когда ничего делать не надо`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded1.html")

            // when
            val result = transform(input)

            // then
            assertThat(result.toString()).isEqualTo("$root$encoded1.html")
            assertThat(result).isEqualTo(input)
        }

        @Test
        fun `добавляем расширение html`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded1")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded1/index.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded1?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded1/${QUESTION}a.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded1?f&g")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded1/${QUESTION}f${AMPERSAND}g.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded1?foo=t&bar=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded1/${QUESTION}foo${EQUALS}t${AMPERSAND}bar${EQUALS}u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }


    @Nested
    inner class `Html с кириллическими query и путём` {

        private val encoded1 = "ы".urlEncode()
        private val encoded2 = "э".urlEncode()
        private val encoded3 = "ф".urlEncode()

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded3?$encoded1")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded3/${QUESTION}$encoded1.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded3?ы&э")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded3/${QUESTION}${encoded1}${AMPERSAND}$encoded2.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/$encoded3?ы=t&э=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}$encoded3/$QUESTION${encoded1}${EQUALS}t${AMPERSAND}${encoded2}${EQUALS}u.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    companion object {
        val QUESTION = "?".urlEncode()
        val EQUALS = "=".urlEncode()
        val SLASH = "/".urlEncode()
        val AMPERSAND = "&".urlEncode()
        val HASH = "#".urlEncode()
    }

}

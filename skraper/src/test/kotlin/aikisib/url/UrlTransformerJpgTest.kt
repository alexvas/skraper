@file:Suppress("NonAsciiCharacters", "ClassName", "LocalVariableName", "ObjectPropertyName")

package aikisib.url

import io.ktor.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class UrlTransformerJpgTest {

    private val root = URI("https://aikisib.ru/")
    private val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl
    private val sut: UrlTouchdownTransformer = UrlTouchdownTransformerImpl

    private fun transform(input: URI) =
        sut.transform(ContentType.Image.JPEG, input)

    @Nested
    inner class ASCII {

        @Test
        fun `когда ничего делать не надо`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg")

            // when
            val result = transform(input)

            // then
            assertThat(result.toString()).isEqualTo("$input")
            assertThat(result).isEqualTo(input)
        }

        @Test
        fun `не расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg?a")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?f&g")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg?f&g")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?foo=t&bar=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg?foo=t&bar=u")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Html с кириллической query` {

        @Test
        fun `не расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?ы")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg?ы")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?ы&э")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg/?ы&э")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/img.jpg?ы=t&э=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}img.jpg?ы=t&э=u")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Jpg с кириллическим путём` {

        @Test
        fun `когда ничего делать не надо`() {
            // given
            val input = URI("${root}ы.jpg")

            // when
            val result = transform(input)

            // then
            assertThat(result.toString()).isEqualTo("${root}ы.jpg")
            assertThat(result).isEqualTo(input)
        }

        @Test
        fun `не расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы.jpg?a")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы.jpg?a.html")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы.jpg?f&g")

            // when
            val result = transform(input)

            // then
            val expected = URI("ы.jpg?f&g")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ы.jpg?foo=t&bar=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ы.jpg?foo=t&bar=u")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    inner class `Jpg с кириллическими query и путём` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф.jpg?ы")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф.jpg?ы")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `не расшиваем элементы query`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф.jpg?ы&э")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф?ы&э")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = canonicolizer.canonicalize(root, "/ф.jpg?ы=t&э=u")

            // when
            val result = transform(input)

            // then
            val expected = URI("${root}ф.jpg?ы=t&э=u")
            assertThat(result.toString()).isEqualTo("$expected")
            assertThat(result).isEqualTo(expected)
        }
    }
}

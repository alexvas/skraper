@file:Suppress("NonAsciiCharacters", "ClassName", "LocalVariableName", "ObjectPropertyName")

package aikisib.url

import io.ktor.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path

class UrlLocalResourceJpgTest {

    private val root = URI("https://aikisib.ru/")
    private val standardizer: UrlStandardizer = UrlStandardizerImpl

    private fun transform(input: URI) =
        LocalResource.fromEtc(input, ContentType.Image.JPEG)

    @Nested
    inner class `корневой УРЛ` {

        @Test
        fun `валидный УРЛ`() {
            // given
            val input = URI("/img.jpg")
            val expectedPath = "img.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `валидный УРЛ с РАСШИРЕНИЕМ`() {
            // given
            val input = URI("/img.JPG")
            val expectedPath = "img.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `невалидный УРЛ`() {
            // given
            val input = URI("/img_wo_ext")
            val expectedPath = "img_wo_ext.jpg"
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class ASCII {

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/z.jpg?y")
            val expectedPath = "z${_QUESTION}y.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/z.jpg?y&w")
            val expectedPath = "z${_QUESTION}y${_AMPERSAND}w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/z.jpg?y=t&w=r")
            val expectedPath = "z${_QUESTION}y=t${_AMPERSAND}w=r.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `ASCII без расширения` {

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = URI("$root?y")
            val expectedPath = "${_QUESTION}y.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/z?y")
            val expectedPath = "z${_QUESTION}y.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/z?y&w")
            val expectedPath = "z${_QUESTION}y${_AMPERSAND}w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/z?y=t&w=r")
            val expectedPath = "z${_QUESTION}y=t${_AMPERSAND}w=r.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллической query` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/i.jpg?ы")
            val expectedPath = "i${_QUESTION}ы.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/i.jpg?ы&э")
            val expectedPath = "i${_QUESTION}ы${_AMPERSAND}э.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/i.jpg?ы=w&э=z")
            val expectedPath = "i${_QUESTION}ы=w${_AMPERSAND}э=z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллической query без расширения` {

        @Test
        fun `расшиваем корневую query`() {
            // given
            val input = standardizer.standardize(URI("."), "$root?ы")
            val expectedPath = "${_QUESTION}ы.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/i?ы")
            val expectedPath = "i${_QUESTION}ы.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/i?ы&э")
            val expectedPath = "i${_QUESTION}ы${_AMPERSAND}э.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/i?ы=w&э=z")
            val expectedPath = "i${_QUESTION}ы=w${_AMPERSAND}э=z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллическим путём` {

        @Test
        fun `HTML страничка ложится в свою директорию`() {
            // given
            val input = URI("${root}ы.jpg")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `ЧПУ ложится в свою директорию`() {
            // given
            val input = URI("${root}ы.jpg")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = URI("${root}ы.jpg?z")
            val expectedPath = "ы${_QUESTION}z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = URI("${root}ы.jpg?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = URI("${root}ы.jpg?f=w&g=z")
            val expectedPath = "ы${_QUESTION}f=w${_AMPERSAND}g=z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллическим путём без расширения` {

        @Test
        fun `HTML страничка ложится в свою директорию`() {
            // given
            val input = URI("${root}ы")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `ЧПУ ложится в свою директорию`() {
            // given
            val input = URI("${root}ы")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = URI("${root}ы?z")
            val expectedPath = "ы${_QUESTION}z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = URI("${root}ы?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = URI("${root}ы?f=w&g=z")
            val expectedPath = "ы${_QUESTION}f=w${_AMPERSAND}g=z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллическим кодированным путём` {

        @Test
        fun `кодированный ЧПУ ложится в свою директорию`() {
            // given
            val yi = "ы".urlEncode()
            val input = standardizer.standardize(root, "/ы.jpg")
            assertThat(input.rawPath).isEqualTo("/$yi.jpg")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/ы.jpg?z")
            val expectedPath = "ы${_QUESTION}z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/ы.jpg?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/ы.jpg?f=z&g=w")
            val expectedPath = "ы${_QUESTION}f=z${_AMPERSAND}g=w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллическим кодированным путём без расширения` {

        @Test
        fun `кодированный ЧПУ ложится в свою директорию`() {
            // given
            val yi = "ы".urlEncode()
            val input = standardizer.standardize(root, "/ы")
            assertThat(input.rawPath).isEqualTo("/$yi")
            val expectedPath = "ы.jpg"
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/ы?z")
            val expectedPath = "ы${_QUESTION}z.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/ы?f&g")
            val expectedPath = "ы${_QUESTION}f${_AMPERSAND}g.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/ы?f=z&g=w")
            val expectedPath = "ы${_QUESTION}f=z${_AMPERSAND}g=w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кириллическими кодированными query и путём` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/ф.jpg?ы")
            val expectedPath = "ф${_QUESTION}ы.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/ф.jpg?ы&э")
            val expectedPath = "ф${_QUESTION}ы${_AMPERSAND}э.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/ф.jpg?ы=z&э=w")
            val expectedPath = "ф${_QUESTION}ы=z${_AMPERSAND}э=w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    @Nested
    inner class `JPG с кирилл кодированными query и путём без расширения` {

        @Test
        fun `расшиваем query`() {
            // given
            val input = standardizer.standardize(root, "/ф?ы")
            val expectedPath = "ф${_QUESTION}ы.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query`() {
            // given
            val input = standardizer.standardize(root, "/ф?ы&э")
            val expectedPath = "ф${_QUESTION}ы${_AMPERSAND}э.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }

        @Test
        fun `расшиваем элементы query со значениями`() {
            // given
            val input = standardizer.standardize(root, "/ф?ы=z&э=w")
            val expectedPath = "ф${_QUESTION}ы=z${_AMPERSAND}э=w.jpg".lowercase()
            // when, then
            whenThen(input, expectedPath)
        }
    }

    private fun whenThen(input: URI, expectedPath: String) {
        // when
        val result = transform(input)

        val norm = input.norm()

        // then
        val expected = LocalResource(
            source = input,
            contentType = ContentType.Image.JPEG,
            normalizedSource = norm,
            reference = URI("/$expectedPath"),
            target = Path(expectedPath),
        )
        assertThat(result).isEqualTo(expected)
    }

    companion object {
        val QUESTION = "?".urlEncode()
        val _QUESTION = QUESTION.replace('%', '_')
        val AMPERSAND = "&".urlEncode()
        val _AMPERSAND = AMPERSAND.replace('%', '_')
    }
}

fun URI.norm() = URI(
    scheme,
    null,
    host,
    -1,
    path.lowercase(),
    query?.lowercase(),
    null,
)

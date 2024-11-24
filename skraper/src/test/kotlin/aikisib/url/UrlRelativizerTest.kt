@file:Suppress("NonAsciiCharacters", "ClassName")

package aikisib.url

import io.ktor.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

internal class UrlRelativizerTest {

    private val root = URI("https://aikisib.ru/")
    private val standardizer: UrlStandardizer = UrlStandardizerImpl
    private val sut: UrlRelativizer = UrlRelativizerImpl

    private fun relativize(source: URI, target: URI) =
        sut.maybeRelativize(
            ContentType.Text.CSS,
            source,
            target,
            target.rawFragment,
        )

    @Nested
    inner class `путь отсутствует` {

        @Test
        fun `путь к элементу во вложенной папке`() {
            // given
            val source = root
            val target = standardizer.standardize(root, "/plugin/somePlugin/css/someStyle.css")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("plugin/somePlugin/css/someStyle.css")
        }

        @Test
        fun `тестируем ссылку из корня на корень`() {
            // given
            val source = standardizer.standardize(root, "#up")
            val target = standardizer.standardize(root, "#down")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("#down")
        }
    }

    @Nested
    inner class `путь записан ASCII` {

        @Test
        fun `путь к элементу во вложенной папке`() {
            // given
            val source = standardizer.standardize(root, "/plugin/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/plugin/somePlugin/css/font/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней папке`() {
            // given
            val source = standardizer.standardize(root, "/plugin/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/plugin/somePlugin/font/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("https://aikisib.ru/plugin/somePlugin/font/someFont.wolf2")
        }

        @Test
        fun `путь к страничке кузену`() {
            // given
            val source = standardizer.standardize(root, "/plugin1.html")
            val target = standardizer.standardize(root, "/plugin2.html")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("plugin2.html")
        }

        @Test
        fun `та же страничка другой фрагмент`() {
            // given
            val source = standardizer.standardize(root, "/timetable#a=b")
            val target = standardizer.standardize(root, "/timetable#c=d")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("#c=d")
        }
    }

    @Nested
    inner class `путь записан кириллицей` {

        private val path = "ы"
        private val encodedPath = path.urlEncode()

        @Test
        fun `путь к элементу во вложенной папке`() {
            // given
            val source = standardizer.standardize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/$encodedPath/somePlugin/css/font/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу во вложенной локализованной папке`() {
            // given
            val source = standardizer.standardize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/$encodedPath/somePlugin/css/$encodedPath/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("$encodedPath/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней папке`() {
            // given
            val source = standardizer.standardize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/$encodedPath/somePlugin/font/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("https://aikisib.ru/%D1%8B/somePlugin/font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней локализованной папке`() {
            // given
            val source = standardizer.standardize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = standardizer.standardize(root, "/$encodedPath/somePlugin/$encodedPath/someFont.wolf2")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("https://aikisib.ru/%D1%8B/somePlugin/%D1%8B/someFont.wolf2")
        }

        @Test
        fun `путь к страничке кузену`() {
            // given
            val source = standardizer.standardize(root, "/plugin1.html")
            val target = standardizer.standardize(root, "/$encodedPath.html")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("$encodedPath.html")
        }

        @Test
        fun `та же страничка другой фрагмент`() {
            // given
            val source = standardizer.standardize(root, "/$encodedPath#a=b")
            val target = standardizer.standardize(root, "/$encodedPath#c=d")

            // when
            val relative = relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("#c=d")
        }
    }

    @Test
    fun `путь содержит спецсимволы`() {
        // given
        @Suppress("MaxLineLength")
        val input = URI("https://aikisib.ru/wp-admin/%3Faction%3Dlostpassword%26redirect_to%3Dhttps%3A%2F%2Faikisib%2Eru%2Fprivacy-policy-2%2F.html")

        // when
        val relative = relativize(root, input)

        // then
        @Suppress("MaxLineLength")
        assertThat(relative.toString())
            .isEqualTo("wp-admin/%3Faction%3Dlostpassword%26redirect_to%3Dhttps%3A%2F%2Faikisib%2Eru%2Fprivacy-policy-2%2F.html")
    }
}

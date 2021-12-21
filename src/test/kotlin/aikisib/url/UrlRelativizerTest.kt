@file:Suppress("NonAsciiCharacters", "ClassName")

package aikisib.url

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

internal class UrlRelativizerTest {

    private val root = URI("https://aikisib.ru/")
    private val canonicolizer: UrlCanonicolizer = UrlCanonicolizerImpl
    private val sut: UrlRelativizer = UrlRelativizerImpl

    @Nested
    inner class `путь отсутствует` {

        @Test
        fun `путь к элементу во вложенной папке`() {
            // given
            val source = root
            val target = canonicolizer.canonicalize(root, "/plugin/somePlugin/css/someStyle.css")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("plugin/somePlugin/css/someStyle.css")
        }

        @Test
        fun `тестируем ссылку из корня на корень`() {
            // given
            val source = canonicolizer.canonicalize(root, "#up")
            val target = canonicolizer.canonicalize(root, "#down")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("#down")
        }
    }

    @Nested
    inner class `путь записан ASCII` {

        @Test
        fun `путь к элементу во вложенной папке`() {
            // given
            val source = canonicolizer.canonicalize(root, "/plugin/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/plugin/somePlugin/css/font/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней папке`() {
            // given
            val source = canonicolizer.canonicalize(root, "/plugin/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/plugin/somePlugin/font/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("../font/someFont.wolf2")
        }

        @Test
        fun `путь к страничке кузену`() {
            // given
            val source = canonicolizer.canonicalize(root, "/plugin1.html")
            val target = canonicolizer.canonicalize(root, "/plugin2.html")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("plugin2.html")
        }

        @Test
        fun `та же страничка другой фрагмент`() {
            // given
            val source = canonicolizer.canonicalize(root, "/timetable#a=b")
            val target = canonicolizer.canonicalize(root, "/timetable#c=d")

            // when
            val relative = sut.relativize(source, target)

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
            val source = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/font/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу во вложенной локализованной папке`() {
            // given
            val source = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/$encodedPath/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("$encodedPath/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней папке`() {
            // given
            val source = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/font/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("../font/someFont.wolf2")
        }

        @Test
        fun `путь к элементу в соседней локализованной папке`() {
            // given
            val source = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/css/someStyle.css")
            val target = canonicolizer.canonicalize(root, "/$encodedPath/somePlugin/$encodedPath/someFont.wolf2")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("../$encodedPath/someFont.wolf2")
        }

        @Test
        fun `путь к страничке кузену`() {
            // given
            val source = canonicolizer.canonicalize(root, "/plugin1.html")
            val target = canonicolizer.canonicalize(root, "/$encodedPath.html")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("$encodedPath.html")
        }

        @Test
        fun `та же страничка другой фрагмент`() {
            // given
            val source = canonicolizer.canonicalize(root, "/$encodedPath#a=b")
            val target = canonicolizer.canonicalize(root, "/$encodedPath#c=d")

            // when
            val relative = sut.relativize(source, target)

            // then
            assertThat(relative.toString()).isEqualTo("#c=d")
        }
    }

    @Test
    fun `путь содержит спецсимволы`() {
        // given
        val input = URI("https://aikisib.ru/wp-admin/%3Faction%3Dlostpassword%26redirect_to%3Dhttps%3A%2F%2Faikisib%2Eru%2Fprivacy-policy-2%2F.html")

        // when
        val relative = sut.relativize(root, input)

        // then
        assertThat(relative.toString()).isEqualTo("wp-admin/%3Faction%3Dlostpassword%26redirect_to%3Dhttps%3A%2F%2Faikisib%2Eru%2Fprivacy-policy-2%2F.html")
    }

}

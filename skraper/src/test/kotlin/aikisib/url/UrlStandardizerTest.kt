@file:Suppress("NonAsciiCharacters", "ClassName")

package aikisib.url

import aikisib.url.UrlLocalResourceJpgTest.Companion.AMPERSAND
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class UrlStandardizerTest {

    // Корневой URI для всех абсолютных
    private val root = URI.create("https://aikido.nsk.su")
    private val sut: UrlStandardizer = UrlStandardizerImpl

    @Nested
    inner class `путь отсутствует` {

        @Test
        fun `прибавляем слэш после корневого пути`() {
            // given
            val input = "$root"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$input/")
        }

        @Test
        fun `не удаляем слэш после корневого пути`() {
            // given
            val input = "$root/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }
    }

    @Nested
    inner class `путь записан ASCII` {

        private val path = "i"

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$path"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$path/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$path")
        }

        @Test
        fun `если слэш кодирован, то не удаляем его`() {
            // given
            val input = "$root/$path$SLASH"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
        }

        @Test
        fun `не удаляем кодированный слэш из середины`() {
            // given
            val input = "$root/i${SLASH}v"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/i${SLASH}v")
        }
    }

    @Nested
    inner class `путь записан кириллицей` {

        private val path = "ы"
        private val encodedPath = path.urlEncode()

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$path"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$encodedPath")
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$path/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$encodedPath")
        }

        @Test
        fun `если слэш кодирован, не удаляем его`() {
            // given
            val input = "$root/$path$SLASH"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/$encodedPath$SLASH")
        }
    }

    @Nested
    inner class `в пути имеются кодированные спецсимволы` {

        private val encodedPath = "i$SLASH$AMPERSAND"

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$encodedPath"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/i$SLASH&")
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$encodedPath/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/i$SLASH&")
        }

        @Test
        fun `если слэш кодирован, то не удаляем его`() {
            // given
            val input = "$root/$encodedPath$SLASH"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/i$SLASH&$SLASH")
        }
    }

    @Nested
    inner class `путь URL кодирован` {

        private val encodedPath = "ы".urlEncode()

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$encodedPath"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$encodedPath/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$encodedPath")
        }

        @Test
        fun `если слэш кодирован, то не удаляем его`() {
            // given
            val input = "$root/$encodedPath$SLASH"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }
    }

    @Nested
    inner class `после пути имеется query запрос` {

        private val path = "i"
        private val query = "a=b&c=d&f"

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$path?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$path/?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$path?$query")
        }

        @Test
        fun `если слэш кодирован, то не удаляем его`() {
            // given
            val input = "$root/$path$SLASH?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
        }
    }

    @Nested
    inner class `после пути имеется кириллический query запрос` {

        private val path = "i"
        private val query = "а=б&в=г&д"

        @Test
        fun `не прибавляем слэш после пути`() {
            // given
            val input = "$root/$path?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo(input)
        }

        @Test
        fun `удаляем слэш после пути`() {
            // given
            val input = "$root/$path/?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/$path?$query")
        }

        @Test
        fun `если слэш кодирован, то не удаляем его`() {
            // given
            val input = "$root/$path$SLASH?$query"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
        }
    }

    @Nested
    inner class фрагмент {

        private val path = "i"
        private val query = "a=b&c=d&f"

        @Nested
        inner class `без особых символов` {
            private val fragment = "2478xdfjk,,21-"

            @Test
            fun `не прибавляем слэш после пути`() {
                // given
                val input = "$root/$path?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo(input)
            }

            @Test
            fun `удаляем слэш после пути`() {
                // given
                val input = "$root/$path/?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo("$root/$path?$query#$fragment")
            }

            @Test
            fun `если слэш кодирован то, не удаляем его`() {
                // given
                val input = "$root/$path$SLASH?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
            }
        }

        @Nested
        inner class `со слэшем` {
            private val fragment = "2478x/dfjk/1=2/,,21-"

            @Test
            fun `не прибавляем слэш после пути`() {
                // given
                val input = "$root/$path?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo(input)
            }

            @Test
            fun `удаляем слэш после пути`() {
                // given
                val input = "$root/$path/?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo("$root/$path?$query#$fragment")
            }

            @Test
            fun `если слэш кодирован, то не удаляем его`() {
                // given
                val input = "$root/$path$SLASH?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
            }
        }

        @Nested
        inner class `с вопросом` {
            private val fragment = "2478x?,,21-"

            @Test
            fun `не прибавляем слэш после пути`() {
                // given
                val input = "$root/$path?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo(input)
            }

            @Test
            fun `удаляем слэш после пути`() {
                // given
                val input = "$root/$path/?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.toString()).isEqualTo("$root/$path?$query#$fragment")
            }

            @Test
            fun `если слэш кодирован, то не удаляем его`() {
                // given
                val input = "$root/$path$SLASH?$query#$fragment"

                // when
                val canonical = sut.standardize(root, input)

                // then
                assertThat(canonical.rawPath).isEqualTo("/$path$SLASH")
            }
        }

        private val fragment = "2478x#,,21-"

        @Test
        fun `фрагмент с решёткой запрещён`() {
            // given
            val input = "$root/$path?$query#$fragment"

            assertThatIllegalArgumentException()
                .isThrownBy {
                    // when
                    sut.standardize(root, input)
                }
                // then
                .withMessageContaining("Illegal character in fragment at index ")
        }
    }

    @Nested
    inner class `относительный путь` {

        @Test
        fun `разрешается верно`() {
            // given
            val parent = URI("$root/plugins/somePlugin/css/someStyle.css")
            val relativeInput = "../fonts/someFont.wolf2"

            // when
            val canonical = sut.standardize(parent, relativeInput)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/plugins/somePlugin/fonts/someFont.wolf2")
        }

        @Test
        fun `относительно корневого узла тоже верно`() {
            // given
            val relativeInput = "./fonts/someFont.wolf2"

            // when
            val canonical = sut.standardize(root, relativeInput)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/fonts/someFont.wolf2")
        }

        @Test
        fun `относительно корневого узла со слэшем тоже верно`() {
            // given
            val parent = URI("$root/")
            val relativeInput = "./fonts/someFont.wolf2"

            // when
            val canonical = sut.standardize(parent, relativeInput)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/fonts/someFont.wolf2")
        }

        @Test
        fun `путь-точка в корневом узле`() {
            // given
            val relativeInput = "."

            // when
            val canonical = sut.standardize(root, relativeInput)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/")
            assertThat(canonical).isEqualTo(URI("$root/"))
        }

        @Test
        fun `путь-точка в корневом узле со слэшем`() {
            // given
            val relativeInput = "."
            val parent = URI("$root/")

            // when
            val canonical = sut.standardize(parent, relativeInput)

            // then
            assertThat(canonical.toString()).isEqualTo("$root/")
            assertThat(canonical).isEqualTo(URI("$root/"))
        }

        @Test
        fun `некорректный путь относительно корневого узла НЕ вызовет ошибку`() {
            // given
            val input = "../somefile.jpg"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/../somefile.jpg")
        }

        @Test
        fun `относительный путь заканчивается слэшем`() {
            // given
            val input = "/nso/"

            // when
            val canonical = sut.standardize(root, input)

            // then
            assertThat(canonical.rawPath).isEqualTo("/nso")
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `динамический протокол`() {
        // given
        val input = "//fonts.googleapis.com/css?family=Montserrat:300,300italic,400,400italic,700,700italic"

        // when
        val canonical = sut.standardize(root, input)

        // then
        assertThat(canonical.toString()).isEqualTo("https://fonts.googleapis.com/css?family=Montserrat:300,300italic,400,400italic,700,700italic")
    }

    @Test
    @Suppress("MaxLineLength")
    fun `ссылка по RFC 3986`() {
        // given
        val input = "//fonts.googleapis.com/css?family=Montserrat:300,300italic,400,400italic,700,700italic|Oswald:300,300italic,400,400italic,700,700italic|Open+Sans:300,300italic,400,400italic,700,700italic&subset=latin,latin-ext,cyrillic"

        // when
        val canonical = sut.standardize(root, input)

        // then
        val expected = "https:" + input.replace("|", "%7C")
        assertThat(canonical.toString()).isEqualTo(expected)
    }

    companion object {
        val SLASH = "/".urlEncode()
    }
}

internal fun String.urlEncode(): String =
    URLEncoder.encode(this, UTF_8)

@file:Suppress("NonAsciiCharacters")

package aikisib

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NonceTest {
    private val page = "slider_revolution_page.html"
    private fun pageIs() = ClassLoader.getSystemClassLoader().getResourceAsStream(page) ?: error("$page not found")
    private val content: String = pageIs().use {
        it.reader().use { reader -> reader.readText() }
    }

    @Test
    fun `находим nonce в сорцах странички`() {
        // given
        assertThat(content)
            .contains("RVS.ENV.nonce")
            .containsPattern(SliderRevolutionScraperImpl.NONCE_REGEX.pattern)
    }

    @Test
    fun `правильно выделяем nonce из странички`() {
        // given
        val result = SliderRevolutionScraperImpl.NONCE_REGEX.find(content)

        assertThat(result).isNotNull
        check(result != null)
        assertThat(result.groupValues.size).isEqualTo(2)
        assertThat(result.groupValues[1]).containsPattern("[a-z0-9]+")
    }
}

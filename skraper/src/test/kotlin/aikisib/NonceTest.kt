@file:Suppress("NonAsciiCharacters")

package aikisib

import aikisib.slider.SliderRevolutionScraperImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NonceTest {
    private val content = readResourceContent("slider_revolution_page.html")

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

fun readResourceContent(fileName: String): String {
    val pageIs = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName) ?: error("$fileName not found")
    return pageIs.use {
        it.reader().use { reader -> reader.readText() }
    }
}

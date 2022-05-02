@file:Suppress("NonAsciiCharacters")

package aikisib.mirror

import aikisib.readResourceContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LinkExtractorTest {

    private val content = readResourceContent("style.css")

    @Test
    fun `достаём ссылки из стиля темы`() {
        val links = content.extractLinks().toList()
        assertThat(links).hasSize(8)
    }
}

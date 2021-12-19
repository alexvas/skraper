@file:Suppress("NonAsciiCharacters")

package aikisib.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResourceTypeTest {

    @Test
    fun `успешно находим тип ресурса`() {
        // given
        val input = "some_pic.png"

        // when
        val found = ResourceType.findOrNull(input)

        // then
        assertThat(found).isNotNull
        check(found != null)
        assertThat(found).isEqualTo(ResourceType.BITMAP_IMAGE)
    }

    @Test
    fun `не находим ресурс произвольного типа`() {
        // given
        val input = "some_pic.abcdef"

        // when
        val found = ResourceType.findOrNull(input)

        // then
        assertThat(found).isNull()
    }

    @Test
    fun `типы ресурсов не пересекаются по расширениям`() {
        // given
        val unique = mutableMapOf<String, ResourceType>()

        // when
        ResourceType.values().forEach { curr ->
            curr.extensions.forEach { ext ->
                // and when
                val prev = unique.put(ext, curr)

                // then
                assertThat(prev)
                    .describedAs("расширение '$ext' зарегистрировано и для $curr, и для $prev")
                    .isNull()
            }
        }
    }
}

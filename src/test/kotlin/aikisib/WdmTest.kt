@file:Suppress("NonAsciiCharacters")

package aikisib

import io.github.bonigarcia.wdm.WebDriverManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File


class WdmTest {
    lateinit var driver: WebDriver
    private val wdm: WebDriverManager = WebDriverManager.chromedriver().browserInDocker()

    @BeforeEach
    fun setupTest() {
        val chromeOptions = ChromeOptions().also {
        }
        val prefs = mutableMapOf<String, Any>()
        prefs["download.prompt_for_download"] = false
        chromeOptions.setExperimentalOption("prefs", prefs)
        driver = wdm.create()
    }

    @AfterEach
    fun teardown() {
        wdm.quit()
    }

    @Test
    fun test() {
        // Exercise
        driver["https://bonigarcia.dev/selenium-webdriver-java/"]
        val title = driver.title

        // Verify
        assertThat(title).contains("Selenium WebDriver")
    }

    private val homePageUrl = "https://aikisib.ru"

    @Test
    fun `Домашняя страница открывается`() {
        driver.run {
            get(homePageUrl)
            pageSource shouldContain "Айкидо"

            val links: List<WebElement> = findElements(By.tagName("a"))
            val hrefs = links
                .asSequence()
                .map {
                    it.getAttribute("href")
                }
                .joinToString(separator = "\n")
            with(File("/tmp/aikisib.links.txt")) {
                val creationResult = createNewFile()
                creationResult shouldBe true
                writeText(hrefs)
            }
            quit()
        }
    }
}

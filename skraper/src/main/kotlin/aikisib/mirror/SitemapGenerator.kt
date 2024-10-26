package aikisib.mirror

import aikisib.model.OriginalDescription
import cz.jiripinkas.jsitemapgenerator.WebPage
import cz.jiripinkas.jsitemapgenerator.robots.RobotsRule
import cz.jiripinkas.jsitemapgenerator.robots.RobotsTxtGenerator
import io.ktor.http.ContentType
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.writeText
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator as JavaSitemapGenerator

interface SitemapGenerator {
    fun generate(descriptions: Sequence<OriginalDescription>)
}

internal class SitemapGeneratorImpl(
    private val canonicalHref: URI,
    private val targerRootPath: Path,
) : SitemapGenerator {

    override fun generate(descriptions: Sequence<OriginalDescription>) {
        generateAndSaveSitemap(descriptions)
        generateAndSaveRobotsTxt()
    }

    private fun generateAndSaveSitemap(descriptions: Sequence<OriginalDescription>) {
        val pages = descriptions.filter { it.type == ContentType.Text.Html }
            .map { description ->
                val path = description.remoteUri.path
                val builder = WebPage.builder()
                    .name(path)
                if (path == null || path == "/") {
                    builder.maxPriorityRoot()
                }
                val lastModified = description.lastModified
                if (lastModified == null) {
                    builder.lastModNow()
                } else {
                    builder.lastMod(lastModified)
                }
                builder.build()
            }
        val sitemapContent = JavaSitemapGenerator.of(canonicalHref.toString())
            .addPages(pages.toList())
            .toString()
        targerRootPath.resolve("sitemap.xml").writeText(sitemapContent)
    }

    private fun generateAndSaveRobotsTxt() {
        val robotsTxtContent = RobotsTxtGenerator.of(canonicalHref.toString())
            .addSitemap("sitemap.xml")
            .addRule(RobotsRule.builder().userAgentAll().disallow("/*?*").build())
            .toString()
        val cleanParam = (
            ('a'..'z') +
                ('A'..'Z') +
                ('0'..'9') +
                '.' + '_' + '-')
            .joinToString("&")
        val robotsTxtContentWithCleanParams = """$robotsTxtContent
                                                |Clean-param: $cleanParam""".trimMargin("|")
        targerRootPath.resolve("robots.txt").writeText(robotsTxtContentWithCleanParams)
    }
}

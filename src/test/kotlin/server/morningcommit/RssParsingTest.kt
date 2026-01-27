package server.morningcommit

import com.rometools.rome.io.FeedException
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.IOException
import java.net.URI
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RssParsingTest {

    @Test
    fun `should parse Woowahan tech blog RSS feed`() {
        val feedUrl = URI("https://techblog.woowahan.com/feed/").toURL()

        val feed = assertDoesNotThrow {
            try {
                XmlReader(feedUrl).use { reader ->
                    SyndFeedInput().build(reader)
                }
            } catch (e: IOException) {
                throw AssertionError("Failed to fetch RSS feed: ${e.message}", e)
            } catch (e: FeedException) {
                throw AssertionError("Failed to parse RSS feed: ${e.message}", e)
            }
        }

        assertNotNull(feed, "Feed should not be null")
        assertTrue(feed.entries.isNotEmpty(), "Feed entries should not be empty")

        println("=== Blog Title ===")
        println(feed.title)
        println()
        println("=== First 5 Entries ===")
        feed.entries.take(5).forEachIndexed { index, entry ->
            println("${index + 1}. ${entry.title}")
        }
    }
}

package server.morningcommit.util

object XmlSanitizer {
    private val DOCTYPE_REGEX = Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE)
    private val CONTROL_CHAR_REGEX = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]")

    fun sanitize(xml: String): String {
        return xml
            .replace(DOCTYPE_REGEX, "")
            .replace(CONTROL_CHAR_REGEX, "")
    }
}

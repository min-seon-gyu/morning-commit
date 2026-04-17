package server.morningcommit.util

import io.github.oshai.kotlinlogging.KLogger

inline fun <T> KLogger.runLogging(contextMessage: String, block: () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        this.error(e) { "$contextMessage: ${e.message}" }
        throw e
    }
}

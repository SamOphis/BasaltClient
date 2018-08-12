package basalt.client.entities.builders

import basalt.client.entities.AudioNode
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnegative

@Suppress("UNUSED")
open class PlaceholderValues internal constructor() {
    @Nonnegative
    var wsPort: Int = 80
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 1) {
                LOGGER.error("Cannot set the WebSocket Port to below one. ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    @Nonnegative
    var maxInterval: Long = 15
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 1) {
                LOGGER.error("Cannot set the Maximum Reconnect Interval to below one. ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    var baseInterval: Long = 1
    var intervalExpander: ((AudioNode, Long) -> Long) = { _, interval -> interval * 2 }
    var intervalTimeUnit: TimeUnit = TimeUnit.SECONDS
    var nodePassword: String = "youshallnotpass"

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PlaceholderValues::class.java)
    }
}
package basalt.client.entities.builders

import basalt.client.entities.BasaltClient
import org.slf4j.LoggerFactory
import javax.annotation.Nonnegative

@Suppress("UNUSED")
class BasaltClientBuilder: PlaceholderValues() {
    @Nonnegative
    var userId: Long = 0
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 0) {
                LOGGER.error("Cannot set User ID to a negative value! ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    fun build(): BasaltClient = BasaltClient(wsPort, baseInterval, maxInterval, intervalTimeUnit, intervalExpander, nodePassword, userId)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClientBuilder::class.java)
    }
}
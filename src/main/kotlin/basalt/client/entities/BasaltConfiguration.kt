package basalt.client.entities

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.Nonnegative

open class BasaltConfiguration {
    protected val isUsed = AtomicBoolean(false)

    @Nonnegative
    var userId: Long = 0
        get() {
            isUsed.set(false)
            return field
        }
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.error("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            if (value < 0) {
                LOGGER.error("Attempt to set User ID to a negative value! {}", value)
                throw IllegalArgumentException("User ID ($value) is negative!")
            }
            field = value
        }

    @Nonnegative
    var wsPort: Int = 80
        get() {
            isUsed.set(true)
            return field

        }
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.error("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            if (value < 0) {
                LOGGER.error("Attempt to set WebSocket Port to a negative value! {}", value)
                throw IllegalArgumentException("WebSocket Port ($value) is negative!")
            }
            field = value
        }

    @Nonnegative
    var maxInterval: Long = 15
        get() {
            isUsed.set(true)
            return field
        }
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.error("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            if (value < 0) {
                LOGGER.error("Attempt to set maximum interval to a negative value! {}", value)
                throw IllegalArgumentException("Maximum Interval ($value) is negative!")
            }
            field = value
        }

    var baseInterval: Long = 0
        get() {
            isUsed.set(true)
            return field
        }
        @Throws(IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.warn("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            field = value
        }

    var intervalTimeUnit: TimeUnit = TimeUnit.SECONDS
        get() {
            isUsed.set(true)
            return field
        }
        @Throws(IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.warn("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            field = value
        }
    // to cause compile time errors to remind me to come back to this point
    var intervalExpander: (Any, Long) -> Long = { node, _ -> Math.pow(2.toDouble(), 5.toDouble()).toLong() }
        get() {
            isUsed.set(true)
            return field
        }
        @Throws(IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.warn("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            field = value
        }

    var password: String = "youshallnotpass"
        get() {
            isUsed.set(true)
            return field
        }
        @Throws(IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.warn("Attempt to modify a configuration class after it has been used!")
                throw IllegalStateException("Configuration classes cannot be modified after being used!")
            }
            field = value
        }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(BasaltConfiguration::class.java)
    }
}
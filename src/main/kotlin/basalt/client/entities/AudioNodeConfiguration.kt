package basalt.client.entities

import com.jsoniter.any.Any
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias SocketHandlerMap = Object2ObjectOpenHashMap<String, (WebSocket, Any) -> Unit>

open class AudioNodeConfiguration: BasaltConfiguration() {
    var factory: WebSocketFactory = WebSocketFactory() // doesn't need checks as is only used once for initial creation.
    var initializer: ((WebSocket) -> WebSocket) = { it } // again doesn't need checks as it's only used once.

    var address: String? = null
        @Throws(IllegalStateException::class)
        get() {
            if (field == null) {
                LOGGER.error("Node addresses must be specified before being used!")
                throw IllegalStateException("Node address == null")
            }
            isUsed.set(true)
            return field
        }
        @Throws(IllegalStateException::class)
        set(value) {
            if (isUsed.get()) {
                LOGGER.error("Cannot modify configuration classes after being used!")
                throw IllegalStateException("Configuration class already used!")
            }
            field = "ws://$value:$wsPort"
        }
    val handlers = SocketHandlerMap()
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioNodeConfiguration::class.java)
    }
}
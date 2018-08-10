package basalt.client.entities

import basalt.client.entities.messages.server.DispatchResponse
import com.jsoniter.JsonIterator
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class AudioNode(val client: BasaltClient, val configuration: AudioNodeConfiguration): WebSocketAdapter() {
    private val handlers = Object2ObjectMaps.unmodifiable(SocketHandlerMap(configuration.handlers))
    private val seq = AtomicLong(0)
    val socket: WebSocket
    init {
        try {
            socket = configuration.initializer(configuration.factory
                    .createSocket(configuration.address)
                    .addListener(this)
                    .addHeader("Authorization", configuration.password)
                    .addHeader("User-Id", configuration.userId.toString())
                    .connectAsynchronously())
        } catch (exc: IOException) {
            LOGGER.error("Error when creating a WebSocket instance!", exc)
            throw exc // no way to recover from this.
        }
    }

    override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        LOGGER.info("Closing BasaltClient connection!")
    }

    override fun onTextMessage(websocket: WebSocket, text: String) {
        val data = JsonIterator.deserialize(text)
        val op = data["op"]!!.toString()
        val handler = handlers[op]
        handler?.let {
            it(websocket, data)
            return
        }
        LOGGER.warn("Unhandled Event: {}", op)
    }

    override fun onError(socket: WebSocket, error: WebSocketException) {
        LOGGER.error("Error thrown in the WebSocket Handler for Node URI: {} | Error Message: {}", configuration.address, error.message ?: "No message.")
    }

    override fun onConnectError(websocket: WebSocket, exception: WebSocketException) {
        LOGGER.error("Error when connecting to ${configuration.address}, Message: {}", exception.message)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioNode::class.java)
    }
}
package basalt.client.entities

import basalt.client.entities.builders.SocketHandlerMap
import com.jsoniter.JsonIterator
import com.jsoniter.any.Any
import com.jsoniter.spi.JsonException
import com.neovisionaries.ws.client.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.io.IOException
import java.util.concurrent.TimeUnit


/*
each creation of node creates a flux for dispatched events
flux consumer adds ws listener. incoming events progress sink (whole Any body is sent)

when initializing, send request obv, filter flux based on event name (INITIALIZED) and matching keys
map to a generated mono (Mono.generate<Unit?>)  which simply completes (no data = no event)

when loading tracks which obv come in chunks, send request, filter flux based on LOAD_TRACK_CHUNK and CHUNK FINISHED + keys.
map to a generated flux (Flux.generate<Array<TrackInfoClass>>). each chunk = next, chunk finished = complete
 */

@Suppress("UNUSED")
class AudioNode internal constructor(val client: BasaltClient, val wsPort: Int, val baseInterval: Long,
                                     val maxInterval: Long, val intervalExpander: ((AudioNode, Long) -> Long),
                                     val intervalTimeUnit: TimeUnit, val factory: WebSocketFactory,
                                     val initializer: ((WebSocket) -> WebSocket), val password: String, val address: String,
                                     private val handlers: SocketHandlerMap): WebSocketAdapter() {

    val socket: WebSocket
    init {
        try {
            socket = initializer(factory
                    .createSocket(address)
                    .addListener(this)
                    .addHeader("Authorization", password)
                    .addHeader("User-Id", client.userId.toString())
                    .connectAsynchronously())
        } catch (exc: IOException) {
            LOGGER.error("Error when creating a WebSocket instance!", exc)
            throw exc // no way to recover from this.
        }
    }

    internal val eventBus = Flux.create<Any> {
        sink ->
        socket.addListener(object: WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket, text: String) {
                try {
                    val data = JsonIterator.deserialize(text)
                    val op = data["op"]!!.toString()
                    if (op == "dispatch")
                        sink.next(data)
                    else {
                        val handler = handlers[op]
                        handler?.let {
                            try {
                                it.invoke(websocket, data)
                            } catch (err: Throwable) {
                                LOGGER.error("Error when invoking the SocketHandler for OP: {} and Content: {}", op, text)
                            }
                            return
                        }
                        LOGGER.warn("Unhandled Response from the Basalt Server! OP: {}, Content: {}", op, text)
                    }
                } catch (err: Throwable) {
                    err.printStackTrace()
                    when (err) {
                        is JsonException -> LOGGER.error("JsonException | Error when deserializing Basalt Server JSON Response! Content: {}, Message: {}", text, err.message)
                        is KotlinNullPointerException -> LOGGER.error("KotlinNullPointerException | Missing Opcode Key from JSON Response! Content: {}, Message: {}", text, err.message)
                        else -> LOGGER.error("{} | Error when sending a JSON Event! Content: {}, Message: {}", err.javaClass.simpleName, text, err.message)
                    }
                    sink.error(err)
                }
            }
        })
    }

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        LOGGER.info("Connected!")

    }
    override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        LOGGER.info("Closing BasaltClient connection!")
    }

    override fun onError(socket: WebSocket, error: WebSocketException) {
        LOGGER.error("Error thrown in the WebSocket Handler for Node URI: {} | Error Message: {}", address, error.message ?: "No message.")
    }

    override fun onConnectError(websocket: WebSocket, exception: WebSocketException) {
        LOGGER.error("Error when connecting to {}, Message: {}", address, exception.message)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioNode::class.java)
    }
}
package basalt.client.entities

import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.StreamSourceFrameChannel
import io.undertow.websockets.core.WebSocketChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// one handler for each connection to node so 1 per node


class WebSocketHandler internal constructor(val node: AudioNode): AbstractReceiveListener() {

    override fun onClose(webSocketChannel: WebSocketChannel, channel: StreamSourceFrameChannel) {
        super.onClose(webSocketChannel, channel)
    }

    override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
        super.onFullTextMessage(channel, message)
    }

    override fun onError(channel: WebSocketChannel, error: Throwable) {
        LOGGER.error("Error thrown in the WebSocketHandler for Node URI: {} | Error Message: {}", node.configuration.address, error.message ?: "No message.")
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    }
}
/*
   Copyright 2018 Sam Pritchard

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.github.samophis.basalt.client.entities

import com.github.samophis.basalt.client.entities.builders.SocketHandlerMap
import com.github.samophis.basalt.client.entities.messages.server.PlayerUpdate
import com.github.samophis.basalt.client.entities.messages.server.stats.StatsUpdate
import com.jsoniter.JsonIterator
import com.jsoniter.any.Any
import com.jsoniter.spi.JsonException
import com.neovisionaries.ws.client.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.io.IOException
import java.util.concurrent.TimeUnit

@Suppress("UNUSED")
class AudioNode internal constructor(val client: BasaltClient, val wsPort: Int, val baseInterval: Long,
                                     val maxInterval: Long, val intervalExpander: ((AudioNode, Long) -> Long),
                                     val intervalTimeUnit: TimeUnit, val factory: WebSocketFactory,
                                     val initializer: ((WebSocket) -> WebSocket), val password: String, val address: String,
                                     handlers: SocketHandlerMap): WebSocketAdapter() {

    private val handlers: SocketHandlerMap = SocketHandlerMap()
    val socket: WebSocket
    var statistics: Statistics? = null
        private set
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

        handlers["statsUpdate"] = {
            _, data ->
            val stats = JsonIterator.deserialize(data.toString(), StatsUpdate::class.java)
            if (statistics == null)
                statistics = Statistics()
            statistics?.apply {
                players = stats.players
                playingPlayers = stats.playingPlayers
                uptime = stats.uptime
                cores = stats.cpu.cores
                systemLoad = stats.cpu.systemLoad
                basaltLoad = stats.cpu.basaltLoad
                freeMemory = stats.memory.free
                usedMemory = stats.memory.used
                reservedMemory = stats.memory.reserved
                allocatedMemory = stats.memory.allocated
            }
        }

        handlers["playerUpdate"] = {
            _, data ->
            val update = JsonIterator.deserialize(data.toString(), PlayerUpdate::class.java)
            val player = client.getPlayerById(update.guildId.toLong())
            if (player != null) {
                LOGGER.debug("Player Update for Guild ID: {}, Position: {}, Timestamp: {}", update.guildId, update.position, update.timestamp)
                player.position = update.position
                player.timestamp = update.timestamp
            }
            else {
                LOGGER.warn("Player Update for non-existent player (Guild ID: {})", update.guildId)
            }
        }
        handlers.entries.forEach { this.handlers[it.key] = it.value }
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
        LOGGER.info("Connected to AudioNode: {} on Port: {}", websocket.uri.host, websocket.uri.port)
        client.internalPlayers.values.forEach {
            player ->
            player.node = this
        }
    }

    override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame, clientCloseFrame: WebSocketFrame, closedByServer: Boolean) {
        val closer = if (closedByServer) "server" else "client"
        LOGGER.info("Disconnected from AudioNode: {} by {}!", websocket.uri.host, closer)
        val best = client.bestNode
        client.internalPlayers.values.forEach {
            player ->
            if (player.node == this)
                player.node = best
        }
    }

    override fun onError(socket: WebSocket, error: WebSocketException) {
        LOGGER.error("Error thrown in the WebSocket Handler for AudioNode: {} | Error Message: {}", socket.uri.host, error.message ?: "No message.")
    }

    override fun onConnectError(websocket: WebSocket, exception: WebSocketException) {
        LOGGER.error("Error when connecting to the AudioNode at {}, Error Message: {}", websocket.uri.host, exception.message)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioNode::class.java)
    }
}
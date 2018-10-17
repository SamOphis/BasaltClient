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
import com.github.samophis.basalt.client.entities.events.*
import com.github.samophis.basalt.client.entities.messages.server.PlayerUpdate
import com.github.samophis.basalt.client.entities.messages.server.stats.StatsUpdate
import com.jsoniter.JsonIterator
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.http.WebsocketVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNUSED")
class AudioNode internal constructor(val client: BasaltClient, val wsPort: Int, val baseInterval: Long,
                                     val maxInterval: Long, val intervalExpander: ((AudioNode, Long) -> Long),
                                     val password: String, val address: String, handlers: SocketHandlerMap): AbstractVerticle() {

    private val handlers: SocketHandlerMap = SocketHandlerMap()
    var statistics: Statistics? = null
        private set

    private val closedByClient = AtomicBoolean(false)
    private val messageConsumers = AtomicReference<List<MessageConsumer<*>>>(null)
    private var httpClient: HttpClient? = null
    internal val isOpen = AtomicBoolean(false)

    init {
        handlers["statsUpdate"] = { _, data ->
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

        handlers["playerUpdate"] = { _, data ->
            val update = JsonIterator.deserialize(data.toString(), PlayerUpdate::class.java)
            val player = client.getPlayerById(update.guildId.toLong())
            if (player != null) {
                LOGGER.debug("Player Update for Guild ID: {}, Position: {}, Timestamp: {}", update.guildId, update.position, update.timestamp)
                player.position = update.position
                player.timestamp = update.timestamp
            } else {
                LOGGER.warn("Player Update for non-existent player (Guild ID: {})", update.guildId)
            }
        }
        handlers.entries.forEach { this.handlers[it.key] = it.value }
    }

    override fun start(startFuture: Future<Void>) {
        httpClient = vertx.createHttpClient()
        val cl = httpClient!!
        val map = MultiMap.caseInsensitiveMultiMap()
                .add("Authorization", password)
                .add("User-Id", client.userId.toString())
        cl.websocketAbs(address, map, WebsocketVersion.V13, null, { socket ->
            isOpen.set(true)
            val consumers = ArrayList<MessageConsumer<*>>()
            consumers.add(vertx.eventBus().consumer<String>("$address:ws-outgoing") {
                socket.writeTextMessage(it.body())
            })
            messageConsumers.set(consumers)
            socket.frameHandler { handleSocketFrame(socket, it) }
            socket.exceptionHandler(this::handleException)
            LOGGER.info("Connected to AudioNode: {} on Port: {}", address, wsPort)
            client.internalPlayers.valueCollection().forEach { player ->
                if (player.node != this)
                    player.node = this
                player.connect()
            }
        }) { err ->
            LOGGER.error("Error when creating a WebSocket instance! AudioNode: $address, Port: $wsPort", err)
            throw err // no way to recover from this
        }
        startFuture.complete()
    }

    override fun stop() {
        messageConsumers.get().forEach { it.unregister() }
        httpClient?.close()

    }

    private fun handleMessage(webSocket: WebSocket, msg: String) {
        val data = JsonIterator.deserialize(msg)
        val op = data["op"]?.toString() ?: throw UnsupportedOperationException("Missing opcode from JSON Data!")
        if (op == "dispatch") {
            vertx.eventBus().publish(data["key"]!!.toString(), data.toString())
            when (data["name"]?.toString()) {
                null -> throw UnsupportedOperationException("Missing name from JSON Data!")
                "ERROR" -> throw RuntimeException(data["data"]?.toString() ?: "No message.")
                "TRACK_STARTED" -> {
                    val player = client.getPlayerById(data["guildId"]!!.toLong())!!
                    player.fireEvent(TrackStartEvent(player, player.guildId,
                            client.trackUtil.decodeTrack(data["data"]!!["data"]!!.toString())))
                }
                "TRACK_ENDED" -> {
                    val raw = data["data"]!!
                    val player = client.getPlayerById(data["guildId"]!!.toLong())!!
                    player.fireEvent(TrackEndEvent(player, player.guildId,
                            client.trackUtil.decodeTrack(raw["track"]!!.toString()),
                            AudioTrackEndReason.valueOf(raw["reason"]!!["type"]!!.toString())))
                }
                "TRACK_EXCEPTION" -> {
                    val raw = data["data"]!!
                    val player = client.getPlayerById(data["guildId"]!!.toLong())!!
                    player.fireEvent(TrackExceptionEvent(player, player.guildId,
                            client.trackUtil.decodeTrack(raw["track"]!!.toString()),
                            raw["exception"]!!["message"]!!.toString(),
                            FriendlyException.Severity.valueOf(raw["exception"]["severity"]!!.toString())))
                }
                "TRACK_STUCK" -> {
                    val player = client.getPlayerById(data["guildId"]!!.toLong())!!
                    player.fireEvent(TrackStuckEvent(player, player.guildId,
                            client.trackUtil.decodeTrack(data["data"]!!["track"]!!.toString()),
                            data["data"]["thresholdMs"]!!.toLong()))
                }
                "PLAYER_PAUSED" -> {
                    val player = client.getPlayerById(data["guildId"]!!.toLong())!!
                    val event = if (data["data"]!!.toBoolean()) {
                        PlayerPauseEvent(player, player.guildId)
                    }
                    else {
                        PlayerResumeEvent(player, player.guildId)
                    }
                    player.fireEvent(event)
                }
            }
        } else {
            val handler = handlers[op]
            handler?.apply { invoke(webSocket, data); return }
            LOGGER.warn("Unhandled Response from Audio Node: {}, OP: {}, Content: {}", address, op, msg)
        }
    }

    private fun handleClose(closeCode: Int, closeReason: String?) {
        isOpen.set(false)
        if (closedByClient.get()) {
            LOGGER.info("Connection to AudioNode: {} closed by client for {}!", address, closeReason ?: "no specified reason")
        } else if (closeCode == 1000 || closeCode == 1001) {
            LOGGER.info("AudioNode: {} safely/gracefully closed the connection for {}!", address, closeReason ?: "no specified reason")
        } else {
            // todo add reconnect attempts
        }
        closedByClient.set(false)
        client.internalPlayers.valueCollection().forEach {
            player ->
            if (player.node == this)
                player.node = client.bestNode
            player.connect()
        }
    }

    private fun handleSocketFrame(webSocket: WebSocket, webSocketFrame: WebSocketFrame) = when {
            webSocketFrame.isText -> handleMessage(webSocket, webSocketFrame.textData())
            webSocketFrame.isClose -> handleClose(webSocketFrame.closeStatusCode().toInt(), webSocketFrame.closeReason())
            else -> {}
        }

    private fun handleException(err: Throwable) = LOGGER.error("Error during WebSocket communication!", err)

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioNode::class.java)
    }
}
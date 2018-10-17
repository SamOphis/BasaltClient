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

import com.github.samophis.basalt.client.entities.events.AudioEventListener
import com.github.samophis.basalt.client.entities.events.Event
import com.github.samophis.basalt.client.entities.messages.client.*
import com.github.samophis.basalt.client.entities.messages.server.tracks.AudioLoadResult
import com.jsoniter.JsonIterator
import com.jsoniter.any.Any
import com.jsoniter.output.JsonStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.vertx.core.Future

import me.escoffier.vertx.completablefuture.VertxCompletableFuture
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletionStage
import kotlin.collections.ArrayList
import kotlin.math.min

@Suppress("UNUSED")
class BasaltPlayer internal constructor(val client: BasaltClient, val guildId: Long) {
    @Volatile var sessionId: String? = null
        private set
    @Volatile var token: String? = null
        private set
    @Volatile var endpoint: String? = null
        private set
    @Volatile var state: State = State.NOT_CONNECTED
        private set

    @Volatile var node: AudioNode? = null
        set(value) {
            if (field == value)
                return
            field?.let {
                destroy()
            }
            if (value == null)
                state = State.NOT_CONNECTED
            else if (value.isOpen.get())
                state = State.CONNECTED
            field = value
        }

    @Volatile var playingTrack: AudioTrack? = null
        internal set

    @Volatile var timestamp: Long = 0
        internal set

    @Volatile var position: Long = 0
        get() {
            if (playingTrack == null) {
                LOGGER.warn("Player for Guild ID: {} is not playing anything (when attempting to get position).", guildId)
                throw IllegalStateException("Guild ID: $guildId | Non-existent AudioTrack!")
            }
            return if (paused)
                min(field, playingTrack!!.duration)
            else
                min(field + (System.currentTimeMillis() - timestamp), playingTrack!!.duration)
        }
        internal set

    @Volatile var paused: Boolean = false
        internal set

    @Volatile var volume: Int = 100
        private set

    private val _eventListeners = ArrayList<AudioEventListener>()
    val eventListeners: List<AudioEventListener>
        get() = Collections.unmodifiableList(_eventListeners)

    fun addEventListener(listener: AudioEventListener) = _eventListeners.add(listener)
    internal fun fireEvent(event: Event) {
        _eventListeners.forEach {
            it.onEvent(event)
        }
    }

    fun connect(sessionId: String? = this.sessionId, token: String? = this.token, endpoint: String? = this.endpoint): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to initialize a player for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state == State.INITIALIZED) {
            LOGGER.warn("Already connected and initialized on an AudioNode for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Already initialized!")
        }
        if (sessionId == null) {
            LOGGER.error("Session ID is null when initializing a player for Guild ID: {}", guildId)
            throw IllegalArgumentException("Guild ID: $guildId | Null Session ID!")
        }
        if (token == null) {
            LOGGER.error("Voice Token is null when initializing a player for Guild ID: {}", guildId)
            throw IllegalArgumentException("Guild ID: $guildId | Null Voice Token!")
        }
        if (endpoint == null) {
            LOGGER.error("Voice Server Endpoint is null when initializing a player for Guild ID: {}", guildId)
            throw IllegalArgumentException("Guild ID: $guildId | Null Voice Server Endpoint!")
        }
        this.sessionId = sessionId
        this.token = token
        this.endpoint = endpoint
        val key = "initialize${System.nanoTime()}"
        val request = InitializeRequest(key, guildId.toString(), sessionId, token, endpoint)
        val text = JsonStream.serialize(request)
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "INITIALIZED") {
                LOGGER.debug("Initialized player for Guild ID: {}", guildId)
                state = State.INITIALIZED
            } else {
                LOGGER.warn("Failed to initialize player for Guild ID: {}", guildId)
            }
        }
        sendData(text)
        return future
    }

    fun loadIdentifiers(vararg identifiers: String): CompletionStage<List<AudioLoadResult>> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to load tracks, from Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized!", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val best = client.bestNode
        if (best != node) {
            node = best
            return connect().thenCompose { any ->
                if (any["name"]?.toString() == "ERROR")
                    throw RuntimeException(any["data"]?.toString() ?: "No message.")

                val key = "loadIdentifiers${System.nanoTime()}"
                val request = LoadIdentifiersRequest(key, *identifiers)
                val text = JsonStream.serialize(request)

                val future = Future.future<List<AudioLoadResult>>()
                val list = ArrayList<AudioLoadResult>()
                val consumer = client.eventBus.consumer<Any>(key)
                consumer.handler { msg ->
                    val body = msg.body()
                    when (body["name"]?.toString()) {
                        null -> throw UnsupportedOperationException("Missing name from JSON Response!")
                        "LOAD_IDENTIFIERS_CHUNK" -> {
                            LOGGER.debug("Received identifier load chunk for Guild ID: {}", guildId)
                            body.get("data").forEach {
                                list.add(JsonIterator.deserialize(it.toString(), AudioLoadResult::class.java))
                            }
                        }
                        "CHUNKS_FINISHED" -> {
                            LOGGER.debug("Requested identifiers fully loaded for Guild ID: {}", guildId)
                            consumer.unregister()
                            future.complete(list)
                        }
                    }
                }
                sendData(text)
                VertxCompletableFuture.from(client.vertx, future)
            }
        }
        val key = "loadIdentifiers${System.nanoTime()}"
        val request = LoadIdentifiersRequest(key, *identifiers)
        val text = JsonStream.serialize(request)

        val future = Future.future<List<AudioLoadResult>>()
        val list = ArrayList<AudioLoadResult>()
        val consumer = client.eventBus.consumer<String>(key)
        consumer.handler { msg ->
            val body = JsonIterator.deserialize(msg.body())
            when (body["name"]?.toString()) {
                null -> throw UnsupportedOperationException("Missing name from JSON Response!")
                "LOAD_IDENTIFIERS_CHUNK" -> {
                    LOGGER.debug("Received identifier load chunk for Guild ID: {}", guildId)
                    body.get("data").forEach {
                        list.add(JsonIterator.deserialize(it.toString(), AudioLoadResult::class.java))
                    }
                }
                "CHUNKS_FINISHED" -> {
                    LOGGER.debug("Requested identifiers fully loaded for Guild ID: {}", guildId)
                    consumer.unregister()
                    future.complete(list)
                }
            }
        }
        sendData(text)
        return VertxCompletableFuture.from(client.vertx, future)
    }

    fun playTrack(track: AudioTrack, startTime: Long? = 0): CompletionStage<Any> = playTrack(client.trackUtil.encodeTrack(track), startTime)
    fun playTrack(track: String, startTime: Long? = 0): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to play audio to Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized!", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val best = client.bestNode
        if (best != node) {
            node = best
            return connect()
                    .thenCompose {
                        any ->
                        if (any["name"]?.toString() == "ERROR")
                            throw RuntimeException(any["data"]?.toString() ?: "No message.")
                        val key = "playTracks${System.nanoTime()}"
                        val request = PlayRequest(key, guildId.toString(), track, startTime)
                        val future = wrapFuture(key) { data ->
                            if (data["name"]?.toString() == "TRACK_STARTED") {
                                LOGGER.debug("Started track for Guild ID: {}", guildId)
                                return@wrapFuture
                            }
                            LOGGER.warn("Failed to start track for Guild ID: {}, JSON Content: {}", data.toString())
                        }
                        sendData(JsonStream.serialize(request))
                        future
                    }
        }
        val key = "playTracks${System.nanoTime()}"
        val request = PlayRequest(key, guildId.toString(), track, startTime)
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "TRACK_STARTED") {
                LOGGER.debug("Started track for Guild ID: {}", guildId)
                return@wrapFuture
            }
            LOGGER.warn("Failed to start track for Guild ID: {}, JSON Content: {}", data.toString())
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    fun stopTrack(): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to stop the current track for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized!", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val key = "stopTrack${System.nanoTime()}"
        val request = EmptyRequest(key, "stop", guildId.toString())
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "TRACK_ENDED") {
                LOGGER.debug("Successfully stopped audio playback for Guild ID: {}", guildId)
                return@wrapFuture
            }
            LOGGER.warn("Failed to stop audio playback for Guild ID: {}", guildId)
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    fun destroy(): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to destroy the player for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        when (state) {
            State.NOT_CONNECTED -> {
                LOGGER.warn("Not connected to an AudioNode for Guild ID: {}", guildId)
                throw IllegalStateException("Guild ID: $guildId | Not connected!")
            }
            State.CONNECTED -> {
                LOGGER.warn("Player for Guild ID: {} is uninitialized!", guildId)
                throw IllegalStateException("Guild ID: $guildId | Not initialized!")
            }
            else -> {}
        }
        val key = "destroy${System.nanoTime()}"
        val request = EmptyRequest(key, "destroy", guildId.toString())
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "DESTROYED") {
                LOGGER.debug("Destroyed player for Guild ID: {}", guildId)
                state = State.CONNECTED
                return@wrapFuture
            }
            LOGGER.warn("Failed to destroy player for Guild ID: {}, JSON Content: {}", guildId, data.toString())
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    fun seek(position: Long): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to seek to a new position for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized!", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val key = "seek${System.nanoTime()}"
        val request = SeekRequest(key, guildId.toString(), position)
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "POSITION_UPDATE") {
                LOGGER.debug("Successfully seeked to Position: {}ms for Guild ID: {}", position, guildId)
                this.position = position
                return@wrapFuture
            }
            LOGGER.warn("Failed to seek for Guild ID: {}, JSON Content: {}", guildId, data.toString())
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    fun setVolume(volume: Int): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to set the volume for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized!", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val key = "volume${System.nanoTime()}$volume"
        val request = SetVolumeRequest(key, guildId.toString(), volume)

        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "VOLUME_UPDATE") {
                LOGGER.debug("Successfully set the volume to {} for Guild ID: {}", volume, guildId)
                this.volume = volume
                return@wrapFuture
            }
            LOGGER.warn("Failed to set volume for Guild ID: {}, JSON Content: {}", guildId, data.toString())
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    fun pause(): CompletionStage<Any> = setPaused0(true)
    fun resume(): CompletionStage<Any> = setPaused0(false)
    private fun setPaused0(paused: Boolean): CompletionStage<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to pause/resume audio for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.INITIALIZED) {
            LOGGER.warn("Player for Guild ID: {} uninitialized", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not initialized!")
        }
        val key = "setPaused${System.nanoTime()}$paused"
        val request = SetPausedRequest(key, guildId.toString(), paused)
        val future = wrapFuture(key) { data ->
            if (data["name"]?.toString() == "PLAYER_PAUSED") {
                LOGGER.debug("Successfully paused/resumed audio for Guild ID: {}", guildId)
                val pause = data["data"]?.toBoolean()
                if (pause != null) {
                    this.paused = pause
                    return@wrapFuture
                }
            }
            LOGGER.warn("Failed to pause/resume audio for Guild ID: {}, JSON Content: {}", guildId, data.toString())
        }
        sendData(JsonStream.serialize(request))
        return future
    }

    private fun wrapFuture(address: String, handler: (Any) -> Unit): CompletionStage<Any> {
        val consumer = client.eventBus.consumer<String>(address)
        val future = Future.future<Any>()
        consumer.handler { msg ->
            val body = JsonIterator.deserialize(msg.body())
            handler(body)
            consumer.unregister()
            future.complete(body)
        }
        return VertxCompletableFuture.from(client.vertx, future)
    }

    private fun sendData(data: String) {
        val node = node!!
        client.eventBus.publish("${node.address}:ws-outgoing", data)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltPlayer::class.java)
    }
}
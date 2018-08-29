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

package basalt.client.entities

import basalt.client.entities.messages.client.*
import basalt.client.entities.messages.server.tracks.AudioLoadResult
import com.jsoniter.JsonIterator
import com.jsoniter.any.Any
import com.jsoniter.output.JsonStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
            if (field != null) {
                // todo destroy player
            }
            if (value?.socket?.isOpen == true) {
                // todo initialize
            }
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

    fun connect(sessionId: String? = this.sessionId, token: String? = this.token, endpoint: String? = this.endpoint): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to initialize a player for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state == State.CONNECTED) {
            LOGGER.warn("Already connected to an AudioNode for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Already connected!")
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
        val node = node!!
        node.socket.sendText(text)
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    if (it["name"]?.toString() == "INITIALIZED") {
                        LOGGER.debug("Initialized the player for Guild ID: {}", guildId)
                        state = State.CONNECTED
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to initialize player for Guild ID: {}", guildId)
                }
                .next()
    }

    fun loadIdentifiers(vararg identifiers: String): Flux<AudioLoadResult> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to load tracks, from Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not connected/destroyed!")
        }
        val node = node!!
        val key = "loadIdentifiers${System.nanoTime()}"
        val request = LoadIdentifiersRequest(key, *identifiers)
        val text = JsonStream.serialize(request)
        node.socket.sendText(text)
        return node.eventBus
                .filter { it["key"]?.toString() == key && it["name"]?.toString() == "LOAD_IDENTIFIERS_CHUNK" }
                .flatMapIterable {
                    any ->
                    // no chance of error here
                    LOGGER.debug("Received identifier load chunk for Guild ID: {}", guildId)
                    val data = any.get("data")
                    val list = ObjectArrayList<AudioLoadResult>(data.size())
                    data.forEach { list.add(JsonIterator.deserialize(it.toString(), AudioLoadResult::class.java)) }
                    list
                }

    }

    fun playTrack(track: AudioTrack, startTime: Long? = 0): Mono<Any> = playTrack(client.trackUtil.encodeTrack(track), startTime)
    fun playTrack(track: String, startTime: Long? = 0): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to play audio to Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not connected/destroyed!")
        }
        val node = node!!
        val key = "playTracks${System.nanoTime()}"
        val request = PlayRequest(key, guildId.toString(), track, startTime)
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "TRACK_STARTED") {
                        LOGGER.debug("Started track for Guild ID: {}", guildId)
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to start track for Guild ID: {}", guildId)
                }
                .next()
    }

    fun stopTrack(): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to stop the current track for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not connected/destroyed!")
        }
        val node = node!!
        val key = "stopTrack${System.nanoTime()}"
        val request = EmptyRequest("stop", guildId.toString())
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "TRACK_ENDED") {
                        LOGGER.debug("Successfully stopped playing audio for Guild ID: {}", guildId)
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to stop the player for Guild ID: {}", guildId)
                }
                .next()
    }

    fun destroy(): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to destroy the player for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        when (state) {
            State.NOT_CONNECTED -> {
                LOGGER.warn("Not connected to an AudioNode for Guild ID: {}", guildId)
                throw IllegalStateException("Guild ID: $guildId | Not connected!")
            }
            State.DESTROYED -> {
                LOGGER.warn("Player for Guild ID: {} is already destroyed!", guildId)
                throw IllegalStateException("Guild ID: $guildId | Already destroyed!")
            }
            else -> {}
        }
        val node = node!!
        val key = "destroy${System.nanoTime()}"
        val request = EmptyRequest("destroy", guildId.toString())
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "DESTROYED") {
                        LOGGER.debug("Destroyed player for Guild ID: {}", guildId)
                        state = State.DESTROYED
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to destroy player for Guild ID: {}, JSON Content: {}", guildId, any.toString())
                }
                .next()
    }

    fun seek(position: Long): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to seek to a new position for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not connected/destroyed!")
        }
        val node = node!!
        val key = "seek${System.nanoTime()}$position"
        val request = SeekRequest(key, guildId.toString(), position)
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "POSITION_UPDATE") {
                        LOGGER.debug("Successfully seeked to Position: {} for Guild ID: {}", position, guildId)
                        this.position = any["data"]?.toLong() ?: position
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to seek for Guild ID: {}, JSON Content: {}", guildId, any.toString())
                }
                .next()
    }

    fun setVolume(volume: Int): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to set the volume for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Not connected/destroyed!")
        }
        val node = node!!
        val key = "volume${System.nanoTime()}$volume"
        val request = SetVolumeRequest(key, guildId.toString(), volume)
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "VOLUME_UPDATE") {
                        LOGGER.debug("Successfully set the volume to {} for Guild ID: {}", volume, guildId)
                        this.volume = volume
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to set volume for Guild ID: {}, JSON Content: {}", guildId, any.toString())
                }
                .next()
    }

    fun pause(): Mono<Any> = setPaused0(true)
    fun resume(): Mono<Any> = setPaused0(false)
    private fun setPaused0(paused: Boolean): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to pause/resume audio for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (state != State.CONNECTED) {
            LOGGER.warn("Not connected/initialized for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        val node = node!!
        val key = "setPaused${System.nanoTime()}$paused"
        val request = SetPausedRequest(key, guildId.toString(), paused)
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .doOnNext {
                    any ->
                    if (any["name"]?.toString() == "PLAYER_PAUSED") {
                        LOGGER.debug("Successfully paused/resumed audio for Guild ID: {}", guildId)
                        this.paused = any["data"]?.toBoolean() ?: this.paused
                        return@doOnNext
                    }
                    LOGGER.warn("Failed to pause/resume audio for Guild ID: {}, JSON Content: {}", guildId, any.toString())
                }
                .next()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltPlayer::class.java)
    }
}
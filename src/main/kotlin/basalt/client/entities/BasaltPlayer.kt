package basalt.client.entities

import basalt.client.entities.messages.client.EmptyRequest
import basalt.client.entities.messages.client.InitializeRequest
import basalt.client.entities.messages.client.LoadTracksRequest
import basalt.client.entities.messages.client.PlayRequest
import basalt.client.entities.messages.server.tracks.AudioLoadResult
import com.jsoniter.JsonIterator
import com.jsoniter.any.Any
import com.jsoniter.output.JsonStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

    @Volatile var position: Long = 0
        internal set

    @Volatile var paused: Boolean = false
        internal set

    fun connect(sessionId: String? = this.sessionId, token: String? = this.token, endpoint: String? = this.endpoint): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to initialize a player for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
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
                .doOnNext { state = State.CONNECTED }
                .next()
    }

    fun loadTracks(vararg identifiers: String): Flux<AudioLoadResult> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to load tracks, from Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        val node = node!!
        val key = "loadTracks${System.nanoTime()}"
        val request = LoadTracksRequest(key, *identifiers)
        val text = JsonStream.serialize(request)
        node.socket.sendText(text)
        return node.eventBus
                .filter { it["key"]?.toString() == key && it["name"]?.toString() == "LOAD_TRACK_CHUNK" }
                .flatMapIterable {
                    any ->
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
        val node = node!!
        val key = "playTracks${System.nanoTime()}"
        val request = PlayRequest(key, guildId.toString(), track, startTime)
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
                .next()
    }

    fun stopTrack(): Mono<Any> {
        if (node == null) {
            LOGGER.error("Node is null when attempting to stop the current track for Guild ID: {}", guildId)
            throw IllegalStateException("Guild ID: $guildId | Null AudioNode!")
        }
        if (playingTrack == null) {
            LOGGER.warn("No track is currently playing for Guild ID: {}", guildId)
            return Mono.empty()
        }
        val node = node!!
        val key = "stopTrack${System.nanoTime()}"
        val request = EmptyRequest("stop", guildId.toString())
        node.socket.sendText(JsonStream.serialize(request))
        return node.eventBus
                .filter { it["key"]?.toString() == key }
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
                .next()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltPlayer::class.java)
    }
}
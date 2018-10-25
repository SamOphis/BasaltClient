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

import com.github.samophis.basalt.client.util.AudioTrackUtil
import com.jsoniter.JsonIterator
import com.jsoniter.output.EncodingMode
import com.jsoniter.output.JsonStream
import com.jsoniter.spi.DecodingMode
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

typealias AudioNodeMap = HashMap<String, AudioNode>
typealias AudioNodeList = ArrayList<AudioNode>

typealias PlayerMap = Map<String, BasaltPlayer>
typealias PlayerHashMap = HashMap<String, BasaltPlayer>
typealias PlayerList = ArrayList<BasaltPlayer>

@Suppress("UNUSED")
class BasaltClient internal constructor(val defaultWsPort: Int, val defaultBaseInterval: Long, val defaultMaxInterval: Long,
                                        val defaultIntervalTimeUnit: TimeUnit, val defaultIntervalExpander: ((AudioNode, Long) -> Long),
                                        val defaultNodePassword: String, val userId: Long, val vertxOptions: VertxOptions) {
    private val _nodes = AudioNodeMap()
    val audioNodes: Map<String, AudioNode>
        get() = Collections.unmodifiableMap(_nodes)

    val audioNodeList: List<AudioNode>
        get() {
            val list = AudioNodeList(_nodes.size)
            list.addAll(_nodes.values)
            return Collections.unmodifiableList(list)
        }

    internal val vertx = Vertx.vertx(vertxOptions)
    internal val eventBus = vertx.eventBus()

    internal val internalPlayers = PlayerHashMap()
    val players: PlayerMap
        get() = Collections.unmodifiableMap(internalPlayers)

    val playerList: List<BasaltPlayer>
        get() {
            val list = PlayerList(internalPlayers.size)
            list.addAll(internalPlayers.values)
            return Collections.unmodifiableList(list)
        }

    val bestNode: AudioNode?
        get() {
            var node: AudioNode? = null
            var record: Int = Integer.MAX_VALUE
            for (value in _nodes.values) {
                val penalty = value.statistics?.getTotalPenalty(value) ?: Integer.MAX_VALUE - 1
                if (penalty < record) {
                    node = value
                    record = penalty
                }
            }
            return node
        }

    val trackUtil = AudioTrackUtil()

    fun addAudioNodes(vararg nodes: AudioNode) = nodes.forEach {
        node ->
        vertx.deployVerticle(node) {
            _nodes[node.address] = node
        }
    }
    fun removeAudioNodes(vararg nodes: AudioNode) = nodes.forEach {
        node ->
        node.stop()
        _nodes.remove(node.address)
    }

    fun getPlayerById(guildId: String): BasaltPlayer? = internalPlayers[guildId]
    fun newPlayer(guildId: String, node: AudioNode? = bestNode): BasaltPlayer {
        if (internalPlayers.containsKey(guildId)) {
            LOGGER.warn("Cannot create duplicate players for Guild ID: {}", guildId)
            throw IllegalArgumentException(guildId)
        }
        val player = BasaltPlayer(this, guildId)
        player.node = node
        internalPlayers[guildId] = player
        return player
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClient::class.java)
        init {
            JsonStream.setMode(EncodingMode.DYNAMIC_MODE)
            JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH)
        }
    }
}
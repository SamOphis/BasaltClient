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

import basalt.client.util.AudioTrackUtil
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

typealias AudioNodeMap = Object2ObjectOpenHashMap<String, AudioNode>
typealias AudioNodeList = ObjectArrayList<AudioNode>

typealias PlayerMap = Long2ObjectMap<BasaltPlayer>
typealias PlayerHashMap = Long2ObjectOpenHashMap<BasaltPlayer>
typealias PlayerList = ObjectArrayList<BasaltPlayer>

@Suppress("UNUSED")
class BasaltClient internal constructor(val defaultWsPort: Int, val defaultBaseInterval: Long, val defaultMaxInterval: Long,
                                        val defaultIntervalTimeUnit: TimeUnit, val defaultIntervalExpander: ((AudioNode, Long) -> Long),
                                        val defaultNodePassword: String, val userId: Long) {
    private val _nodes = AudioNodeMap()
    val audioNodes: Map<String, AudioNode>
        get() = Object2ObjectMaps.unmodifiable(_nodes)

    val audioNodeList: List<AudioNode>
        get() {
            val list = AudioNodeList(_nodes.size)
            list.addAll(_nodes.values)
            return ObjectLists.unmodifiable(list)
        }

    internal val internalPlayers = PlayerHashMap()
    val players: PlayerMap
        get() = Long2ObjectMaps.unmodifiable(internalPlayers)

    val playerList: List<BasaltPlayer>
        get() {
            val list = PlayerList(internalPlayers.size)
            list.addAll(internalPlayers.values)
            return ObjectLists.unmodifiable(list)
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

    fun addAudioNodes(vararg nodes: AudioNode) = nodes.forEach { _nodes[it.address] = it }
    fun removeAudioNodes(vararg nodes: AudioNode) = nodes.forEach { _nodes.remove(it.address) }

    fun getPlayerById(guildId: Long): BasaltPlayer? = internalPlayers[guildId]
    fun newPlayer(guildId: Long, node: AudioNode? = bestNode): BasaltPlayer {
        if (internalPlayers.containsKey(guildId)) {
            LOGGER.warn("Cannot create duplicate players for Guild ID: {}", guildId)
            throw IllegalArgumentException(guildId.toString())
        }
        val player = BasaltPlayer(this, guildId)
        player.node = node
        internalPlayers[guildId] = player
        return player
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClient::class.java)
    }
}
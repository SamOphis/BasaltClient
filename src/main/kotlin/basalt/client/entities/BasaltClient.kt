package basalt.client.entities

import basalt.client.util.AudioTrackUtil
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

typealias AudioNodeMap = Object2ObjectOpenHashMap<String, AudioNode>
typealias AudioNodeList = ObjectArrayList<AudioNode>

typealias PlayerMap = Long2ObjectOpenHashMap<BasaltPlayer>
typealias PlayerList = ObjectArrayList<BasaltPlayer>

@Suppress("UNUSED")
class BasaltClient internal constructor(val defaultWsPort: Int, val defaultBaseInterval: Long, val defaultMaxInterval: Long,
                                        val defaultIntervalTimeUnit: TimeUnit, val defaultIntervalExpander: ((AudioNode, Long) -> Long),
                                        val defaultNodePassword: String, val userId: Long) {
    private val _nodes = AudioNodeMap()
    val audioNodes
        get() = Object2ObjectMaps.unmodifiable(_nodes)

    val audioNodeList: List<AudioNode>
        get() {
            val list = AudioNodeList(_nodes.size)
            list.addAll(_nodes.values)
            return ObjectLists.unmodifiable(list)
        }

    private val _players = PlayerMap()
    val players
        get() = Long2ObjectMaps.unmodifiable(_players)

    val playerList: List<BasaltPlayer>
        get() {
            val list = PlayerList(_players.size)
            list.addAll(_players.values)
            return ObjectLists.unmodifiable(list)
        }

    val bestNode: AudioNode?
        get() {
            // todo load balancing
            return _nodes.values.first()
        }

    val trackUtil = AudioTrackUtil()

    fun addAudioNodes(vararg nodes: AudioNode) = nodes.forEach { _nodes[it.address] = it }
    fun removeAudioNodes(vararg nodes: AudioNode) = nodes.forEach { _nodes.remove(it.address) }

    fun getPlayerById(guildId: Long): BasaltPlayer? = _players[guildId]
    fun newPlayer(guildId: Long, node: AudioNode? = bestNode): BasaltPlayer {
        if (_players.containsKey(guildId)) {
            LOGGER.warn("Cannot create duplicate players for Guild ID: {}", guildId)
            throw IllegalArgumentException(guildId.toString())
        }
        val player = BasaltPlayer(this, guildId)
        player.node = node
        _players[guildId] = player
        return player
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClient::class.java)
    }
}
package basalt.client.entities

import io.undertow.Undertow
import it.unimi.dsi.fastutil.objects.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

typealias NodeMap = Object2ObjectOpenHashMap<String, AudioNode>
typealias NodeMapInterface = Object2ObjectMap<String, AudioNode>
typealias AudioNodeList = ObjectArrayList<AudioNode>

@Suppress("UNUSED")
class BasaltClient(val configuration: BasaltConfiguration) {
    private val _nodes = NodeMap()
    val audioNodes: NodeMapInterface
        get() = Object2ObjectMaps.unmodifiable(_nodes)

    private val undertow = configuration.undertowBuilder

    private val isRunning = AtomicBoolean(false)

    fun getAudioNodeList(): List<AudioNode> {
        val list = AudioNodeList(_nodes.size)
        _nodes.values.forEach { list.add(it) }
        return ObjectLists.unmodifiable(list)
    }

    fun addAudioNodes(vararg nodes: AudioNode) {
        if (!isRunning.get()) {
            nodes.forEach { _nodes[it.configuration.address] = it }
            return
        }
        LOGGER.warn("Cannot add,")
    }

    fun removeAudioNodes(vararg nodes: AudioNode) {
        nodes.forEach { _nodes.remove(it.configuration.address) }
    }

    fun getBestNode(): AudioNode {
        // todo load balancing
        return _nodes["ws://localhost:5555"]!!
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClient::class.java)
    }
}
package basalt.client.jda

import basalt.client.entities.BasaltClient
import basalt.client.entities.builders.BasaltClientBuilder
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.dv8tion.jda.core.JDABuilder
import org.slf4j.LoggerFactory

class BasaltClientManager private constructor() {
    companion object {
        private val CLIENTS = Long2ObjectOpenHashMap<BasaltClient>()
        private val LOGGER = LoggerFactory.getLogger(BasaltClientManager::class.java)
        private var isShutdown = false

        fun buildClient(userId: Long): BasaltClient {
            val client = with (BasaltClientBuilder()) {
                this.userId = userId
                build()
            }
            CLIENTS[userId] = client
            return client
        }

        fun addClient(client: BasaltClient): BasaltClient {
            CLIENTS[client.userId] = client
            return client
        }

        fun initShard(client: BasaltClient, builder: JDABuilder): JDABuilder = builder.addEventListener(ShardInitializer(client))
        // todo more
    }
}
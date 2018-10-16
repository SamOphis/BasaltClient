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

package com.github.samophis.basalt.client.jda

import com.github.samophis.basalt.client.entities.BasaltClient
import com.github.samophis.basalt.client.entities.builders.BasaltClientBuilder
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.JDABuilder
import org.slf4j.LoggerFactory

class BasaltClientManager private constructor() {
    companion object {
        private val CLIENTS = TLongObjectHashMap<BasaltClient>()
        private val LOGGER = LoggerFactory.getLogger(BasaltClientManager::class.java)
        private var isShutdown = false

        fun buildClient(userId: Long): BasaltClient {
            val client = with (BasaltClientBuilder()) {
                this.userId = userId
                build()
            }
            CLIENTS.put(userId, client)
            return client
        }

        fun addClient(client: BasaltClient): BasaltClient {
            CLIENTS.put(client.userId, client)
            return client
        }

        fun initShard(client: BasaltClient, builder: JDABuilder): JDABuilder = builder.addEventListener(ShardInitializer(client))
        // todo more
    }
}
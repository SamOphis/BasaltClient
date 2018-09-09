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
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.handle.SocketHandler
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VoiceServerInterceptor(private val client: BasaltClient, jda: JDAImpl): SocketHandler(jda) {
    override fun handleInternally(content: JSONObject): Long? {
        LOGGER.debug(content.toString())
        val id = content.getLong("guild_id")
        val jda = api.get()
        if (jda == null) {
            LOGGER.error("JDA API reference garbage collected? wtf this should never happen. Guild ID: {}", id)
            throw IllegalStateException("JDA API reference garbage collected! This should never happen normally! Guild ID: $id")
        }
        if (jda.guildLock.isLocked(id))
            return id
        val guild = jda.getGuildById(id)
        if (guild == null) {
            LOGGER.error("Non-existent Guild with ID: {}", id)
            throw IllegalStateException("Guild ID: $id | Guild is non-existent!")
        }
        val self = guild.selfMember
        val state = self.voiceState
        if (state == null) {
            LOGGER.error("Non-existent VoiceState for Member ID: {} and Guild ID: {}", self.user.id, guild.id)
            throw IllegalStateException("Member ID: ${self.user.id}, Guild ID: ${guild.id} | VoiceState is non-existent!")
        }
        val ignoredPlayer = client.getPlayerById(id)
        if (ignoredPlayer == null) {
            client.newPlayer(id)
                    .connect(state.sessionId, content.getString("token"), content.getString("endpoint"))
                    .subscribe({ LOGGER.debug("Connected to Basalt!") }) { LOGGER.error("Error when connecting to Basalt!", it) }
        }
        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(VoiceServerInterceptor::class.java)
    }
}
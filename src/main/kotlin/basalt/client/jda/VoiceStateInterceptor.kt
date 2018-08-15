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

package basalt.client.jda

import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.handle.VoiceStateUpdateHandler
import org.json.JSONObject

class VoiceStateInterceptor(jda: JDAImpl): VoiceStateUpdateHandler(jda) {
    override fun handleInternally(content: JSONObject): Long? {
        val id = if (content.has("guild_id")) content.getLong("guild_id") else null
        when {
            id != null && api.guildLock.isLocked(id) -> return id
            id == null -> return super.handleInternally(content)
        }
        val guild = api.getGuildById(id!!) ?: return super.handleInternally(content)
        val userId = content.getLong("user_id")
        val member = guild.getMemberById(userId) ?: return super.handleInternally(content)
        if (member != guild.selfMember)
            return super.handleInternally(content)
        val channelId = if (!content.isNull("channel_id")) content.getLong("channel_id") else null
        api.client.updateAudioConnection(id, guild.getVoiceChannelById(channelId ?: 0))
        return super.handleInternally(content)
    }
}
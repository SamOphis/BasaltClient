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
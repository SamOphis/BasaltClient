package basalt.client.jda

import basalt.client.entities.BasaltClient
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.handle.SocketHandler
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VoiceServerInterceptor(private val client: BasaltClient, jda: JDAImpl): SocketHandler(jda) {
    override fun handleInternally(content: JSONObject): Long? {
        LOGGER.debug(content.toString())
        val id = content.getLong("guild_id")
        if (api.guildLock.isLocked(id))
            return id
        val guild = api.getGuildById(id)
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
        client.newPlayer(id)
                .connect(state.sessionId, content.getString("token"), content.getString("endpoint"))
                .subscribe({ LOGGER.debug("Connected to Basalt!") }) { LOGGER.error("Error when connecting to Basalt!", it) }
        return null
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(VoiceServerInterceptor::class.java)
    }
}
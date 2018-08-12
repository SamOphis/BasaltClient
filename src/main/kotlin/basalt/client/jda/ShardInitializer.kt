package basalt.client.jda

import basalt.client.entities.BasaltClient
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class ShardInitializer(private val client: BasaltClient): ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        val impl = event.jda as JDAImpl
        val handlers = impl.client.handlers
        handlers["VOICE_SERVER_UPDATE"] = VoiceServerInterceptor(client, impl)
        handlers["VOICE_STATE_UPDATE"] = VoiceStateInterceptor(impl)
    }
}
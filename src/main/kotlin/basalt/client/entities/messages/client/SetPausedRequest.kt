package basalt.client.entities.messages.client

@Suppress("UNUSED")
class SetPausedRequest internal constructor(val key: String, val guildId: String, val paused: Boolean) {
    val op = "setPaused"
}
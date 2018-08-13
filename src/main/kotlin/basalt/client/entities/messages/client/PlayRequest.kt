package basalt.client.entities.messages.client

@Suppress("UNUSED")
class PlayRequest internal constructor(val key: String, val guildId: String, val track: String, val startTime: Long? = null) {
    val op = "play"
}
package basalt.client.entities.messages.client

@Suppress("UNUSED")
class SeekRequest internal constructor(val key: String, val guildId: String, val position: Long) {
    val op = "seek"
}
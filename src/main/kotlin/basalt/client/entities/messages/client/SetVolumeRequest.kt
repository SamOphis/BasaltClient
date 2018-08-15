package basalt.client.entities.messages.client

@Suppress("UNUSED")
class SetVolumeRequest internal constructor(val key: String, val guildId: String, val volume: Int) {
    val op = "volume"
}
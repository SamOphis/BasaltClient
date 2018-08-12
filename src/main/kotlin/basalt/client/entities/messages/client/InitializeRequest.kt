package basalt.client.entities.messages.client

@Suppress("UNUSED")
class InitializeRequest internal constructor(val key: String, val guildId: String, val sessionId: String, val token: String, val endpoint: String) {
    val op = "initialize"
}
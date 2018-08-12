package basalt.client.entities.messages.client

@Suppress("UNUSED")
class LoadTracksRequest internal constructor(val key: String, vararg val identifiers: String) {
    val op = "load"
}
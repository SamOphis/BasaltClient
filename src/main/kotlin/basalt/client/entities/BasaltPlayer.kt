package basalt.client.entities

class BasaltPlayer internal constructor(val client: BasaltClient, val guildId: String) {
    @Volatile var session_id: String? = null
        private set
    @Volatile var token: String? = null
        private set
    @Volatile var endpoint: String? = null
        private set
    @Volatile var state: State = State.NOT_CONNECTED
        private set

    @Volatile var node: AudioNode? = null
        set(value) {
            if (field != null) {
                // todo destroy player
            }
            if (value?.socket?.isOpen == true) {
                // todo initialize
            }

        }
}
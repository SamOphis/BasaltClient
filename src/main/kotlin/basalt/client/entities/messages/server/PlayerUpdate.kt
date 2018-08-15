package basalt.client.entities.messages.server

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class PlayerUpdate @JsonCreator constructor(@JsonProperty("op", required = true, nullable = false) val op: String,
                                            @JsonProperty("guildId", required = true, nullable = false) val guildId: String,
                                            @JsonProperty("position", required = true, nullable = false) val position: Long,
                                            @JsonProperty("timestamp", required = true, nullable = false) val timestamp: Long)
package basalt.client.entities.messages.server

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class DispatchResponse @JsonCreator constructor(@JsonProperty("op", required = true, nullable = false) val op: String,
                                                @JsonProperty("name", required = true, nullable = false) val name: String,
                                                @JsonProperty("guildId", required = true, nullable = true) val guildId: String?,
                                                @JsonProperty("data", required = true, nullable = true) val data: Any?,
                                                @JsonProperty("seq", required = true, nullable = false) val seq: Long)

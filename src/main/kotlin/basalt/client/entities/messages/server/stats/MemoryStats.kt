package basalt.client.entities.messages.server.stats

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class MemoryStats @JsonCreator constructor(@JsonProperty("free", required = true, nullable = false) val free: Long,
                                           @JsonProperty("used", required = true, nullable = false) val used: Long,
                                           @JsonProperty("reserved", required = true, nullable = false) val reserved: Long,
                                           @JsonProperty("allocated", required = true, nullable = false) val allocated: Long)
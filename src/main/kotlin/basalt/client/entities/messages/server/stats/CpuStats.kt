package basalt.client.entities.messages.server.stats

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class CpuStats @JsonCreator constructor(@JsonProperty("cores", required = true, nullable = false) val cores: Int,
                                        @JsonProperty("systemLoad", required = true, nullable = false) val systemLoad: Double,
                                        @JsonProperty("basaltLoad", required = true, nullable = false) val basaltLoad: Double)
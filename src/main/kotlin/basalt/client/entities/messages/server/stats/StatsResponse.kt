package basalt.client.entities.messages.server.stats

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class StatsResponse @JsonCreator constructor(@JsonProperty("op", required = true, nullable = false) val op: String,
                                             @JsonProperty("players", required = true, nullable = false) val players: Int,
                                             @JsonProperty("playingPlayers", required = true, nullable = false) val playingPlayers: Int,
                                             @JsonProperty("uptime", required = true, nullable = false) val uptime: Long,
                                             @JsonProperty("memory", required = true, nullable = false) val memory: MemoryStats,
                                             @JsonProperty("cpu", required = true, nullable = false) val cpu: CpuStats)

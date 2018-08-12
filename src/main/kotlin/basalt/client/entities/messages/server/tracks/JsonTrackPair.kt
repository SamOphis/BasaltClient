package basalt.client.entities.messages.server.tracks

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class JsonTrackPair @JsonCreator constructor(@JsonProperty("data", required = true, nullable = false) val data: String,
                                             @JsonProperty("track", required = true, nullable = false) val track: JsonTrack)
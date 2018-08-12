package basalt.client.entities.messages.server.tracks

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class PlaylistInfo @JsonCreator constructor(@JsonProperty("name", required = true, nullable = false) val name: String,
                                            @JsonProperty("selectedTrack", required = true, nullable = false) val selectedTrack: Int)
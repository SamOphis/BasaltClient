package basalt.client.entities.messages.server.tracks

import basalt.client.entities.LoadType
import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class AudioLoadResult @JsonCreator constructor(@JsonProperty("loadType", required = true, nullable = false) loadType: String,
                                               @JsonProperty("playlistInfo") val playlistInfo: PlaylistInfo?,
                                               @JsonProperty("tracks", required = true, nullable = false) val tracks: Array<JsonTrackPair>) {
    val loadType = LoadType.from(loadType)
}
package basalt.client.entities.messages.server.tracks

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class JsonTrack @JsonCreator constructor(@JsonProperty("title", required = true, nullable = false) val title: String,
                                         @JsonProperty("author", required = true, nullable = false) val author: String,
                                         @JsonProperty("identifier", required = true, nullable = false) val identifier: String,
                                         @JsonProperty("uri", required = true, nullable = false) val uri: String,
                                         @JsonProperty("stream", required = true, nullable = false) val stream: Boolean,
                                         @JsonProperty("seekable", required = true, nullable = false) val seekable: Boolean,
                                         @JsonProperty("position", required = true, nullable = false) val position: Long,
                                         @JsonProperty("length", required = true, nullable = false) val length: Long)
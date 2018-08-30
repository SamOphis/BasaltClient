/*
   Copyright 2018 Sam Pritchard

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
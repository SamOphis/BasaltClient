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

package com.github.samophis.basalt.client.entities.messages.server.tracks

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
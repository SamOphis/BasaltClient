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

package com.github.samophis.basalt.client.entities.messages.server.stats

import com.jsoniter.annotation.JsonCreator
import com.jsoniter.annotation.JsonProperty

@Suppress("UNUSED")
class MemoryStats @JsonCreator constructor(@JsonProperty("free", required = true, nullable = false) val free: Long,
                                           @JsonProperty("used", required = true, nullable = false) val used: Long,
                                           @JsonProperty("reserved", required = true, nullable = false) val reserved: Long,
                                           @JsonProperty("allocated", required = true, nullable = false) val allocated: Long)
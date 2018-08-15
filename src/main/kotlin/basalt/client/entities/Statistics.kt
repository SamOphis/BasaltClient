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

package basalt.client.entities

@Suppress("UNUSED")
class Statistics internal constructor() {
    var players: Int = 0; internal set
    var playingPlayers: Int = 0; internal set
    var uptime: Long = 0; internal set
    var freeMemory: Long = 0; internal set
    var usedMemory: Long = 0; internal set
    var allocatedMemory: Long = 0; internal set
    var reservedMemory: Long = 0; internal set
    var cores: Int = 0; internal set
    var systemLoad: Double = 0.toDouble(); internal set
    var basaltLoad: Double = 0.toDouble(); internal set
}
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

package basalt.client.entities.builders

import basalt.client.entities.AudioNode
import basalt.client.entities.BasaltClient
import com.jsoniter.any.Any
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

typealias SocketHandler = ((WebSocket, Any) -> Unit)
typealias SocketHandlerMap = Object2ObjectOpenHashMap<String, SocketHandler>

class AudioNodeBuilder(): PlaceholderValues() {
    constructor(values: PlaceholderValues): this() {
        wsPort = values.wsPort
        baseInterval = values.baseInterval
        maxInterval = values.maxInterval
        intervalExpander = values.intervalExpander
        intervalTimeUnit = values.intervalTimeUnit
        nodePassword = values.nodePassword
    }

    private var innerAddress = "localhost"
    var address: String
        get() = "ws://$innerAddress:$wsPort"
        set(value) {
            innerAddress = value
        }

    var factory: WebSocketFactory = WebSocketFactory()
    var initializer: ((WebSocket) -> WebSocket) = { it }

    private val handlers = SocketHandlerMap()

    fun putHandler(name: String, handler: SocketHandler) = handlers.put(name, handler)

    fun build(client: BasaltClient): AudioNode = AudioNode(client, wsPort, baseInterval, maxInterval, intervalExpander,
            intervalTimeUnit, factory, initializer, nodePassword, address, handlers)

}
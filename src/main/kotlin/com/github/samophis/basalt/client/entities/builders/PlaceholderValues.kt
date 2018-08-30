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

package com.github.samophis.basalt.client.entities.builders

import com.github.samophis.basalt.client.entities.AudioNode
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnegative

@Suppress("UNUSED")
open class PlaceholderValues internal constructor() {
    @Nonnegative
    var wsPort: Int = 80
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 1) {
                LOGGER.error("Cannot set the WebSocket Port to below one. ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    @Nonnegative
    var maxInterval: Long = 15
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 1) {
                LOGGER.error("Cannot set the Maximum Reconnect Interval to below one. ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    var baseInterval: Long = 1
    var intervalExpander: ((AudioNode, Long) -> Long) = { _, interval -> interval * 2 }
    var intervalTimeUnit: TimeUnit = TimeUnit.SECONDS
    var nodePassword: String = "youshallnotpass"

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PlaceholderValues::class.java)
    }
}
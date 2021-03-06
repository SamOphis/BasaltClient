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

import com.github.samophis.basalt.client.entities.BasaltClient
import io.vertx.core.VertxOptions
import org.slf4j.LoggerFactory
import javax.annotation.Nonnegative

@Suppress("UNUSED")
class BasaltClientBuilder: PlaceholderValues() {
    @Nonnegative
    var userId: Long = 0
        @Throws(IllegalArgumentException::class)
        set(value) {
            if (value < 0) {
                LOGGER.error("Cannot set User ID to a negative value! ({})", value)
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }

    var vertxOptions: VertxOptions = VertxOptions()

    fun build(): BasaltClient = BasaltClient(wsPort, baseInterval, maxInterval, intervalTimeUnit, intervalExpander, nodePassword, userId, vertxOptions)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BasaltClientBuilder::class.java)
    }
}
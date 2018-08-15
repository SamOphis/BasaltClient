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

package basalt.client.jda

import basalt.client.entities.BasaltClient
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class ShardInitializer(private val client: BasaltClient): ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        val impl = event.jda as JDAImpl
        val handlers = impl.client.handlers
        handlers["VOICE_SERVER_UPDATE"] = VoiceServerInterceptor(client, impl)
        handlers["VOICE_STATE_UPDATE"] = VoiceStateInterceptor(impl)
    }
}
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

package com.github.samophis.basalt.client.entities.events

@Suppress("UNUSED")
open class AudioEventAdapter: AudioEventListener {
    open fun onTrackStart(trackStartEvent: TrackStartEvent) {}
    open fun onTrackEnd(trackEndEvent: TrackEndEvent) {}
    open fun onTrackStuck(trackStuckEvent: TrackStuckEvent) {}
    open fun onTrackException(trackExceptionEvent: TrackExceptionEvent) {}
    open fun onPlayerPause(playerPauseEvent: PlayerPauseEvent) {}
    open fun onPlayerResume(playerResumeEvent: PlayerResumeEvent) {}

    override fun onEvent(event: Event) {
        when (event) {
            is TrackStartEvent -> onTrackStart(event)
            is TrackEndEvent -> onTrackEnd(event)
            is TrackStuckEvent -> onTrackStuck(event)
            is TrackExceptionEvent -> onTrackException(event)
            is PlayerPauseEvent -> onPlayerPause(event)
            is PlayerResumeEvent -> onPlayerResume(event)
        }
    }
}
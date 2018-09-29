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

package com.github.samophis.basalt.client.util

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import it.unimi.dsi.fastutil.io.FastByteArrayInputStream
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream
import net.iharder.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class AudioTrackUtil internal constructor() {
    @Throws(IOException::class)
    fun encodeTrack(track: AudioTrack): String {
        try {
            val out = FastByteArrayOutputStream()
            MANAGER.encodeTrack(MessageOutput(out), track)
            return Base64.encodeBytes(out.array)
        } catch (err: IOException) {
            LOGGER.error("Error when encoding AudioTrack!", err)
            throw err
        }
    }

    @Throws(IOException::class)
    fun decodeTrack(data: String): AudioTrack {
        try {
            return MANAGER.decodeTrack(MessageInput(FastByteArrayInputStream(Base64.decode(data)))).decodedTrack
        } catch (err: IOException) {
            LOGGER.error("Error when decoding AudioTrack!", err)
            throw err
        }
    }

    /** @suppress */
    companion object {
        /** @suppress */
        private val MANAGER = DefaultAudioPlayerManager()
        /** @suppress */
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioTrackUtil::class.java)
        /** @suppress */
        init {
            MANAGER.registerSourceManager(YoutubeAudioSourceManager(true))
            MANAGER.registerSourceManager(SoundCloudAudioSourceManager(true))
            MANAGER.registerSourceManager(TwitchStreamAudioSourceManager())
            MANAGER.registerSourceManager(BandcampAudioSourceManager())
            MANAGER.registerSourceManager(VimeoAudioSourceManager())
            MANAGER.registerSourceManager(BeamAudioSourceManager())
            MANAGER.registerSourceManager(HttpAudioSourceManager())
            Runtime.getRuntime().addShutdownHook(Thread {MANAGER::shutdown})
        }
    }
}
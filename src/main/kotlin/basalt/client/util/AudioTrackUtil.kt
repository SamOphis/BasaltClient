package basalt.client.util

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
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
        private val LOGGER: Logger = LoggerFactory.getLogger(AudioTrackUtil::class.java)
        init {
            Runtime.getRuntime().addShutdownHook(Thread {MANAGER::shutdown})
        }
    }
}
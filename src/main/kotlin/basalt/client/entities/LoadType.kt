package basalt.client.entities

@Suppress("UNUSED")
enum class LoadType {
    TRACK_LOADED,
    PLAYLIST_LOADED,
    SEARCH_RESULT,
    NO_MATCHES,
    LOAD_FAILED,
    UNKNOWN;

    companion object {
        fun from(name: String): LoadType = enumValueOf(name.toUpperCase())
    }
}
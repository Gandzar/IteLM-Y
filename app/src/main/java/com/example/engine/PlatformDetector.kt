package com.example.engine

import com.example.data.PlatformId

object PlatformDetector {
    private data class Rule(val id: PlatformId, val patterns: List<Regex>)

    private val rules = listOf(
        Rule(
            PlatformId.YOUTUBE,
            listOf(
                Regex("(?:youtube\\.com|youtu\\.be|youtube-nocookie\\.com)", RegexOption.IGNORE_CASE),
                Regex("youtube\\.com/shorts/", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.TIKTOK,
            listOf(
                Regex("tiktok\\.com", RegexOption.IGNORE_CASE),
                Regex("vt\\.tiktok\\.com", RegexOption.IGNORE_CASE),
                Regex("vm\\.tiktok\\.com", RegexOption.IGNORE_CASE),
                Regex("tt\\.site", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.INSTAGRAM,
            listOf(
                Regex("instagram\\.com", RegexOption.IGNORE_CASE),
                Regex("instagr\\.am", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.TWITTER,
            listOf(
                Regex("twitter\\.com", RegexOption.IGNORE_CASE),
                Regex("x\\.com/", RegexOption.IGNORE_CASE),
                Regex("t\\.co/", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.FACEBOOK,
            listOf(
                Regex("facebook\\.com", RegexOption.IGNORE_CASE),
                Regex("fb\\.watch", RegexOption.IGNORE_CASE),
                Regex("fb\\.com", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.SPOTIFY,
            listOf(
                Regex("open\\.spotify\\.com", RegexOption.IGNORE_CASE),
                Regex("spotify\\.link", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.SOUNDCLOUD,
            listOf(Regex("soundcloud\\.com", RegexOption.IGNORE_CASE)),
        ),
        Rule(
            PlatformId.PINTEREST,
            listOf(
                Regex("pinterest\\.", RegexOption.IGNORE_CASE),
                Regex("pin\\.it", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.REDDIT,
            listOf(
                Regex("reddit\\.com", RegexOption.IGNORE_CASE),
                Regex("redd\\.it", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.BILIBILI,
            listOf(
                Regex("bilibili\\.com", RegexOption.IGNORE_CASE),
                Regex("b23\\.tv", RegexOption.IGNORE_CASE),
            ),
        ),
        Rule(
            PlatformId.THREADS,
            listOf(Regex("threads\\.net", RegexOption.IGNORE_CASE)),
        ),
    )

    fun detectPlatform(url: String): PlatformId =
        rules.firstOrNull { rule -> rule.patterns.any { it.containsMatchIn(url) } }?.id ?: PlatformId.UNKNOWN

    fun platformLabel(id: PlatformId): String = when (id) {
        PlatformId.YOUTUBE -> "YouTube"
        PlatformId.TIKTOK -> "TikTok"
        PlatformId.INSTAGRAM -> "Instagram"
        PlatformId.TWITTER -> "X / Twitter"
        PlatformId.FACEBOOK -> "Facebook"
        PlatformId.SPOTIFY -> "Spotify"
        PlatformId.SOUNDCLOUD -> "SoundCloud"
        PlatformId.PINTEREST -> "Pinterest"
        PlatformId.REDDIT -> "Reddit"
        PlatformId.BILIBILI -> "Bilibili"
        PlatformId.THREADS -> "Threads"
        PlatformId.UNKNOWN -> "Unknown"
    }

    private val ytRegex = Regex(
        "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?|shorts)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})",
        RegexOption.IGNORE_CASE,
    )

    fun extractYoutubeId(url: String): String? = ytRegex.find(url)?.groupValues?.getOrNull(1)
}

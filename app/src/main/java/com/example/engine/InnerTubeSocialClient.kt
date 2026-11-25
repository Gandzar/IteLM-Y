package com.example.engine

import com.example.data.CommentsPageResult
import com.example.data.YtComment
import com.example.debug.AppDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object InnerTubeSocialClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private const val YT_API = "https://www.youtube.com/youtubei/v1"
    private const val YT_MUSIC_API = "https://music.youtube.com/youtubei/v1"
    private const val WEB_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCommentsAndLikeState(
        videoId: String,
        authHeaders: Map<String, String>,
        continuationToken: String? = null,
    ): CommentsPageResult = withContext(Dispatchers.IO) {
        val cleanId = videoId.trim()
        if (cleanId.isEmpty()) return@withContext CommentsPageResult()

        runCatching {
            val ytHeaders = toYoutubeOriginHeaders(authHeaders)
            val payload = JSONObject().put("context", webContext())
            if (!continuationToken.isNullOrBlank()) {
                payload.put("continuation", continuationToken)
            } else {
                payload.put("videoId", cleanId)
            }

            val nextJson = postJson("$YT_API/next?prettyPrint=false", payload, WEB_UA, ytHeaders)
                ?: return@withContext CommentsPageResult()

            var isLiked: Boolean? = null
            var likeCountText = ""
            findObjectsByKey(nextJson, "likeButtonViewModel").forEach { vm ->
                val toggleVm = vm.optJSONObject("toggleButtonViewModel")?.optJSONObject("toggleButtonViewModel")
                if (toggleVm != null) {
                    val toggled = toggleVm.optBoolean("isToggled", false)
                    isLiked = toggled
                    val defaultTitle = toggleVm.optJSONObject("defaultButtonViewModel")
                        ?.optJSONObject("buttonViewModel")
                        ?.optString("title").orEmpty()
                    val toggledTitle = toggleVm.optJSONObject("toggledButtonViewModel")
                        ?.optJSONObject("buttonViewModel")
                        ?.optString("title").orEmpty()
                    likeCountText = (if (toggled) toggledTitle else defaultTitle).ifBlank { defaultTitle }
                }
            }
            if (isLiked == null) {
                findObjectsByKey(nextJson, "toggleButtonRenderer").forEach { tbr ->
                    val status = tbr.optJSONObject("defaultIcon")?.optString("iconType").orEmpty()
                    if (status == "LIKE") {
                        isLiked = tbr.optBoolean("isToggled", false)
                        likeCountText = extractText(tbr.optJSONObject("defaultText"))
                    }
                }
            }

            // Locate createCommentParams
            var createParams: String? = findFirstStringByKey(nextJson, "createCommentParams")
            if (createParams.isNullOrBlank()) {
                findObjectsByKey(nextJson, "createCommentEndpoint").forEach { ep ->
                    val p = ep.optString("createCommentParams")
                    if (p.isNotBlank()) createParams = p
                }
            }

            // Locate comments continuation token if initial call didn't return commentEntityPayload
            var commentsRoot = nextJson
            var nextCont: String? = null

            val directComments = parseComments(commentsRoot)
            if (directComments.isEmpty() && continuationToken.isNullOrBlank()) {
                val commentSectionCont = findCommentSectionContinuation(nextJson)
                if (!commentSectionCont.isNullOrBlank()) {
                    val contPayload = JSONObject()
                        .put("context", webContext())
                        .put("continuation", commentSectionCont)
                    val contJson = postJson("$YT_API/next?prettyPrint=false", contPayload, WEB_UA, ytHeaders)
                    if (contJson != null) {
                        commentsRoot = contJson
                        if (createParams.isNullOrBlank()) {
                            createParams = findFirstStringByKey(contJson, "createCommentParams")
                        }
                    }
                }
            }

            val comments = parseComments(commentsRoot)
            findObjectsByKey(commentsRoot, "continuationCommand").forEach { cmd ->
                val tok = cmd.optString("token")
                if (tok.isNotBlank()) nextCont = tok
            }

            CommentsPageResult(
                comments = comments,
                continuationToken = nextCont,
                createCommentParams = createParams,
                isLikedByMe = isLiked,
                likeCountText = likeCountText,
            )
        }.getOrElse { e ->
            AppDiagnostics.warn("Social", "Comments fetch error: ${e.message}")
            CommentsPageResult()
        }
    }

    suspend fun toggleLikeVideo(
        videoId: String,
        like: Boolean,
        authHeaders: Map<String, String>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext false
        runCatching {
            val ytHeaders = toYoutubeOriginHeaders(authHeaders)
            val endpoint = if (like) "$YT_API/like/like?prettyPrint=false" else "$YT_API/like/removelike?prettyPrint=false"
            val payload = JSONObject()
                .put("context", webContext())
                .put("target", JSONObject().put("videoId", videoId))
            val res = postJson(endpoint, payload, WEB_UA, ytHeaders)
            val ok = res != null
            AppDiagnostics.info("Social", "Like($like) $videoId -> $ok")
            ok
        }.getOrDefault(false)
    }

    suspend fun createComment(
        videoId: String,
        commentText: String,
        createCommentParams: String?,
        authHeaders: Map<String, String>,
    ): Result<YtComment> = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Login akun Google dulu untuk mengirim komentar"))
        }
        val cleanText = commentText.trim()
        if (cleanText.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Tulis komentar terlebih dahulu"))
        }

        runCatching {
            val ytHeaders = toYoutubeOriginHeaders(authHeaders)
            var params = createCommentParams
            if (params.isNullOrBlank()) {
                params = fetchCommentsAndLikeState(videoId, authHeaders).createCommentParams
            }
            if (params.isNullOrBlank()) {
                error("Token komentar belum tersedia untuk video ini")
            }

            val payload = JSONObject()
                .put("context", webContext())
                .put("commentText", cleanText)
                .put("createCommentParams", params)

            val res = postJson("$YT_API/comment/create_comment?prettyPrint=false", payload, WEB_UA, ytHeaders)
                ?: error("Gagal mengirim komentar ke server YouTube")

            val err = res.optJSONObject("error")?.optString("message")
            if (!err.isNullOrBlank()) error(err)

            AppDiagnostics.info("Social", "Comment posted on $videoId")
            YtComment(
                id = "local-${System.currentTimeMillis()}",
                author = "Anda",
                text = cleanText,
                publishedTime = "Baru saja",
                likeCount = "0",
            )
        }
    }

    suspend fun fetchLyrics(
        videoId: String,
        authHeaders: Map<String, String>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("context", webRemixContext())
                .put("videoId", videoId)
            val nextJson = postJson("$YT_MUSIC_API/next?prettyPrint=false", payload, WEB_UA, authHeaders)
                ?: error("Lirik tidak ditemukan")

            var lyricsBrowseId: String? = null
            findObjectsByKey(nextJson, "tabRenderer").forEach { tab ->
                val endpoint = tab.optJSONObject("endpoint")?.optJSONObject("browseEndpoint")
                val bid = endpoint?.optString("browseId").orEmpty()
                val title = tab.optString("title")
                if (bid.startsWith("MPLYt") || title.contains("Lyrics", ignoreCase = true) || title.contains("Lirik", ignoreCase = true)) {
                    lyricsBrowseId = bid
                }
            }
            val bid = lyricsBrowseId ?: error("Lagu ini belum memiliki lirik di YouTube Music")
            val browsePayload = JSONObject()
                .put("context", webRemixContext())
                .put("browseId", bid)
            val browseJson = postJson("$YT_MUSIC_API/browse?prettyPrint=false", browsePayload, WEB_UA, authHeaders)
                ?: error("Gagal mengambil teks lirik")

            val lyricsRenderer = findObjectsByKey(browseJson, "musicDescriptionShelfRenderer").firstOrNull()
                ?: error("Lirik tidak tersedia")
            val text = extractText(lyricsRenderer.optJSONObject("description")).trim()
            if (text.isBlank()) error("Lirik kosong")
            text
        }
    }

    private fun parseComments(root: JSONObject): List<YtComment> {
        val out = mutableListOf<YtComment>()
        val seen = hashSetOf<String>()

        // Modern YouTube frameworkUpdates commentEntityPayload
        findObjectsByKey(root, "commentEntityPayload").forEach { p ->
            val props = p.optJSONObject("properties")
            val id = props?.optString("commentId").orEmpty()
                .ifBlank { p.optString("key") }
            val content = props?.optJSONObject("content")?.optString("content").orEmpty()
                .ifBlank { extractText(props?.optJSONObject("content")) }
            val published = props?.optString("publishedTime").orEmpty()
            val authorObj = p.optJSONObject("author")
            val author = authorObj?.optString("displayName").orEmpty().ifBlank { "Pengguna" }
            val avatar = authorObj?.optString("avatarThumbnailUrl").orEmpty()
            val likes = p.optJSONObject("toolbar")?.optString("likeCountNotliked").orEmpty()
            if (content.isNotBlank() && seen.add(id.ifBlank { content })) {
                out.add(
                    YtComment(
                        id = id.ifBlank { "c-${out.size}" },
                        author = author,
                        avatarUrl = avatar,
                        text = content,
                        publishedTime = published,
                        likeCount = likes,
                    ),
                )
            }
        }

        // Legacy commentRenderer fallback
        findObjectsByKey(root, "commentRenderer").forEach { c ->
            val id = c.optString("commentId")
            val content = extractText(c.optJSONObject("contentText"))
            val author = extractText(c.optJSONObject("authorText")).ifBlank { "Pengguna" }
            val published = extractText(c.optJSONObject("publishedTimeText"))
            val likes = c.optString("voteCountText").ifBlank { extractText(c.optJSONObject("voteCount")) }
            val avatarArr = c.optJSONObject("authorThumbnail")?.optJSONArray("thumbnails")
            val avatar = avatarArr?.optJSONObject(avatarArr.length() - 1)?.optString("url").orEmpty()
            if (content.isNotBlank() && seen.add(id.ifBlank { content })) {
                out.add(
                    YtComment(
                        id = id.ifBlank { "cl-${out.size}" },
                        author = author,
                        avatarUrl = avatar,
                        text = content,
                        publishedTime = published,
                        likeCount = likes,
                    ),
                )
            }
        }

        return out
    }

    private fun findCommentSectionContinuation(root: JSONObject): String? {
        var found: String? = null
        findObjectsByKey(root, "itemSectionRenderer").forEach { sec ->
            if (sec.optString("sectionIdentifier").contains("comment", ignoreCase = true)) {
                val tok = findFirstStringByKey(sec, "token")
                if (!tok.isNullOrBlank()) found = tok
            }
        }
        if (!found.isNullOrBlank()) return found
        findObjectsByKey(root, "continuationItemRenderer").forEach { item ->
            val tok = item.optJSONObject("continuationEndpoint")
                ?.optJSONObject("continuationCommand")
                ?.optString("token")
            if (!tok.isNullOrBlank()) return tok
        }
        return null
    }

    private fun toYoutubeOriginHeaders(authHeaders: Map<String, String>): Map<String, String> =
        authHeaders.mapValues { (k, v) ->
            if (k == "Origin" || k == "X-Origin") "https://www.youtube.com" else v
        }

    private fun postJson(
        url: String,
        body: JSONObject,
        userAgent: String,
        extraHeaders: Map<String, String>,
    ): JSONObject? {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
        extraHeaders.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) builder.header(k, v)
        }
        client.newCall(builder.build()).execute().use { res ->
            if (!res.isSuccessful) return null
            val raw = res.body?.string().orEmpty()
            if (raw.isBlank()) return null
            return JSONObject(raw)
        }
    }

    private fun webContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "WEB")
            .put("clientVersion", "2.20241127.01.00")
            .put("hl", "id")
            .put("gl", "ID"),
    )

    private fun webRemixContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", "1.20241127.01.00")
            .put("hl", "id")
            .put("gl", "ID"),
    )

    private fun extractText(obj: JSONObject?): String {
        if (obj == null) return ""
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString().trim()
    }

    private fun findFirstStringByKey(node: Any?, key: String): String? {
        when (node) {
            is JSONObject -> {
                if (node.has(key)) {
                    val v = node.optString(key)
                    if (v.isNotBlank()) return v
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val res = findFirstStringByKey(node.opt(keys.next()), key)
                    if (!res.isNullOrBlank()) return res
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val res = findFirstStringByKey(node.opt(i), key)
                    if (!res.isNullOrBlank()) return res
                }
            }
        }
        return null
    }

    private fun findObjectsByKey(node: Any?, targetKey: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        fun walk(current: Any?) {
            when (current) {
                is JSONObject -> {
                    val keys = current.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = current.opt(k)
                        if (k == targetKey && v is JSONObject) result.add(v)
                        walk(v)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until current.length()) walk(current.opt(i))
                }
            }
        }
        walk(node)
        return result
    }
}

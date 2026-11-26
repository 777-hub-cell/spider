package com.vistora.android.data.catalog

import com.vistora.android.domain.CatalogResult
import com.vistora.android.domain.MediaType
import com.vistora.android.domain.SeriesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CatalogDiscoveryRepository {
    suspend fun load(tmdbCredential: String?): List<CatalogResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CatalogResult>()
        runCatching { discoverAnime() }.onSuccess { results.addAll(it) }
        val credential = tmdbCredential?.trim().orEmpty()
        if (credential.isNotBlank()) {
            listOf(MediaType.MOVIE, MediaType.K_DRAMA, MediaType.TV_SERIES).forEach { type ->
                runCatching { discoverTmdb(type, credential) }.onSuccess { results.addAll(it) }
            }
        }
        val unique = results.distinctBy { it.providerId }
        if (unique.isEmpty()) throw IOException("The catalogue could not be reached. Check your connection and try again.")
        unique
    }

    private fun discoverTmdb(type: MediaType, credential: String): List<CatalogResult> {
        val endpoint = if (type == MediaType.MOVIE) "movie" else "tv"
        val url = buildString {
            append("https://api.themoviedb.org/3/discover/").append(endpoint)
            append("?sort_by=popularity.desc&include_adult=false&language=en-US&page=1")
            if (type == MediaType.K_DRAMA) append("&with_original_language=ko")
            if (type == MediaType.TV_SERIES) append("&without_original_language=ko")
            if (credential.count { it == '.' } != 2) append("&api_key=").append(credential)
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (credential.count { it == '.' } == 2) connection.setRequestProperty("Authorization", "Bearer $credential")
            val root = readJson(connection)
            val rows = root.optJSONArray("results") ?: return emptyList()
            return (0 until rows.length()).mapNotNull { index ->
                val item = rows.optJSONObject(index) ?: return@mapNotNull null
                val titleKey = if (endpoint == "movie") "title" else "name"
                val dateKey = if (endpoint == "movie") "release_date" else "first_air_date"
                val title = item.optString(titleKey).trim()
                if (title.isBlank()) return@mapNotNull null
                val genreIds = item.optJSONArray("genre_ids")
                val genres = if (genreIds == null) emptyList() else {
                    val labels = mapOf(
                        28 to "Action",12 to "Adventure",16 to "Animation",35 to "Comedy",
                        80 to "Crime",99 to "Documentary",18 to "Drama",10751 to "Family",
                        14 to "Fantasy",36 to "History",27 to "Horror",10402 to "Music",
                        9648 to "Mystery",10749 to "Romance",878 to "Science Fiction",
                        53 to "Thriller",10752 to "War",37 to "Western",
                        10759 to "Action & Adventure",10765 to "Sci-Fi & Fantasy"
                    )
                    (0 until genreIds.length()).mapNotNull { labels[genreIds.optInt(it)] }.distinct()
                }
                val poster = item.optString("poster_path").takeIf { it.startsWith("/") }
                CatalogResult(
                    providerId = "tmdb:" + item.optLong("id"),
                    title = title,
                    type = type,
                    releaseYear = item.optString(dateKey).take(4).toIntOrNull(),
                    totalEpisodes = 0,
                    seriesState = SeriesState.UNKNOWN,
                    synopsis = item.optString("overview").take(700),
                    genres = genres,
                    posterUrl = poster?.let { "https://image.tmdb.org/t/p/w500" + it },
                    source = "TMDB popular",
                    rating = item.optDouble("vote_average").takeIf { it > 0.0 }
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun discoverAnime(): List<CatalogResult> {
        val graphQl = """
            query {
              Page(page: 1, perPage: 24) {
                media(type: ANIME, sort: [TRENDING_DESC, POPULARITY_DESC], isAdult: false) {
                  id
                  title { english romaji native }
                  episodes
                  format
                  status
                  startDate { year }
                  description(asHtml: false)
                  averageScore
                  genres
                  coverImage { large }
                }
              }
            }
        """.trimIndent()
        val payload = JSONObject().put("query", graphQl)
        val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val root = readJson(connection)
            val errors = root.optJSONArray("errors")
            if (errors != null && errors.length() > 0) throw IOException("AniList discovery is temporarily unavailable.")
            val rows = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
            return (0 until rows.length()).mapNotNull { index ->
                val item = rows.optJSONObject(index) ?: return@mapNotNull null
                val titles = item.optJSONObject("title") ?: JSONObject()
                val title = sequenceOf("english","romaji","native").map { titles.optString(it).trim() }.firstOrNull { it.isNotBlank() }
                    ?: return@mapNotNull null
                val movie = item.optString("format") == "MOVIE"
                val state = if (movie) SeriesState.UNKNOWN else when (item.optString("status")) {
                    "RELEASING" -> SeriesState.ONGOING
                    "FINISHED" -> SeriesState.COMPLETED
                    "NOT_YET_RELEASED" -> SeriesState.UPCOMING
                    else -> SeriesState.UNKNOWN
                }
                val genreArray = item.optJSONArray("genres")
                val genres = if (genreArray == null) emptyList() else (0 until minOf(8,genreArray.length())).mapNotNull { genreArray.optString(it).takeIf(String::isNotBlank) }
                val poster = item.optJSONObject("coverImage")?.optString("large")?.takeIf { it.startsWith("https://") }
                CatalogResult(
                    providerId = "anilist:" + item.optInt("id"),
                    title = title,
                    type = MediaType.ANIME,
                    releaseYear = item.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 },
                    totalEpisodes = if (movie) 0 else item.optInt("episodes",0).coerceAtLeast(0),
                    seriesState = state,
                    synopsis = item.optString("description").replace(Regex("<[^>]*>"),"").take(700),
                    genres = genres,
                    posterUrl = poster,
                    source = "AniList trending",
                    rating = item.optDouble("averageScore").takeIf { it > 0.0 }?.div(10.0)
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code == 401 || code == 403) throw IOException("The catalogue rejected the TMDB credential. Check Settings.")
        if (code == 429) throw IOException("The catalogue is busy. Wait a moment and try again.")
        if (code !in 200..299) throw IOException("Catalogue request failed (HTTP $code).")
        return runCatching { JSONObject(body) }.getOrElse { throw IOException("The catalogue returned unreadable data.") }
    }
}

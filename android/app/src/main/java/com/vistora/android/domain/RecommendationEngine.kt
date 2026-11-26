package com.vistora.android.domain

import java.util.Locale
import kotlin.math.sqrt

object RecommendationEngine {
    private fun itemSignal(item: MediaItem): Double {
        val statusSignal = when (item.status) {
            LibraryStatus.COMPLETED -> 1.25
            LibraryStatus.WATCHING -> 0.75
            LibraryStatus.PLAN_TO_WATCH -> 0.18
            LibraryStatus.ON_HOLD -> -0.05
            // One abandoned title is only a soft signal, not a verdict on its genre.
            LibraryStatus.DROPPED -> -0.45
        }
        val ratingSignal = when (item.rating.coerceIn(0, 5)) {
            5 -> 2.4
            4 -> 1.55
            3 -> 0.35
            2 -> -0.55
            1 -> -1.15
            else -> 0.0
        }
        val progressSignal = if (item.isEpisodic && item.status != LibraryStatus.DROPPED) {
            (item.progressPercent.coerceIn(0, 100) / 100.0) * 0.8
        } else 0.0
        val watchlistSignal = if (item.isWatchlisted && item.status != LibraryStatus.DROPPED) 0.12 else 0.0
        return statusSignal + ratingSignal + progressSignal + watchlistSignal
    }

    private fun genreAffinities(library: List<MediaItem>): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val counts = mutableMapOf<String, Int>()
        library.forEach { item ->
            val signal = itemSignal(item)
            item.genres.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.distinct().forEach { genre ->
                totals[genre] = (totals[genre] ?: 0.0) + signal
                counts[genre] = (counts[genre] ?: 0) + 1
            }
        }
        return totals.mapValues { (genre, total) -> total / sqrt((counts[genre] ?: 1).toDouble()) }
    }

    private fun typeAffinities(library: List<MediaItem>): Map<MediaType, Double> {
        val totals = mutableMapOf<MediaType, Double>()
        val counts = mutableMapOf<MediaType, Int>()
        library.forEach { item ->
            totals[item.type] = (totals[item.type] ?: 0.0) + itemSignal(item)
            counts[item.type] = (counts[item.type] ?: 0) + 1
        }
        return totals.mapValues { (type, total) -> total / sqrt((counts[type] ?: 1).toDouble()) }
    }

    fun score(candidate: CatalogResult, library: List<MediaItem>): Double {
        if (library.isEmpty()) return 0.0
        val genreScores = genreAffinities(library)
        val typeScores = typeAffinities(library)
        val genres = candidate.genres.map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }.distinct()
        val genreScore = if (genres.isEmpty()) 0.0 else
            genres.sumOf { genreScores[it] ?: 0.0 } / sqrt(genres.size.toDouble())
        return genreScore + (typeScores[candidate.type] ?: 0.0) * 0.22
    }

    fun rank(candidates: List<CatalogResult>, library: List<MediaItem>): List<CatalogResult> =
        candidates.sortedWith(
            compareByDescending<CatalogResult> { score(it, library) }
                .thenByDescending { it.rating ?: 0.0 }
                .thenByDescending { it.releaseYear ?: 0 }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )

    fun favouriteGenre(library: List<MediaItem>): String? =
        genreAffinities(library).maxByOrNull { it.value }
            ?.takeIf { it.value > 0.15 }
            ?.key
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    fun reasonFor(candidate: CatalogResult, library: List<MediaItem>): String {
        if (library.isEmpty()) return "A starter pick to help you explore"
        val affinities = genreAffinities(library)
        val strongestMatch = candidate.genres
            .mapNotNull { genre -> affinities[genre.trim().lowercase(Locale.ROOT)]?.let { genre to it } }
            .filter { it.second > 0.15 }
            .maxByOrNull { it.second }
        if (strongestMatch != null) return "Because you enjoy " + strongestMatch.first
        val typeSignal = typeAffinities(library)[candidate.type] ?: 0.0
        if (typeSignal > 0.15) return "More " + candidate.type.category + " based on your library"
        return if (candidate.source.contains("starter", ignoreCase = true)) {
            "A starter pick to explore"
        } else "Popular in " + candidate.type.category
    }
}

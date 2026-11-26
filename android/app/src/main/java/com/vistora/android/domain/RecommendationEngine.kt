package com.vistora.android.domain

object RecommendationEngine {
    fun score(candidate: CatalogResult, library: List<MediaItem>): Double {
        if (library.isEmpty()) return 0.0
        val genreAffinity = mutableMapOf<String, Double>()
        val typeAffinity = mutableMapOf<MediaType, Double>()
        library.forEach { item ->
            val statusWeight = when (item.status) {
                LibraryStatus.COMPLETED -> 3.0
                LibraryStatus.WATCHING -> 2.0
                LibraryStatus.PLAN_TO_WATCH -> 0.8
                LibraryStatus.ON_HOLD -> 0.0
                LibraryStatus.DROPPED -> -2.5
            }
            val ratingWeight = when (item.rating) {
                5 -> 3.0
                4 -> 2.0
                3 -> 0.5
                2 -> -1.0
                1 -> -2.0
                else -> 0.0
            }
            val progressWeight = if (item.isEpisodic && item.watchedEpisodes > 0 && item.status != LibraryStatus.DROPPED) 0.4 else 0.0
            val weight = statusWeight + ratingWeight + progressWeight
            item.genres.forEach { genre ->
                val key = genre.trim().lowercase()
                if (key.isNotEmpty()) genreAffinity[key] = (genreAffinity[key] ?: 0.0) + weight
            }
            typeAffinity[item.type] = (typeAffinity[item.type] ?: 0.0) + weight
        }
        val genreScore = candidate.genres.sumOf { genreAffinity[it.trim().lowercase()] ?: 0.0 }
        val typeScore = (typeAffinity[candidate.type] ?: 0.0) * 0.2
        return genreScore + typeScore
    }

    fun rank(candidates: List<CatalogResult>, library: List<MediaItem>): List<CatalogResult> =
        candidates.sortedWith(compareByDescending<CatalogResult> { score(it, library) }
            .thenByDescending { it.releaseYear ?: 0 }.thenBy { it.title.lowercase() })

    fun favouriteGenre(library: List<MediaItem>): String? {
        val scores = mutableMapOf<String, Double>()
        library.forEach { item ->
            val status = when (item.status) {
                LibraryStatus.COMPLETED -> 3.0
                LibraryStatus.WATCHING -> 2.0
                LibraryStatus.PLAN_TO_WATCH -> 0.8
                LibraryStatus.ON_HOLD -> 0.0
                LibraryStatus.DROPPED -> -2.5
            }
            val rating = when (item.rating) {
                5 -> 3.0
                4 -> 2.0
                3 -> 0.5
                2 -> -1.0
                1 -> -2.0
                else -> 0.0
            }
            item.genres.forEach { genre ->
                val key = genre.trim()
                if (key.isNotEmpty()) scores[key] = (scores[key] ?: 0.0) + status + rating
            }
        }
        return scores.maxByOrNull { it.value }?.takeIf { it.value > 0.0 }?.key
    }
}

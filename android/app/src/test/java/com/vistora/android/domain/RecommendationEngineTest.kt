package com.vistora.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test
    fun highlyRatedCompletedGenresOutrankDroppedGenres() {
        val library = listOf(
            MediaItem(title = "Comedy favourite", type = MediaType.MOVIE, genres = listOf("Comedy"), status = LibraryStatus.COMPLETED, rating = 5),
            MediaItem(title = "Dropped mystery", type = MediaType.MOVIE, genres = listOf("Mystery"), status = LibraryStatus.DROPPED, rating = 1)
        )
        val comedy = result("comedy", "New comedy", listOf("Comedy"))
        val mystery = result("mystery", "New mystery", listOf("Mystery"))
        assertTrue(RecommendationEngine.score(comedy, library) > RecommendationEngine.score(mystery, library))
    }

    @Test
    fun favouriteGenreUsesFinishedAndRatedTitles() {
        val library = listOf(
            MediaItem(title = "Comedy favourite", type = MediaType.TV_SERIES, genres = listOf("Comedy"), status = LibraryStatus.COMPLETED, rating = 5),
            MediaItem(title = "Dropped thriller", type = MediaType.MOVIE, genres = listOf("Thriller"), status = LibraryStatus.DROPPED, rating = 1)
        )
        assertEquals("Comedy", RecommendationEngine.favouriteGenre(library))
    }

    @Test
    fun oneDroppedTitleIsOnlyASoftNegativeSignal() {
        val library = listOf(
            MediaItem(title = "Dropped mystery", type = MediaType.MOVIE, genres = listOf("Mystery"), status = LibraryStatus.DROPPED)
        )
        val mystery = RecommendationEngine.score(result("mystery", "Another mystery", listOf("Mystery")), library)
        val comedy = RecommendationEngine.score(result("comedy", "A comedy", listOf("Comedy")), library)
        assertTrue(mystery < comedy)
        assertTrue(comedy - mystery < 1.0)
    }

    @Test
    fun progressAndFinishingIncreasePreferenceForThatGenre() {
        val started = MediaItem(
            title = "Comedy series", type = MediaType.TV_SERIES, genres = listOf("Comedy"),
            status = LibraryStatus.WATCHING, totalEpisodes = 20, watchedEpisodes = 4
        )
        val finished = started.copy(status = LibraryStatus.COMPLETED, watchedEpisodes = 20, rating = 5)
        val candidate = result("candidate", "Another comedy", listOf("Comedy"))
        assertTrue(RecommendationEngine.score(candidate, listOf(finished)) > RecommendationEngine.score(candidate, listOf(started)))
    }

    @Test
    fun planToWatchIsWeakerThanAHighlyRatedFinish() {
        val candidate = result("candidate", "Another mystery", listOf("Mystery"))
        val planned = MediaItem(title = "Planned mystery", type = MediaType.MOVIE,
            genres = listOf("Mystery"), status = LibraryStatus.PLAN_TO_WATCH)
        val finished = planned.copy(status = LibraryStatus.COMPLETED, rating = 5)
        assertTrue(RecommendationEngine.score(candidate, listOf(finished)) > RecommendationEngine.score(candidate, listOf(planned)))
    }

    private fun result(id: String, title: String, genres: List<String>) = CatalogResult(
        providerId = id, title = title, type = MediaType.MOVIE, releaseYear = 2025,
        totalEpisodes = 0, seriesState = SeriesState.UNKNOWN, synopsis = "",
        genres = genres, posterUrl = null, source = "test"
    )
}

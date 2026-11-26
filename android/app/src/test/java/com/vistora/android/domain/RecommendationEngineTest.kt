package com.vistora.android.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test
    fun highlyRatedCompletedGenresOutrankDroppedGenres() {
        val library = listOf(
            MediaItem(title="Comedy favourite",type=MediaType.MOVIE,genres=listOf("Comedy"),status=LibraryStatus.COMPLETED,rating=5),
            MediaItem(title="Dropped mystery",type=MediaType.MOVIE,genres=listOf("Mystery"),status=LibraryStatus.DROPPED,rating=1)
        )
        val comedy = CatalogResult("comedy","New comedy",MediaType.MOVIE,2025,0,SeriesState.UNKNOWN,"",listOf("Comedy"),null,"test")
        val mystery = CatalogResult("mystery","New mystery",MediaType.MOVIE,2025,0,SeriesState.UNKNOWN,"",listOf("Mystery"),null,"test")
        assertTrue(RecommendationEngine.score(comedy,library) > RecommendationEngine.score(mystery,library))
    }

    @Test
    fun favouriteGenreUsesFinishedAndRatedTitles() {
        val library = listOf(
            MediaItem(title="Comedy favourite",type=MediaType.TV_SERIES,genres=listOf("Comedy"),status=LibraryStatus.COMPLETED,rating=5),
            MediaItem(title="Dropped thriller",type=MediaType.MOVIE,genres=listOf("Thriller"),status=LibraryStatus.DROPPED,rating=1)
        )
        assertTrue(RecommendationEngine.favouriteGenre(library) == "Comedy")
    }
}

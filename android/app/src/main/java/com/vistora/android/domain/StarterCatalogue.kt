package com.vistora.android.domain

object StarterCatalogue {
    val results: List<CatalogResult> = listOf(
        CatalogResult("starter:spirited-away", "Spirited Away", MediaType.ANIME, 2001, 0, SeriesState.UNKNOWN,
            "A young girl enters a mysterious world of spirits.", listOf("Fantasy", "Adventure"), null, "VISTORA starter picks"),
        CatalogResult("starter:interstellar", "Interstellar", MediaType.MOVIE, 2014, 0, SeriesState.UNKNOWN,
            "A crew searches for a new home beyond Earth.", listOf("Science Fiction", "Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:crash-landing-on-you", "Crash Landing on You", MediaType.K_DRAMA, 2019, 16, SeriesState.COMPLETED,
            "A chance accident brings two people from different sides of a border together.", listOf("Romance", "Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:stranger-things", "Stranger Things", MediaType.TV_SERIES, 2016, 34, SeriesState.ONGOING,
            "A group of friends encounter strange events in their town.", listOf("Mystery", "Science Fiction"), null, "VISTORA starter picks"),
        CatalogResult("starter:your-name", "Your Name", MediaType.ANIME, 2016, 0, SeriesState.UNKNOWN,
            "", listOf("Romance", "Fantasy"), null, "VISTORA starter picks"),
        CatalogResult("starter:the-bear", "The Bear", MediaType.TV_SERIES, 2022, 28, SeriesState.ONGOING,
            "", listOf("Comedy", "Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:my-mister", "My Mister", MediaType.K_DRAMA, 2018, 16, SeriesState.COMPLETED,
            "", listOf("Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:parasite", "Parasite", MediaType.MOVIE, 2019, 0, SeriesState.UNKNOWN,
            "", listOf("Thriller", "Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:knives-out", "Knives Out", MediaType.MOVIE, 2019, 0, SeriesState.UNKNOWN,
            "", listOf("Comedy", "Mystery", "Thriller"), null, "VISTORA starter picks"),
        CatalogResult("starter:mob-psycho-100", "Mob Psycho 100", MediaType.ANIME, 2016, 37, SeriesState.COMPLETED,
            "", listOf("Comedy", "Action", "Fantasy"), null, "VISTORA starter picks"),
        CatalogResult("starter:hospital-playlist", "Hospital Playlist", MediaType.K_DRAMA, 2020, 24, SeriesState.COMPLETED,
            "", listOf("Comedy", "Drama"), null, "VISTORA starter picks"),
        CatalogResult("starter:brooklyn-nine-nine", "Brooklyn Nine-Nine", MediaType.TV_SERIES, 2013, 153, SeriesState.COMPLETED,
            "", listOf("Comedy", "Crime"), null, "VISTORA starter picks")
    )
}

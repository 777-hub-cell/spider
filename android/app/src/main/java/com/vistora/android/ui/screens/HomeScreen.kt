package com.vistora.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vistora.android.domain.CatalogResult
import com.vistora.android.domain.CatalogSearchState
import com.vistora.android.domain.LibraryStatus
import com.vistora.android.domain.MediaItem
import com.vistora.android.domain.MediaType
import com.vistora.android.domain.RecommendationEngine
import com.vistora.android.domain.StarterCatalogue
import com.vistora.android.ui.components.PosterArt
import com.vistora.android.ui.components.PosterTile
import com.vistora.android.ui.components.SectionHeading
import com.vistora.android.ui.components.VistoraAction
import com.vistora.android.ui.components.VistoraIcon
import com.vistora.android.ui.theme.VistoraPalette
import java.util.Locale

@Composable
fun HomeScreen(
    items: List<MediaItem>,
    discoveryState: CatalogSearchState,
    tmdbConfigured: Boolean,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTitle: (Long) -> Unit,
    onOpenCatalogTitle: (CatalogResult) -> Unit,
    onDiscover: () -> Unit,
    onOpenCategory: (MediaType) -> Unit,
    onOpenStatus: (LibraryStatus) -> Unit
) {
    val alreadyAdded = remember(items) {
        items.map { it.type to it.title.trim().lowercase(Locale.ROOT) }.toSet()
    }
    val live = remember(discoveryState.results, alreadyAdded) {
        discoveryState.results.filter {
            (it.type to it.title.trim().lowercase(Locale.ROOT)) !in alreadyAdded
        }
    }
    val starter = remember(alreadyAdded) {
        StarterCatalogue.results.filter {
            (it.type to it.title.trim().lowercase(Locale.ROOT)) !in alreadyAdded
        }
    }
    val catalogue = remember(live, starter) {
        (live + starter.filterNot { pick ->
            live.any { it.type == pick.type && it.title.equals(pick.title, ignoreCase = true) }
        }).distinctBy { it.type to it.title.trim().lowercase(Locale.ROOT) }
    }
    val ranked = remember(catalogue, items) {
        RecommendationEngine.rank(catalogue, items)
    }
    val favouriteGenre = remember(items) { RecommendationEngine.favouriteGenre(items) }
    val genrePicks = remember(ranked, favouriteGenre) {
        favouriteGenre?.let { genre ->
            ranked.filter { result -> result.genres.any { it.equals(genre, ignoreCase = true) } }
        }.orEmpty()
    }
    val watching = remember(items) {
        items.filter { it.status == LibraryStatus.WATCHING }.sortedByDescending { it.updatedAt }.take(12)
    }
    val watchlist = remember(items) {
        items.filter { it.isWatchlisted || it.status == LibraryStatus.PLAN_TO_WATCH }
            .sortedByDescending { it.updatedAt }.take(12)
    }
    val finished = remember(items) {
        items.filter { it.status == LibraryStatus.COMPLETED }
            .sortedByDescending { it.finishedAt ?: it.updatedAt }.take(12)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 7.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HomeHeader(
                loading = discoveryState.isLoading,
                onSearch = onSearch,
                onNotifications = onNotifications,
                onRefresh = onRefresh
            )
        }

        val featured = ranked.firstOrNull()
        if (featured != null) {
            item {
                FeaturedTitle(
                    result = featured,
                    library = items,
                    isPersonalized = items.isNotEmpty() && RecommendationEngine.score(featured, items) > 0.0,
                    onClick = { onOpenCatalogTitle(featured) }
                )
            }
        } else if (discoveryState.isLoading) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(VistoraPalette.Surface).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VistoraPalette.Rose)
                    Text("Finding something worth watching…", color = VistoraPalette.Secondary,
                        fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }

        item { CategoryStrip(onOpenCategory) }

        if (items.isNotEmpty() && ranked.isNotEmpty()) {
            item {
                CatalogShelf(
                    title = "Picked for you",
                    subtitle = "Based on what you finish, rate and keep watching",
                    results = ranked.take(12),
                    library = items,
                    onOpen = onOpenCatalogTitle,
                    onSeeAll = onDiscover
                )
            }
        } else if (ranked.isNotEmpty()) {
            item {
                CatalogShelf(
                    title = "Popular picks to start with",
                    subtitle = "Explore a few titles and VISTORA will learn your taste",
                    results = ranked.take(12),
                    library = items,
                    onOpen = onOpenCatalogTitle,
                    onSeeAll = onDiscover
                )
            }
        }

        if (favouriteGenre != null && genrePicks.isNotEmpty()) {
            item {
                CatalogShelf(
                    title = "Because you enjoy " + favouriteGenre,
                    subtitle = "Your ratings and viewing activity help shape this row",
                    results = genrePicks.take(12),
                    library = items,
                    onOpen = onOpenCatalogTitle,
                    onSeeAll = onDiscover
                )
            }
        }

        if (live.isNotEmpty()) {
            item {
                CatalogShelf(
                    title = "Trending now",
                    subtitle = "Fresh picks from the connected catalogue",
                    results = live.take(12),
                    library = items,
                    onOpen = onOpenCatalogTitle,
                    onSeeAll = onDiscover
                )
            }
        }

        MediaType.entries.forEach { type ->
            val typeResults = ranked.filter { it.type == type }
            if (typeResults.isNotEmpty()) {
                item {
                    CatalogShelf(
                        title = "Explore " + type.category,
                        subtitle = null,
                        results = typeResults.take(10),
                        library = items,
                        onOpen = onOpenCatalogTitle,
                        onSeeAll = { onOpenCategory(type) }
                    )
                }
            }
        }

        if (watching.isNotEmpty()) {
            item {
                LibraryShelf("Continue watching", watching, onOpenTitle) {
                    onOpenStatus(LibraryStatus.WATCHING)
                }
            }
        }
        if (watchlist.isNotEmpty()) {
            item {
                LibraryShelf("Your watchlist", watchlist, onOpenTitle) {
                    onOpenStatus(LibraryStatus.PLAN_TO_WATCH)
                }
            }
        }
        if (finished.isNotEmpty()) {
            item {
                LibraryShelf("Recently finished", finished, onOpenTitle) {
                    onOpenStatus(LibraryStatus.COMPLETED)
                }
            }
        }

        if (starter.isNotEmpty() && (live.isEmpty() || !tmdbConfigured)) {
            item {
                CatalogShelf(
                    title = "Starter picks",
                    subtitle = "A small catalogue is available even before you connect a provider",
                    results = starter.take(12),
                    library = items,
                    onOpen = onOpenCatalogTitle,
                    onSeeAll = onDiscover
                )
            }
        }

        if (discoveryState.error != null || !tmdbConfigured) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(VistoraPalette.Surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        if (discoveryState.error != null) "Live catalogue is temporarily unavailable"
                        else "Connect movie and TV discovery",
                        color = VistoraPalette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (discoveryState.error != null)
                            "Your library and starter picks still work. Try refreshing the catalogue."
                        else
                            "Anime and starter picks are available now. Add your TMDB developer credential in Settings for live movie, K-Drama and TV shelves.",
                        color = VistoraPalette.Muted, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    if (discoveryState.error != null) {
                        VistoraAction("Refresh catalogue", onRefresh, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item {
            Text(
                if (items.isEmpty())
                    "Your recommendations are private and start learning from the titles you add, finish, drop and rate."
                else
                    "Your taste profile is calculated on this device from your library activity. Nothing is sent to a recommendation server.",
                color = VistoraPalette.Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
            VistoraAction(
                label = "Explore all titles",
                onClick = onDiscover,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                leading = "discover",
                quiet = true
            )
        }
    }
}

@Composable
private fun HomeHeader(
    loading: Boolean,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("VISTORA", color = VistoraPalette.Text, fontFamily = FontFamily.Serif,
                fontSize = 23.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
            Text("YOUR NEXT FAVOURITE STARTS HERE", color = VistoraPalette.Muted,
                fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderIcon("search", "Search titles", onSearch)
            HeaderIcon("bell", "Notifications", onNotifications)
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Refresh catalogue"; role = Role.Button }
                    .clickable(role = Role.Button, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = VistoraPalette.Rose)
                else VistoraIcon("refresh", Modifier.size(19.dp), VistoraPalette.Secondary)
            }
        }
    }
}

@Composable
private fun FeaturedTitle(
    result: CatalogResult,
    library: List<MediaItem>,
    isPersonalized: Boolean,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (isPersonalized) "MADE FOR YOUR TASTE" else "A TITLE WORTH EXPLORING",
            color = VistoraPalette.Rose, fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4B2735), Color(0xFF2C2029), Color(0xFF17191F))))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterArt(result.title, result.type.category, result.posterUrl, Modifier.width(118.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("FEATURED FOR YOU", color = Color(0xFFE1A9BC), fontSize = 8.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(result.title, color = Color.White, fontFamily = FontFamily.Serif,
                    fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(result.type.label, result.releaseYear?.toString()).joinToString(" · "),
                    color = Color.White.copy(alpha = .78f), fontSize = 11.sp
                )
                Text(
                    result.genres.take(3).joinToString(" · ").ifBlank { result.type.category },
                    color = Color.White.copy(alpha = .72f), fontSize = 10.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                result.rating?.let {
                    Text("★ " + "%.1f".format(Locale.ROOT, it) + " / 10",
                        color = Color(0xFFF2D7A4), fontSize = 11.sp)
                }
                Text(RecommendationEngine.reasonFor(result, library), color = Color(0xFFE4B7C7),
                    fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("View details  →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CategoryStrip(onOpenCategory: (MediaType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Browse your way", color = VistoraPalette.Text, fontFamily = FontFamily.Serif,
            fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaType.entries.forEach { type ->
                Text(
                    type.category,
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(999.dp))
                        .background(VistoraPalette.Surface)
                        .clickable(role = Role.Button) { onOpenCategory(type) }
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    color = VistoraPalette.Secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CatalogShelf(
    title: String,
    subtitle: String?,
    results: List<CatalogResult>,
    library: List<MediaItem>,
    onOpen: (CatalogResult) -> Unit,
    onSeeAll: () -> Unit
) {
    if (results.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(title, "See all", onAction = onSeeAll)
        if (!subtitle.isNullOrBlank()) Text(subtitle, color = VistoraPalette.Muted, fontSize = 11.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results, key = { it.providerId }) { result ->
                Column(Modifier.width(112.dp).clickable(role = Role.Button) { onOpen(result) }) {
                    PosterArt(result.title, result.type.category, result.posterUrl, Modifier.fillMaxWidth())
                    Text(result.title, color = VistoraPalette.Text, fontSize = 12.sp,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp))
                    Text(
                        listOfNotNull(result.type.label, result.releaseYear?.toString()).joinToString(" · "),
                        color = VistoraPalette.Muted, fontSize = 10.sp, maxLines = 1
                    )
                    if (result.rating != null) {
                        Text("★ " + "%.1f".format(Locale.ROOT, result.rating),
                            color = VistoraPalette.Rose, fontSize = 10.sp)
                    } else {
                        Text(RecommendationEngine.reasonFor(result, library), color = VistoraPalette.Muted,
                            fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryShelf(
    title: String,
    items: List<MediaItem>,
    onOpenTitle: (Long) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(title, "View all", onAction = onSeeAll)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { item ->
                PosterTile(item = item, onClick = { onOpenTitle(item.id) },
                    modifier = Modifier.width(112.dp), compact = true)
            }
        }
    }
}

@Composable
private fun HeaderIcon(name: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).semantics { contentDescription = description; role = Role.Button }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        VistoraIcon(name, Modifier.size(20.dp), VistoraPalette.Text)
    }
}

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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vistora.android.domain.CatalogResult
import com.vistora.android.domain.CatalogSearchState
import com.vistora.android.domain.MediaItem
import com.vistora.android.domain.MediaType
import com.vistora.android.domain.RecommendationEngine
import com.vistora.android.domain.StarterCatalogue
import com.vistora.android.ui.components.PosterArt
import com.vistora.android.ui.components.VistoraAction
import com.vistora.android.ui.components.VistoraIcon
import com.vistora.android.ui.components.VistoraTextField
import com.vistora.android.ui.theme.VistoraPalette

@Composable
fun ExploreScreen(
    library: List<MediaItem>,
    discoveryState: CatalogSearchState,
    searchState: CatalogSearchState,
    tmdbConfigured: Boolean,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String, MediaType?) -> Unit,
    onClearSearch: () -> Unit,
    onOpenTitle: (CatalogResult) -> Unit,
    initialTypeFilter: MediaType? = null,
    startInSearch: Boolean = false
) {
    var searchVisible by rememberSaveable(startInSearch) { mutableStateOf(startInSearch) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable(initialTypeFilter) { mutableStateOf(initialTypeFilter) }
    val alreadyAdded = remember(library) { library.map { it.type to it.title.lowercase() }.toSet() }
    val available = remember(discoveryState.results, alreadyAdded, selectedType) {
        discoveryState.results.filter {
            (selectedType == null || it.type == selectedType) &&
                (it.type to it.title.lowercase()) !in alreadyAdded
        }
    }
    val fallback = remember(alreadyAdded, selectedType) {
        StarterCatalogue.results.filter {
            (selectedType == null || it.type == selectedType) &&
                (it.type to it.title.lowercase()) !in alreadyAdded
        }
    }
    val catalogue = (available + fallback.filterNot { starter -> available.any { it.type == starter.type && it.title.equals(starter.title,true) } }).distinctBy { it.type to it.title.lowercase() }
    val ranked = remember(catalogue, library) { RecommendationEngine.rank(catalogue, library) }
    val searchResults = remember(searchState.results, selectedType, alreadyAdded) {
        searchState.results.filter { (selectedType == null || it.type == selectedType) && (it.type to it.title.lowercase()) !in alreadyAdded }
    }
    val topGenre = remember(library) { RecommendationEngine.favouriteGenre(library) }

    LaunchedEffect(Unit) { onLoad() }

    Column(Modifier.fillMaxSize().padding(horizontal=16.dp).padding(top=8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
            Column {
                Text("VISTORA",color=VistoraPalette.Text,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=3.sp)
                Text("Explore titles",color=VistoraPalette.Text,fontSize=30.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable { onRefresh() },contentAlignment=Alignment.Center) {
                    if (discoveryState.isLoading) CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp,color=VistoraPalette.Rose)
                    else VistoraIcon("refresh",Modifier.size(20.dp),VistoraPalette.Secondary)
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable { searchVisible = !searchVisible },contentAlignment=Alignment.Center) {
                    VistoraIcon("search",Modifier.size(21.dp),VistoraPalette.Text)
                }
            }
        }
        if (searchVisible) {
            Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(14.dp)).background(VistoraPalette.Surface).padding(start=12.dp,end=5.dp),
                verticalAlignment=Alignment.CenterVertically) {
                VistoraIcon("search",Modifier.size(18.dp),VistoraPalette.Muted)
                Spacer(Modifier.width(8.dp))
                VistoraTextField(value=query,onValueChange={query=it},placeholder="Search titles to explore",modifier=Modifier.weight(1f))
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled=query.isNotBlank()&&!searchState.isLoading) { onSearch(query,selectedType) },contentAlignment=Alignment.Center) {
                    VistoraIcon("arrow-right",Modifier.size(18.dp),VistoraPalette.Rose)
                }
            }
        }
        if (searchVisible || selectedType != null) {
            Row(Modifier.fillMaxWidth().padding(top=7.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TypeChip("All",selectedType==null){selectedType=null}
                MediaType.entries.forEach { type -> TypeChip(type.category,selectedType==type){selectedType=type} }
            }
        }
        when {
            searchVisible && searchState.isLoading -> Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=VistoraPalette.Rose)
                Text("  Finding titles…",color=VistoraPalette.Secondary,fontSize=13.sp)
            }
            searchVisible && searchState.query.isNotBlank() && !searchState.isLoading -> {
                Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.End) {
                    Text("Clear results",Modifier.clickable { query="";onClearSearch() }.padding(8.dp),color=VistoraPalette.Rose,fontSize=12.sp)
                }
            }
        }
        if (searchVisible && searchState.query.isNotBlank() && !searchState.isLoading) {
            if (searchResults.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Text(searchState.error ?: "No matches found. Try another title.",color=VistoraPalette.Muted,fontSize=13.sp)
                }
            } else {
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=12.dp,bottom=24.dp)) {
                    items(searchResults,key={it.providerId}) { result -> CatalogRow(result){onOpenTitle(result)} }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(23.dp),contentPadding=PaddingValues(top=12.dp,bottom=28.dp)) {
                if (ranked.isNotEmpty()) {
                    item {
                        val hero = ranked.first()
                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(if(library.isEmpty()) "A good place to start" else "Picked for you",color=VistoraPalette.Text,fontSize=24.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium)
                            Row(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(19.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF422A34),Color(0xFF26262E),Color(0xFF17191F))))
                                .clickable { onOpenTitle(hero) }.padding(13.dp),
                                horizontalArrangement=Arrangement.spacedBy(13.dp),verticalAlignment=Alignment.CenterVertically) {
                                PosterArt(hero.title,hero.type.category,hero.posterUrl,Modifier.width(112.dp))
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Text(if(library.isEmpty()) "FEATURED PICK" else "YOUR TASTE, YOUR FEED",color=VistoraPalette.Rose,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                                    Text(hero.title,color=Color.White,fontSize=20.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
                                    Text(hero.genres.take(3).joinToString(" · ").ifBlank { hero.type.label },color=Color.White.copy(alpha=.76f),fontSize=11.sp,maxLines=2)
                                    Text("Explore title  →",color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                if (library.isNotEmpty() && ranked.isNotEmpty()) item { Shelf("Picked for you",ranked.take(12),onOpenTitle) }
                if (fallback.isNotEmpty()) item { Shelf("Starter picks",fallback.take(12),onOpenTitle) }
                if (topGenre != null) {
                    val genrePicks = ranked.filter { result -> result.genres.any { it.equals(topGenre,true) } }
                    if (genrePicks.isNotEmpty()) item { Shelf("Because you like " + topGenre,genrePicks.take(12),onOpenTitle) }
                }
                if (available.isNotEmpty()) {
                    MediaType.entries.forEach { type ->
                        val titles = available.filter { it.type == type }
                        if (titles.isNotEmpty()) item { Shelf("Popular " + type.category,titles.take(12),onOpenTitle) }
                    }
                }
                if (catalogue.isEmpty() && !discoveryState.isLoading) {
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VistoraPalette.Surface).padding(18.dp)) {
                            Text("Explore is having trouble loading",color=VistoraPalette.Text,fontWeight=FontWeight.SemiBold)
                            Text(discoveryState.error ?: "Check your connection and try again.",color=VistoraPalette.Muted,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
                            if (!tmdbConfigured) Text("Anime discovery is available without a TMDB key. Add a TMDB key in Settings for movie and TV shelves.",color=VistoraPalette.Muted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
                            VistoraAction("Try again",onRefresh,Modifier.fillMaxWidth().padding(top=12.dp))
                        }
                    }
                } else if (discoveryState.error != null) {
                    item { Text("Live catalogue is unavailable right now. Starter picks are still here, and your library stays on this device.",color=VistoraPalette.Muted,fontSize=11.sp,lineHeight=16.sp) }
                } else if (!tmdbConfigured) {
                    item { Text("Anime discovery works without a TMDB key. Add a free TMDB developer key in Settings for live movie and TV shelves.",color=VistoraPalette.Muted,fontSize=11.sp,lineHeight=16.sp) }
                }
                item {
                    Text(
                        if (library.isEmpty()) "Your feed becomes more personal as you add, rate and track titles. Your taste profile is calculated on this device."
                        else "Your recommendations are calculated on this device from your ratings, watch progress and library statuses.",
                        color=VistoraPalette.Muted,fontSize=11.sp,lineHeight=16.sp
                    )
                }
                if (discoveryState.isLoading && available.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator(color=VistoraPalette.Rose) } }
            }
        }
    }
}

@Composable
private fun Shelf(title:String,results:List<CatalogResult>,onOpen:(CatalogResult)->Unit) {
    if(results.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=VistoraPalette.Text,fontSize=21.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(results,key={it.providerId}) { result ->
                Column(Modifier.width(112.dp).clickable { onOpen(result) }) {
                    PosterArt(result.title,result.type.category,result.posterUrl,Modifier.fillMaxWidth())
                    Text(result.title,color=VistoraPalette.Text,fontSize=12.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=7.dp))
                    Text(listOfNotNull(result.type.label,result.releaseYear?.toString()).joinToString(" · "),color=VistoraPalette.Muted,fontSize=10.sp,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(result:CatalogResult,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(VistoraPalette.Surface).clickable(onClick=onClick).padding(9.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        PosterArt(result.title,result.type.category,result.posterUrl,Modifier.width(62.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(result.title,color=VistoraPalette.Text,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
            Text(listOfNotNull(result.type.label,result.releaseYear?.toString()).joinToString(" · "),color=VistoraPalette.Muted,fontSize=11.sp)
            Text(result.genres.take(3).joinToString(" · "),color=VistoraPalette.Secondary,fontSize=11.sp,maxLines=1)
            Text("View details",color=VistoraPalette.Rose,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TypeChip(label:String,selected:Boolean,onClick:()->Unit) {
    Text(label,Modifier.clip(RoundedCornerShape(999.dp)).background(if(selected)VistoraPalette.Burgundy else VistoraPalette.Surface)
        .clickable(onClick=onClick).padding(horizontal=12.dp,vertical=8.dp),
        color=if(selected)VistoraPalette.OnAccent else VistoraPalette.Secondary,fontSize=12.sp,fontWeight=FontWeight.Medium)
}


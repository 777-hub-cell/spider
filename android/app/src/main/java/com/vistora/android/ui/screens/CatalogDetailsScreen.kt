package com.vistora.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vistora.android.domain.CatalogResult
import com.vistora.android.domain.SeriesState
import com.vistora.android.ui.components.PosterArt
import com.vistora.android.ui.components.VistoraAction
import com.vistora.android.ui.theme.VistoraPalette
import java.util.Locale

@Composable
fun CatalogDetailsScreen(result: CatalogResult, onBack: () -> Unit, onAddToLibrary: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        VistoraAction("Back to Explore", onBack, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            leading = "back", quiet = true)
        PosterArt(result.title, result.type.category, result.posterUrl, Modifier.fillMaxWidth(.58f))
        Text(result.title, color = VistoraPalette.Text, fontSize = 30.sp, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
        Text(listOfNotNull(result.type.label, result.releaseYear?.toString()).joinToString(" · "),
            color = VistoraPalette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            result.rating?.let { MetadataPill("★ " + "%.1f".format(Locale.ROOT, it) + " / 10") }
            if (result.totalEpisodes > 0) MetadataPill(result.totalEpisodes.toString() + " episodes")
            if (result.seriesState != SeriesState.UNKNOWN) MetadataPill(result.seriesState.label)
        }
        if (result.genres.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                result.genres.take(5).forEach { genre -> MetadataPill(genre) }
            }
        }
        Text("About this title", color = VistoraPalette.Text, fontSize = 20.sp,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
        Text(result.synopsis.ifBlank {
            "A synopsis is not available from this catalogue. You can still save the title and add your own notes later."
        }, color = VistoraPalette.Secondary, fontSize = 14.sp, lineHeight = 21.sp)
        Text("Catalogue: " + result.source, color = VistoraPalette.Muted, fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp))
        Text("Adding this title saves it to your library right away. You can edit details later.",
            color = VistoraPalette.Muted, fontSize = 12.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 15.dp))
        VistoraAction("Add to Library", onAddToLibrary,
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 30.dp), leading = "plus")
    }
}

@Composable
private fun MetadataPill(label: String) {
    Text(label, Modifier.clip(RoundedCornerShape(999.dp)).background(VistoraPalette.Surface)
        .padding(horizontal = 10.dp, vertical = 7.dp), color = VistoraPalette.Secondary, fontSize = 11.sp)
}

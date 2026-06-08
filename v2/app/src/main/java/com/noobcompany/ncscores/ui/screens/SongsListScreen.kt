package com.noobcompany.ncscores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noobcompany.ncscores.model.Resource
import com.noobcompany.ncscores.model.Song
import com.noobcompany.ncscores.ui.theme.DarkBackground
import com.noobcompany.ncscores.ui.theme.DarkSurface
import com.noobcompany.ncscores.ui.theme.PremiumGold
import com.noobcompany.ncscores.ui.theme.TextPrimary
import com.noobcompany.ncscores.ui.theme.TextSecondary
import com.noobcompany.ncscores.viewmodel.SongsViewModel

@Composable
fun SongsListScreen(
    viewModel: SongsViewModel,
    onSongClick: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongsState by viewModel.filteredSongs.collectAsState()

    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val selectedKey by viewModel.selectedKey.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    val difficulties = listOf("Easy", "Intermediate", "Hard")
    val instruments = listOf("Piano", "Organ", "Violin", "Keyboard")
    val keys = listOf("C# Minor", "G Major", "D Minor", "C Minor", "D Major")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Sticky Header & Search Area with beautiful OLED highlight border
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = "LIBRARY CATALOG",
                color = PremiumGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("score_search_input"),
                    placeholder = { Text("Search songs, artists...", color = TextSecondary) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PremiumGold
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        cursorColor = PremiumGold,
                        focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier
                        .background(
                            if (showFilters) PremiumGold else DarkBackground,
                            RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Toggles filters drawer",
                        tint = if (showFilters) DarkBackground else PremiumGold
                    )
                }
            }

            AnimatedVisibility(visible = showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Reset Button
                    if (selectedDifficulty != null || selectedInstrument != null || selectedKey != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Clear All Filters",
                                color = PremiumGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.resetFilters() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Key selector
                    FilterRowHeader(text = "Keys")
                    LazyRow(modifier = Modifier.padding(bottom = 8.dp)) {
                        items(keys) { key ->
                            FilterChip(
                                label = key,
                                selected = selectedKey == key,
                                onClick = { viewModel.selectKey(if (selectedKey == key) null else key) }
                            )
                        }
                    }

                    // Difficulty selector
                    FilterRowHeader(text = "Difficulty")
                    LazyRow(modifier = Modifier.padding(bottom = 8.dp)) {
                        items(difficulties) { diff ->
                            FilterChip(
                                label = diff,
                                selected = selectedDifficulty == diff,
                                onClick = { viewModel.selectDifficulty(if (selectedDifficulty == diff) null else diff) }
                            )
                        }
                    }

                    // Instrument selector
                    FilterRowHeader(text = "Instruments")
                    LazyRow {
                        items(instruments) { instr ->
                            FilterChip(
                                label = instr,
                                selected = selectedInstrument == instr,
                                onClick = { viewModel.selectInstrument(if (selectedInstrument == instr) null else instr) }
                            )
                        }
                    }
                }
            }
        }

        // Search Grid catalog
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            when (val state = filteredSongsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        color = PremiumGold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Error -> {
                    Text(
                        text = "Error gathering library details: ${state.exception.localizedMessage}",
                        color = Color.Red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                is Resource.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Masterpieces Found",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try modifying your search queries or filters.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list) { song ->
                                SongCatalogGridItem(song = song, onSongClick = onSongClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRowHeader(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (selected) PremiumGold else DarkBackground,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) DarkBackground else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SongCatalogGridItem(
    song: Song,
    onSongClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_item_${song.id}")
            .clickable { onSongClick(song.id) }
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumCover)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (song.isComingSoon) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SOON",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artistNames.firstOrNull() ?: "",
                    color = PremiumGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Key: ${song.originalKey}",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    
                    if (song.isPremium) {
                        Text(
                            text = "Gold",
                            color = PremiumGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Free",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

package com.noobcompany.ncscores.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.noobcompany.ncscores.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (String) -> Unit
) {
    val featuredState by viewModel.featuredSongsState.collectAsState()
    val recentState by viewModel.recentSongsState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // App Header conforming to Professional Polish UI Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NC SCORES",
                    color = PremiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Digital Library",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
            
            // App Bar Premium Visual Circle (Sleek Search Icon)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(DarkSurface, CircleShape)
                    .border(1.dp, Color(0x0DFFFFFF), CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.8.dp, PremiumGold, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 1.8.dp)
                            .background(PremiumGold)
                            .align(androidx.compose.ui.Alignment.BottomEnd)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. FEATURED ARRANGEMENTS SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Featured Arrangement",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "View All",
                color = PremiumGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* No-op action */ }
            )
        }

        when (val state = featuredState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator(color = PremiumGold)
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "Unable to load featured list: ${state.exception.localizedMessage}",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
            is Resource.Success -> {
                FeaturedCarousel(songs = state.data, onSongClick = onSongClick)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. CONTINUE PRACTICING (RECENTS) SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Viewed",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            // Indicator dots matching design HTML
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(PremiumGold, CircleShape))
                Box(modifier = Modifier.size(6.dp).background(TextSecondary.copy(alpha = 0.3f), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(TextSecondary.copy(alpha = 0.3f), CircleShape))
            }
        }

        when (val state = recentState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator(color = PremiumGold, modifier = Modifier.size(24.dp))
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(text = "Unpacking recent scores failed.", color = Color.Red, fontSize = 13.sp)
                }
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(DarkSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "Browse catalogue to start cataloging sheet music arrangements.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    RecentSongsList(songs = state.data, onSongClick = onSongClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Mini-Player Mock adhering precisely to colors, borders and styling of the Polish HTML
        BottomPlayerBlock(
            songTitle = recentState.let {
                if (it is Resource.Success && it.data.isNotEmpty()) {
                    it.data.first().title
                } else {
                    "Symphony No. 5 in C Minor"
                }
            }
        )
    }
}

@Composable
fun FeaturedCarousel(
    songs: List<Song>,
    onSongClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(songs) { song ->
            // Card layout matching the aspect-[16/9] premium card representation in the HTML
            Card(
                modifier = Modifier
                    .width(290.dp)
                    .height(180.dp)
                    .testTag("featured_card_${song.id}")
                    .clickable { onSongClick(song.id) },
                shape = RoundedCornerShape(24.dp), // rounded-3xl corresponding to 24dp in Compose
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    Color(0xFF121A2C)
                                )
                            )
                        )
                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
                ) {
                    // Custom radial ambient glows built directly in background matching HTML details
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(androidx.compose.ui.Alignment.TopEnd)
                            .background(PremiumGold.copy(alpha = 0.08f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(androidx.compose.ui.Alignment.BottomStart)
                            .background(PremiumGold.copy(alpha = 0.04f), CircleShape)
                    )

                    // Cover artwork as back layer, faded out slightly on left/bottom
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.albumCover)
                            .crossfade(true)
                            .build(),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    // Deep atmospheric rich overlay gradient 
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Foreground layout detailing premium status, beautiful serif/italic title, and info specs
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            if (song.isPremium) {
                                Box(
                                    modifier = Modifier
                                        .background(PremiumGold, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "PREMIUM",
                                        color = Color(0xFF0F172A),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Accent Circle Play Button mimicking the theme's glassmorphism player visual
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(PremiumGold, RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        Column {
                            Text(
                                text = song.artistNames.firstOrNull() ?: "Composer Legend",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Stunning serif-italic custom title look from Polish Guidelines
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Piano • Grade 8 • Key: ${song.originalKey}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentSongsList(
    songs: List<Song>,
    onSongClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (song in songs) {
            // Elegant modern card item with clean layout structure, precise corner rounding & white/5 border
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_card_${song.id}")
                    .clickable { onSongClick(song.id) },
                shape = RoundedCornerShape(16.dp), // rounded-2xl
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF334155)))),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.albumCover)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover description for ${song.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
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
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .border(1.dp, PremiumGold.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = song.originalKey,
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayerBlock(songTitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumGold),
        shape = RoundedCornerShape(16.dp), // rounded-2xl
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Background container matching design HTML player icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Practice Active Symbol",
                        tint = PremiumGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Practice Mode Active",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = songTitle,
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Pause Indicator button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, Color(0xFF0F172A).copy(alpha = 0.2f), CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 11.dp)
                            .background(Color(0xFF0F172A))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 11.dp)
                            .background(Color(0xFF0F172A))
                    )
                }
            }
        }
    }
}

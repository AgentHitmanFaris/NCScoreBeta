package com.noobcompany.ncscores.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement as LayoutArrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noobcompany.ncscores.data.FirestoreService
import com.noobcompany.ncscores.model.Arrangement
import com.noobcompany.ncscores.model.Resource
import com.noobcompany.ncscores.model.Song
import com.noobcompany.ncscores.ui.theme.DarkBackground
import com.noobcompany.ncscores.ui.theme.DarkSurface
import com.noobcompany.ncscores.ui.theme.PremiumGold
import com.noobcompany.ncscores.ui.theme.TextPrimary
import com.noobcompany.ncscores.ui.theme.TextSecondary

@Composable
fun SongDetailsScreen(
    songId: String,
    firestoreService: FirestoreService,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPdfOpen: (String, String) -> Unit,
    onInteractiveRegister: () -> Unit
) {
    val context = LocalContext.current
    var songDetailsState by remember { mutableStateOf<Resource<Song>>(Resource.Loading) }
    var arrangementsState by remember { mutableStateOf<Resource<List<Arrangement>>>(Resource.Loading) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Fetch details
    LaunchedEffect(songId) {
        songDetailsState = Resource.Loading
        arrangementsState = Resource.Loading
        try {
            val song = firestoreService.getSongDetails(songId)
            if (song != null) {
                songDetailsState = Resource.Success(song)
                
                // Track interaction
                onInteractiveRegister()

                val arrangements = firestoreService.getArrangements(songId)
                arrangementsState = Resource.Success(arrangements)
            } else {
                songDetailsState = Resource.Error(Exception("Song score profile detail lookup failed"))
            }
        } catch (e: Exception) {
            songDetailsState = Resource.Error(e)
            arrangementsState = Resource.Error(e)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(DarkSurface.copy(alpha = 0.8f), CircleShape)
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = PremiumGold
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            when (val details = songDetailsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        color = PremiumGold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Unpacking Details",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = details.exception.localizedMessage ?: "Unknown compilation error",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is Resource.Success -> {
                    val song = details.data
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Visual Hero artwork
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(song.albumCover)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Glass scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                DarkBackground.copy(alpha = 0.4f),
                                                DarkBackground
                                            )
                                        )
                                    )
                            )

                            // Overlaid title details
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (song.isPremium) {
                                        Box(
                                            modifier = Modifier
                                                .background(PremiumGold, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "GOLD SCORE",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    if (song.isComingSoon) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Red, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "COMING SOON",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, PremiumGold, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Key: ${song.originalKey}",
                                            color = PremiumGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Classical serif title
                                Text(
                                    text = song.title,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 32.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Clickable artist link
                                Row(
                                    modifier = Modifier.clickable {
                                        val firstArtistId = song.artistIds.firstOrNull()
                                        if (firstArtistId != null) {
                                            onArtistClick(firstArtistId)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "By ",
                                        color = TextSecondary,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = song.artistNames.joinToString(", "),
                                        color = PremiumGold,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("artist_redirect_link")
                                    )
                                }
                            }
                        }

                        // Playback helper
                        if (song.video.isNotEmpty()) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(song.video))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open video link launcher", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("youtube_video_launcher"),
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Reference Video Masterclass",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Tab selectors
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = DarkSurface,
                            contentColor = PremiumGold,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = PremiumGold
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Sheet Arrangements", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Performance Lyrics", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }

                        // Content matching specific tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (selectedTab == 0) {
                                // Arrangements Content
                                when (val arrs = arrangementsState) {
                                    is Resource.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = PremiumGold, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    is Resource.Error -> {
                                        Text(
                                            text = "Arrangements fail to fetch: ${arrs.exception.localizedMessage}",
                                            color = Color.Red,
                                            fontSize = 13.sp
                                        )
                                    }
                                    is Resource.Success -> {
                                        val list = arrs.data
                                        if (list.isEmpty()) {
                                            Text(
                                                text = "No arrangements documented for this song yet.",
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                                            )
                                        } else {
                                            for (arr in list) {
                                                ArrangementRowItem(
                                                    arrangement = arr,
                                                    onPdfClick = {
                                                        onPdfOpen(arr.downloadLink, song.title)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Lyrics Section
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Lyrics Sheet & Sync Notes",
                                            color = PremiumGold,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = PremiumGold.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = song.lyrics.ifEmpty { "No lyrics recorded. Traditional instrumental masterpiece." },
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            textAlign = if (song.lyrics.isEmpty()) TextAlign.Center else TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArrangementRowItem(
    arrangement: Arrangement,
    onPdfClick: () -> Unit
) {
    val difficultyColor = when (arrangement.difficulty.lowercase()) {
        "easy" -> Color(0xFF10B981)          // Emerald green
        "intermediate" -> Color(0xFFF59E0B)  // Golden amber
        "hard" -> Color(0xFFEF4444)          // Soft red
        else -> PremiumGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onPdfClick() }
            .testTag("arrangement_item_${arrangement.id}")
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = arrangement.instruments.joinToString(" • "),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Downloadable interactive PDF",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(difficultyColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, difficultyColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = arrangement.difficulty,
                    color = difficultyColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

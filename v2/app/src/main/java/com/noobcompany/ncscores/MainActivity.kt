package com.noobcompany.ncscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noobcompany.ncscores.data.FirestoreService
import com.noobcompany.ncscores.ui.pdf.PdfViewerScreen
import com.noobcompany.ncscores.ui.screens.ArtistScreen
import com.noobcompany.ncscores.ui.screens.HomeScreen
import com.noobcompany.ncscores.ui.screens.ProfileScreen
import com.noobcompany.ncscores.ui.screens.SongDetailsScreen
import com.noobcompany.ncscores.ui.screens.SongsListScreen
import com.noobcompany.ncscores.ui.theme.DarkSurface
import com.noobcompany.ncscores.ui.theme.NCScoresTheme
import com.noobcompany.ncscores.ui.theme.PremiumGold
import com.noobcompany.ncscores.ui.theme.TextPrimary
import com.noobcompany.ncscores.ui.theme.TextSecondary
import com.noobcompany.ncscores.viewmodel.ArtistViewModel
import com.noobcompany.ncscores.viewmodel.HomeViewModel
import com.noobcompany.ncscores.viewmodel.SongsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NCScoresTheme {
                MainAppHost()
            }
        }
    }
}

sealed class NavigationTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : NavigationTab("home", "Home", Icons.Default.Home)
    object Catalog : NavigationTab("catalog", "Library", Icons.Default.Search)
    object Profile : NavigationTab("profile", "Profile", Icons.Default.AccountCircle)
}

@Composable
fun MainAppHost() {
    val navController = rememberNavController()
    val firestoreService = remember { FirestoreService() }

    // Direct memory overlay state for safe offline deep-linking of PDF structures
    var activePdfUrl by remember { mutableStateOf<String?>(null) }
    var activePdfTitle by remember { mutableStateOf<String?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tab list for navigation
    val navigationTabs = listOf(
        NavigationTab.Home,
        NavigationTab.Catalog,
        NavigationTab.Profile
    )

    if (activePdfUrl != null && activePdfTitle != null) {
        // Direct native high performance view overlay with safe memory backup hooks
        PdfViewerScreen(
            pdfUrl = activePdfUrl!!,
            songTitle = activePdfTitle!!,
            onBackClick = {
                activePdfUrl = null
                activePdfTitle = null
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Render bottom nav only on main root destinations to give maximal reading area for scores
                val isRootDestination = navigationTabs.any { it.route == currentRoute }
                if (isRootDestination) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("portal_bottom_navigation")
                    ) {
                        navigationTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DarkSurface,
                                    selectedTextColor = PremiumGold,
                                    indicatorColor = PremiumGold,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = NavigationTab.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                // 1. HOME tab
                composable(NavigationTab.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(firestoreService) }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onSongClick = { songId ->
                            navController.navigate("song_details/$songId")
                        }
                    )
                }

                // 2. LIBRARY CATALOG tab
                composable(NavigationTab.Catalog.route) {
                    val songsViewModel: SongsViewModel = viewModel { SongsViewModel(firestoreService) }
                    SongsListScreen(
                        viewModel = songsViewModel,
                        onSongClick = { songId ->
                            navController.navigate("song_details/$songId")
                        }
                    )
                }

                // 3. PROFILE tab
                composable(NavigationTab.Profile.route) {
                    ProfileScreen(userEmail = "noobpianizt@gmail.com")
                }

                // 4. SONG DETAILS flow (not in BottomBar)
                composable(
                    route = "song_details/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getString("songId") ?: ""
                    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(firestoreService) }
                    
                    SongDetailsScreen(
                        songId = songId,
                        firestoreService = firestoreService,
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onPdfOpen = { link, songTitle ->
                            activePdfUrl = link
                            activePdfTitle = songTitle
                        },
                        onInteractiveRegister = {
                            homeViewModel.registerSongInteracted(songId)
                        }
                    )
                }

                // 5. ARTIST DETAILS flow (not in BottomBar)
                composable(
                    route = "artist/{artistId}",
                    arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                    val artistViewModel: ArtistViewModel = viewModel { ArtistViewModel(firestoreService) }
                    
                    ArtistScreen(
                        artistId = artistId,
                        viewModel = artistViewModel,
                        onBackClick = { navController.popBackStack() },
                        onSongClick = { songId ->
                            navController.navigate("song_details/$songId")
                        }
                    )
                }
            }
        }
    }
}

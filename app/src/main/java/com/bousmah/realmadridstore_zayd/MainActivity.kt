package com.bousmah.realmadridstore_zayd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.bousmah.realmadridstore_zayd.ui.*
import com.bousmah.realmadridstore_zayd.ui.theme.RMGold
import com.bousmah.realmadridstore_zayd.ui.theme.RMNavy
import com.bousmah.realmadridstore_zayd.ui.theme.RealMadridStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealMadridStoreTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Shop : Screen("shop", "Shop", Icons.Default.ShoppingBag)
    object Locator : Screen("locator", "Locator", Icons.Default.LocationOn)
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Cart : Screen("cart", "Cart", Icons.Default.ShoppingCart)
    object Payment : Screen("payment", "Payment", Icons.Default.ShoppingCart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val shopViewModel: ShopViewModel = viewModel()
    val shopUiState by shopViewModel.uiState.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomNavItems = listOf(
        Screen.Shop,
        Screen.Locator,
        Screen.Chat
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    AsyncImage(
                        model = "https://s3-eu-west-1.amazonaws.com/tpd/logos/575a691d0000ff00058fe0e7/0x0.png",
                        contentDescription = "Real Madrid",
                        modifier = Modifier.height(48.dp).width(180.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                navigationIcon = {
                    if (currentRoute == Screen.Cart.route || currentRoute == Screen.Payment.route) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RMNavy)
                        }
                    }
                },
                actions = {
                    if (currentRoute != Screen.Cart.route && currentRoute != Screen.Payment.route) {
                        IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                            BadgedBox(
                                badge = {
                                    if (shopUiState.cartItemCount > 0) {
                                        Badge(containerColor = RMGold) {
                                            Text(shopUiState.cartItemCount.toString(), color = RMNavy)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = RMNavy)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (currentRoute != Screen.Cart.route && currentRoute != Screen.Payment.route) {
                NavigationBar(containerColor = RMNavy) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RMNavy,
                                selectedTextColor = RMGold,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = RMGold
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = Screen.Shop.route) {
                composable(Screen.Shop.route) { ShopScreen(shopViewModel) }
                composable(Screen.Locator.route) { StoreLocatorScreen() }
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Cart.route) { 
                    CartScreen(shopViewModel, onProceedToPayment = {
                        navController.navigate(Screen.Payment.route)
                    })
                }
                composable(Screen.Payment.route) {
                    PaymentScreen(shopViewModel, onOrderConfirmed = {
                        navController.popBackStack(Screen.Shop.route, false)
                    })
                }
            }
        }
    }
}

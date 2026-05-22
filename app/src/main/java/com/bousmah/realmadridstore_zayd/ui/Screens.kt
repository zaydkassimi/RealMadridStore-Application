package com.bousmah.realmadridstore_zayd.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bousmah.realmadridstore_zayd.BuildConfig
import com.bousmah.realmadridstore_zayd.data.ChatMessage
import com.bousmah.realmadridstore_zayd.data.MockData
import com.bousmah.realmadridstore_zayd.data.Product
import com.bousmah.realmadridstore_zayd.data.Store
import com.bousmah.realmadridstore_zayd.ui.theme.RMGold
import com.bousmah.realmadridstore_zayd.ui.theme.RMNavy
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(viewModel: ShopViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.categories) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RMNavy,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Product Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.products) { product ->
                ProductCard(
                    product = product,
                    isFavorite = uiState.wishlist.contains(product.id),
                    onAddToCart = { viewModel.addToCart(product) },
                    onFavoriteToggle = { viewModel.toggleWishlist(product.id) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onAddToCart: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.storeUrl))
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            Column {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "€${product.price}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RMNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RMNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add to Cart", fontSize = 12.sp)
                    }
                }
            }
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) RMGold else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreLocatorScreen() {
    val stores = MockData.stores
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val madrid = LatLng(40.453054, -3.688344)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(madrid, 10f)
    }

    LaunchedEffect(Unit) {
        Log.d("MAPS_KEY", "Key injected: ${BuildConfig.MAPS_API_KEY}")
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("StoreLocator", "Permission granted")
            getUserLocation(context, fusedLocationClient, coroutineScope, cameraPositionState, stores) {
                userLocation = it
            }
        } else {
            Log.d("StoreLocator", "Permission denied")
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            stores.forEach { store ->
                Marker(
                    state = MarkerState(position = LatLng(store.latitude, store.longitude)),
                    title = store.name,
                    onClick = {
                        selectedStore = store
                        showBottomSheet = true
                        true
                    }
                )
            }
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "You",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                Log.d("StoreLocator", "Near Me tapped")
                Toast.makeText(context, "Getting location...", Toast.LENGTH_SHORT).show()
                val permission = Manifest.permission.ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    getUserLocation(context, fusedLocationClient, coroutineScope, cameraPositionState, stores) {
                        userLocation = it
                    }
                } else {
                    locationPermissionLauncher.launch(permission)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp),
            containerColor = RMNavy,
            contentColor = Color.White
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Near Me")
            }
        }

        if (showBottomSheet && selectedStore != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                StoreDetailContent(selectedStore!!) {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${selectedStore!!.latitude},${selectedStore!!.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)
                }
            }
        }
    }
}

private fun getUserLocation(
    context: Context,
    client: com.google.android.gms.location.FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    cameraState: CameraPositionState,
    stores: List<Store>,
    onLocationReceived: (LatLng) -> Unit
) {
    try {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    Log.d("StoreLocator", "Location received: ${it.latitude}, ${it.longitude}")
                    Toast.makeText(context, "Location: ${String.format(Locale.US, "%.4f", it.latitude)}, ${String.format(Locale.US, "%.4f", it.longitude)}", Toast.LENGTH_LONG).show()
                    onLocationReceived(latLng)
                    scope.launch {
                        cameraState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
                        )
                    }
                    findClosestStore(it, stores, context)
                } ?: run {
                    Log.d("StoreLocator", "Location is null")
                    Toast.makeText(context, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("StoreLocator", "Error getting location", it)
                Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    } catch (e: SecurityException) {
        Log.e("StoreLocator", "Security exception", e)
    }
}

private fun findClosestStore(userLocation: Location, stores: List<Store>, context: Context) {
    var minDistance = Float.MAX_VALUE
    var closestStore: Store? = null

    for (store in stores) {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude, userLocation.longitude,
            store.latitude, store.longitude,
            results
        )
        if (results[0] < minDistance) {
            minDistance = results[0]
            closestStore = store
        }
    }

    closestStore?.let {
        val distanceKm = minDistance / 1000
        Toast.makeText(
            context,
            "Closest: ${it.name} (${String.format(Locale.US, "%.1f", distanceKm)} km)",
            Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
fun StoreDetailContent(store: Store, onGetDirections: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(text = store.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RMNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = store.address, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = RMNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Open: ${store.openingHours}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGetDirections,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RMGold, contentColor = RMNavy)
        ) {
            Text("Get Directions", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val photoFile = remember {
        File(context.cacheDir, "camera_image.jpg")
    }
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            if (bitmap != null) {
                viewModel.onImageCaptured(bitmap)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isRecording = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(error: Int) { isRecording = false }
            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    viewModel.onInputChange(data[0])
                }
                isRecording = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechRecognizer.startListening(speechIntent)
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            reverseLayout = false
        ) {
            items(uiState.messages) { message ->
                ChatBubble(message)
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RMGold)
        }

        ChatInputBar(
            text = uiState.inputText,
            selectedImage = uiState.selectedImage,
            isRecording = isRecording,
            onTextChange = { viewModel.onInputChange(it) },
            onSend = { viewModel.sendMessage() },
            onCameraClick = {
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(photoUri)
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            onMicClick = {
                if (isRecording) {
                    speechRecognizer.stopListening()
                } else {
                    val permission = Manifest.permission.RECORD_AUDIO
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        speechRecognizer.startListening(speechIntent)
                    } else {
                        audioPermissionLauncher.launch(permission)
                    }
                }
            },
            onRemoveImage = { viewModel.removeSelectedImage() }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) RMNavy else Color(0xFFE0E0E0)
    val textColor = if (message.isUser) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.image != null) {
                    Image(
                        bitmap = message.image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    selectedImage: Bitmap?,
    isRecording: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onRemoveImage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        if (selectedImage != null) {
            Box(modifier = Modifier.padding(8.dp).size(80.dp)) {
                Image(
                    bitmap = selectedImage.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCameraClick) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = RMNavy)
            }
            IconButton(onClick = onMicClick) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isRecording) Color.Red else RMNavy
                )
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about products...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(onClick = onSend) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = RMNavy)
            }
        }
    }
}

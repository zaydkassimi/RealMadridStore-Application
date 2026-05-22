package com.bousmah.realmadridstore_zayd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bousmah.realmadridstore_zayd.data.CartItem
import com.bousmah.realmadridstore_zayd.ui.theme.RMGold
import com.bousmah.realmadridstore_zayd.ui.theme.RMNavy
import java.util.Locale

@Composable
fun CartScreen(viewModel: ShopViewModel, onProceedToPayment: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your cart is empty", style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { viewModel.updateQuantity(item.product, 1) },
                        onDecrease = { viewModel.updateQuantity(item.product, -1) },
                        onRemove = { viewModel.removeFromCart(item.product) }
                    )
                }
            }

            OrderSummary(uiState, onProceedToPayment)
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.Bold)
                Text("€${item.product.price}", color = RMNavy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = RMNavy)
                    }
                    Text("${item.quantity}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = onIncrease) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = RMNavy)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun OrderSummary(state: ShopUiState, onProceed: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow("Subtotal", "€${String.format(Locale.GERMANY, "%.2f", state.subtotal)}")
            SummaryRow("Shipping", "€${String.format(Locale.GERMANY, "%.2f", state.shipping)}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryRow("Total", "€${String.format(Locale.GERMANY, "%.2f", state.total)}", isTotal = true)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RMNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PROCEED TO PAYMENT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isTotal) 18.sp else 16.sp
        )
        Text(
            value,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isTotal) 18.sp else 16.sp,
            color = if (isTotal) RMNavy else Color.Black
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(viewModel: ShopViewModel, onOrderConfirmed: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* No dismiss */ },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onOrderConfirmed()
                }) {
                    Text("OK", color = RMNavy, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Order Confirmed", fontWeight = FontWeight.Bold) },
            text = { Text("Hala Madrid! Your order is confirmed 🏆") },
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Payment Details", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            label = { Text("Card Number") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RMNavy, focusedLabelColor = RMNavy)
        )
        OutlinedTextField(
            value = holderName,
            onValueChange = { holderName = it },
            label = { Text("Cardholder Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RMNavy, focusedLabelColor = RMNavy)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { expiry = it },
                label = { Text("Expiry (MM/YY)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RMNavy, focusedLabelColor = RMNavy)
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = { cvv = it },
                label = { Text("CVV") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RMNavy, focusedLabelColor = RMNavy)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                viewModel.placeOrder {
                    showDialog = true 
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RMGold, contentColor = RMNavy),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("CONFIRM ORDER", fontWeight = FontWeight.Bold)
        }
    }
}

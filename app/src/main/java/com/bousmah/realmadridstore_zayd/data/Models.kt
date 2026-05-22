package com.bousmah.realmadridstore_zayd.data

import android.graphics.Bitmap

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val storeUrl: String = "",
    val isFavorite: Boolean = false
)

data class CartItem(
    val product: Product = Product(),
    var quantity: Int = 1
)

data class Store(
    val name: String = "",
    val address: String = "",
    val openingHours: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class ChatMessage(
    val text: String = "",
    val isUser: Boolean = false,
    val image: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderRequest(
    val items: List<CartItem>,
    val total: Double,
    val userId: String = "anonymous",
    val timestamp: Long = System.currentTimeMillis()
)

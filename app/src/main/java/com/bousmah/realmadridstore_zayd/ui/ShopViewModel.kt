package com.bousmah.realmadridstore_zayd.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bousmah.realmadridstore_zayd.data.BackendRepository
import com.bousmah.realmadridstore_zayd.data.CartItem
import com.bousmah.realmadridstore_zayd.data.MockData
import com.bousmah.realmadridstore_zayd.data.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "wishlist_prefs")

data class ShopUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = listOf("All", "Wishlist", "Jerseys", "Training", "Accessories", "Kids"),
    val selectedCategory: String = "All",
    val cartItems: List<CartItem> = emptyList(),
    val wishlist: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val orderSuccess: Boolean = false
) {
    val cartItemCount: Int get() = cartItems.sumOf { it.quantity }
    val subtotal: Double get() = cartItems.sumOf { it.product.price * it.quantity }
    val shipping: Double = 4.99
    val total: Double get() = subtotal + shipping
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.applicationContext.dataStore
    private val WISHLIST_KEY = stringSetPreferencesKey("wishlist_ids")
    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private var allProducts: List<Product> = emptyList()

    init {
        loadProducts()
        loadWishlist()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val products = repository.getProducts()
                allProducts = if (products.isEmpty()) MockData.products else products
            } catch (e: Exception) {
                Log.e("ShopViewModel", "Failed to load products from backend, using mock data", e)
                allProducts = MockData.products
            }
            
            _uiState.update { it.copy(products = allProducts, isLoading = false) }
            applyFilter(_uiState.value.selectedCategory)
        }
    }

    private fun loadWishlist() {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[WISHLIST_KEY] ?: emptySet()
            }.collect { wishlist ->
                _uiState.update { it.copy(wishlist = wishlist) }
                applyFilter(_uiState.value.selectedCategory)
            }
        }
    }

    fun selectCategory(category: String) {
        applyFilter(category)
    }

    private fun applyFilter(category: String) {
        val filteredProducts = when (category) {
            "All" -> allProducts
            "Wishlist" -> allProducts.filter { _uiState.value.wishlist.contains(it.id) }
            else -> allProducts.filter { it.category == category }
        }
        _uiState.update { it.copy(selectedCategory = category, products = filteredProducts) }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val current = preferences[WISHLIST_KEY] ?: emptySet()
                val updated = if (current.contains(productId)) {
                    current - productId
                } else {
                    current + productId
                }
                preferences[WISHLIST_KEY] = updated
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { state ->
            val existingItem = state.cartItems.find { it.product.id == product.id }
            val newItems = if (existingItem != null) {
                state.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                state.cartItems + CartItem(product)
            }
            state.copy(cartItems = newItems)
        }
    }

    fun updateQuantity(product: Product, delta: Int) {
        _uiState.update { state ->
            val newItems = state.cartItems.map {
                if (it.product.id == product.id) {
                    val newQty = (it.quantity + delta).coerceAtLeast(1)
                    it.copy(quantity = newQty)
                } else it
            }
            state.copy(cartItems = newItems)
        }
    }

    fun removeFromCart(product: Product) {
        _uiState.update { state ->
            state.copy(cartItems = state.cartItems.filter { it.product.id != product.id })
        }
    }

    fun placeOrder(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                repository.createOrder(currentState.cartItems, currentState.total)
                clearCart()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ShopViewModel", "Failed to place order", e)
            }
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }
}

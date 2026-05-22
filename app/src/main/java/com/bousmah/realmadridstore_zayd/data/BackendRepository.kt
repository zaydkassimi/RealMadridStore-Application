package com.bousmah.realmadridstore_zayd.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackendRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(BackendApi::class.java)

    suspend fun getProducts(): List<Product> = api.getProducts()

    suspend fun createOrder(cartItems: List<CartItem>, total: Double): OrderRequest {
        val order = OrderRequest(
            items = cartItems,
            total = total
        )
        return api.createOrder(order)
    }

    suspend fun getOrders(): List<OrderRequest> = api.getOrders()
}

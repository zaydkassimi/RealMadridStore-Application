package com.bousmah.realmadridstore_zayd.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BackendApi {
    @GET("api/products")
    suspend fun getProducts(): List<Product>

    @POST("api/orders")
    suspend fun createOrder(@Body order: OrderRequest): OrderRequest

    @GET("api/orders")
    suspend fun getOrders(): List<OrderRequest>
}

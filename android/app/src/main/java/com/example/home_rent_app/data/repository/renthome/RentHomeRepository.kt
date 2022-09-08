package com.example.home_rent_app.data.repository.renthome

import com.example.home_rent_app.data.dto.AddRentHomeRequest
import com.example.home_rent_app.data.dto.AddWantHomeResponseDTO
import com.example.home_rent_app.data.model.ImageUrl
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

interface RentHomeRepository {

    suspend fun addRentHome(request: AddRentHomeRequest): AddWantHomeResponseDTO
}

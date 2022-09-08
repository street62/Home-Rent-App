package com.example.home_rent_app.data.repository.wanthome

import com.example.home_rent_app.data.api.AddWantHomeApi
import com.example.home_rent_app.data.dto.AddWantHomeResponseDTO
import com.example.home_rent_app.data.dto.WantHomeDetailResponseDTO
import com.example.home_rent_app.data.model.AddWantHomeRequest
import com.example.home_rent_app.data.session.UserSession
import javax.inject.Inject

class WantHomeRepositoryImpl @Inject constructor(
    private val api: AddWantHomeApi,
    private val userSession: UserSession
) : WantHomeRepository {

    override suspend fun addWantHome(addWantHomeRequest: AddWantHomeRequest): AddWantHomeResponseDTO {
        return api.addWantHome(addWantHomeRequest)
    }

    override suspend fun getWantHome(itemId: Int): WantHomeDetailResponseDTO {
        return api.getWantHome(itemId)
    }
}

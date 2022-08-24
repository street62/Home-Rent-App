package com.example.home_rent_app.data

import androidx.lifecycle.viewModelScope
import com.example.home_rent_app.data.api.TokenRefreshApi
import com.example.home_rent_app.data.dto.OAuthTokenResponse
import com.example.home_rent_app.data.repository.login.LoginRepository
import com.example.home_rent_app.data.repository.refresh.RefreshRepository
import com.example.home_rent_app.util.AppSession
import com.example.home_rent_app.util.CoroutineException
import com.example.home_rent_app.util.logger
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appSession: AppSession,
    private val loginRepository: LoginRepository,
    private val refreshRepository: RefreshRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val jwt = appSession.jwt

        val requestBuilder = chain.request()
            .newBuilder()

        jwt?.let {
            requestBuilder.addHeader(
                "access-token",
                it.accessToken.tokenCode
            ) // 추후 수정
        }

        val response = chain.proceed(requestBuilder.build())

        val ceh = CoroutineExceptionHandler { _, throwable ->
            logger("refresh token error : ${CoroutineException.checkThrowable(throwable).errorMessage}")
        }
        if (response.code == 401) {

            jwt?.let {
                requestBuilder.addHeader(
                    "access-token",
                    it.accessToken.tokenCode
                ).addHeader(
                    "refresh-token",
                    it.refreshToken.tokenCode
                )
            }

            val refreshResponse = chain.proceed(requestBuilder.build())

            CoroutineScope(Job() + ceh).launch {
                val token = refreshRepository.refreshToken()
                val list = listOf(token.accessToken.tokenCode, token.refreshToken.tokenCode)
                loginRepository.saveToken(list)
                loginRepository.setAppSession(list)
            }
            return refreshResponse
        }
        return response
    }

}

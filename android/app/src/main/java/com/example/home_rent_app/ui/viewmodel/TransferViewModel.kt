package com.example.home_rent_app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.home_rent_app.data.model.ImageUrl
import com.example.home_rent_app.data.model.RoomPicture
import com.example.home_rent_app.data.repository.transfer.TransferRepository
import com.example.home_rent_app.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(private val transferRepository: TransferRepository, private val fileController: FileController) : ViewModel() {

    val title = MutableStateFlow("")

    val houseType = MutableStateFlow(HouseType.ONE_ROOM)

    val contractType = MutableStateFlow(RentType.MONTHLY)

    val deposit = MutableStateFlow("")

    val monthly = MutableStateFlow("")

    val maintenance = MutableStateFlow("")

    val maintenanceDescription = MutableStateFlow("")

    val availableFrom = MutableStateFlow("")

    val expiredAt = MutableStateFlow("")

    private val _homeDescriptionState = MutableStateFlow(false)
    val homeDescriptionState = _homeDescriptionState.asStateFlow()

    private val _isCorrectDate = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val isCorrectDate = _isCorrectDate.asSharedFlow()

    private val _picture = MutableStateFlow<List<RoomPicture>>(emptyList())
    val picture = _picture.asStateFlow()

    private val _overPictures = MutableStateFlow(false)
    val overPictures = _overPictures.asStateFlow()

    private val _houseImages = MutableStateFlow<UiState<ImageUrl>>(UiState.Loading)
    val houseImages = _houseImages.asStateFlow()

    val address = MutableStateFlow("")

    val addressDetail = MutableStateFlow("")

    val facilities = MutableStateFlow<List<String>>(emptyList())

    val securityFaclities = MutableStateFlow<List<String>>(emptyList())

    val content = MutableStateFlow("")

    val maxFloor = MutableStateFlow("")

    val thisFloor = MutableStateFlow("")

    val hasParkingLot = MutableStateFlow(false)

    val hasBalcony = MutableStateFlow(false)

    val hasElevator = MutableStateFlow(false)

    private var id = 0

    fun setHomeDescriptionState() {
        when (contractType.value) {
            RentType.JEONSE -> setJeonseHomeDescriptionState()
            RentType.MONTHLY -> setMonthlyHomeDescriptionState()
        }
    }

    fun setPictureUri(uri: Uri) {
        when (_picture.value.size) {
            0 -> {
                _picture.value = listOf(RoomPicture(id, uri, true))
                id++
            }
            6 -> {
                _overPictures.value = true
            }
            else -> {
                val list = mutableListOf<RoomPicture>()
                list.addAll(_picture.value)
                list.add(RoomPicture(id, uri))
                _picture.value = list
                id++
            }
        }
    }

    fun removePicUri(index: Int) {
        val list = mutableListOf<RoomPicture>()
        list.addAll(_picture.value)
        list.removeAt(index)
        if(list.isNotEmpty()) {
            list[0].isMain = true
        }
        _picture.value = list
        _overPictures.value = false
    }

    private fun setJeonseHomeDescriptionState() {
        if (title.value != "" &&
            deposit.value != "" &&
            maintenance.value != "" &&
            maintenanceDescription.value != "" &&
            availableFrom.value != "" &&
            expiredAt.value != ""
        ) {
            _homeDescriptionState.value = true
        }
    }

    private fun setMonthlyHomeDescriptionState() {
        if (title.value != "" &&
            deposit.value != "" &&
            monthly.value != "" &&
            maintenance.value != "" &&
            maintenanceDescription.value != "" &&
            availableFrom.value != "" &&
            expiredAt.value != ""
        ) {
            _homeDescriptionState.value = true
        }
    }

    fun replacePic(beforePosition: Int, targetPosition: Int) {
        when {
            targetPosition == 0 -> {
                val list = mutableListOf<RoomPicture>()
                list.addAll(_picture.value)
                list[targetPosition].isMain = false
                list[beforePosition].isMain = true

                val temp = list[beforePosition]
                list[beforePosition] = list[targetPosition]
                list[targetPosition] = temp

                _picture.value = list
            }
            beforePosition == 0 -> {
                val list = mutableListOf<RoomPicture>()
                list.addAll(_picture.value)
                list[beforePosition].isMain = false
                list[targetPosition].isMain = true

                val temp = list[beforePosition]
                list[beforePosition] = list[targetPosition]
                list[targetPosition] = temp
                _picture.value = list
            }
            else -> {
                val list = mutableListOf<RoomPicture>()
                list.addAll(_picture.value)
                val temp = list[beforePosition]
                list[beforePosition] = list[targetPosition]
                list[targetPosition] = temp
                _picture.value = list
            }
        }
    }

    suspend fun checkCorrectDate() {

        if (availableFrom.value != "" && expiredAt.value != "") {
            _isCorrectDate.emit(compareToDate() < 0)
        }
    }

    private fun compareToDate() = availableFrom.value.compareTo(expiredAt.value)

    fun getImageUrl() {
        val list = mutableListOf<MultipartBody.Part>()
        _picture.value.forEach { roomPic ->
            logger("URI: ${roomPic.uri}")
            list.add(fileController.uriToMultiPart(roomPic.uri))
        }
        viewModelScope.launch {
            list.forEach {
                logger("image : $it")
            }
            transferRepository.getImageUrl(list).catch {  e ->
                _houseImages.value = UiState.Error(e.stackTraceToString())
            }.collect {
                _houseImages.value = UiState.Success(it)
            }
        }
    }
}

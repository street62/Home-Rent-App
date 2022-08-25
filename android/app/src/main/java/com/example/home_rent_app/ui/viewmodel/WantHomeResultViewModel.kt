package com.example.home_rent_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.home_rent_app.data.dto.WantedArticle
import com.example.home_rent_app.data.model.AddBookmarkRequest
import com.example.home_rent_app.data.model.WantHomeResultRequest
import com.example.home_rent_app.data.repository.wanthomeresult.WantHomeResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WantHomeResultViewModel @Inject constructor(private val wantHomeResultRepository: WantHomeResultRepository) :
    ViewModel() {

    private val _searchWord = MutableSharedFlow<String>()
    val searchWord = _searchWord.debounce { 400 }

    private val _wantHomeResult = MutableStateFlow<List<WantedArticle>>(emptyList())
    val wantHomeResult: StateFlow<List<WantedArticle>> get() = _wantHomeResult

    private val _addBookmarkStatusCode = MutableSharedFlow<Int>()
    val addBookmarkStatusCode: SharedFlow<Int> get() = _addBookmarkStatusCode

    fun handleSearchWork(searchWord: String) {
        viewModelScope.launch {
            _searchWord.emit(searchWord)
        }
    }

    fun getWantHomeResult(wantHomeResultRequest: WantHomeResultRequest) {
        viewModelScope.launch {
            wantHomeResultRepository.getResult(wantHomeResultRequest).collect { response ->
                _wantHomeResult.value = response
            }
        }
    }

    fun addBookmark(addBookmarkRequest: AddBookmarkRequest) {
        viewModelScope.launch {
            _addBookmarkStatusCode.emit(wantHomeResultRepository.addBookmark(addBookmarkRequest).code)
        }
    }
}

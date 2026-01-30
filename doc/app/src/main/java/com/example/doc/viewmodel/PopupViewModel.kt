package com.example.doc.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PopupViewModel @Inject constructor() : ViewModel() {
    val showResultPopup = mutableStateOf(false)
    val resultPopupMessage = mutableStateOf("")
    val showEvaluationPopup = mutableStateOf(false)
    val evaluationMessage = mutableStateOf("")
    val evaluationDetails = mutableStateOf(listOf<String>())
    val resultPopupColor: MutableState<Color> = mutableStateOf(Color.Unspecified)
    val evaluationColor: MutableState<Color> = mutableStateOf(Color.Unspecified)


}
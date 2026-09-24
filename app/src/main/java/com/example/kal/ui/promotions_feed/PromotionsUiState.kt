package com.example.kal.ui.promotions_feed

import com.example.kal.data.Post

sealed class PromotionsUiState {
    object Loading : PromotionsUiState()
    data class Success(val posts: List<Post>) : PromotionsUiState()
    data class Error(val message: String) : PromotionsUiState()
}
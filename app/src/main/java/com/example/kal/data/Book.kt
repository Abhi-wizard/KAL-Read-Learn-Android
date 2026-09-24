package com.example.kal.data

import com.google.firebase.firestore.DocumentId

data class Book(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val author: String = "",

    val descrption: String = "", // <-- RENAMED

    val coverImageUrl: String = "",
    val genre: String = "",
    val pdfUrl: String = "",
    val pageCount: Int = 0,
    val storagePath: String = "",
    val contentStartPage: Long = 0,
    val price: Int = 0
) {
    fun withId(id: String): Book = this.copy(id = id)
}
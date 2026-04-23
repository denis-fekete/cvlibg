package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class CardDetail(
    val id: String,
    val title: String,
    val description: String,
    val links: List<String> = emptyList(),
    val imagePath: String? = null
)
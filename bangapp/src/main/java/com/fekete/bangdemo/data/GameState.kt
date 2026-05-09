package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val id: Int = 0,
    val role: String? = null,
    val character: String? = null,
    val inventory: MutableList<String> = mutableListOf(),
)

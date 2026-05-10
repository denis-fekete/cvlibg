package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

/**
 * Holds the current game state of the application.
 *
 * @param id id of this game state, in case more game states were loaded
 * @param role link of the current role card
 * @param character link of the current character card
 * @param inventory list of links to card in the inventory
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
@Serializable
data class GameState(
    val id: Int = 0,
    val role: String? = null,
    val character: String? = null,
    val inventory: MutableList<String> = mutableListOf(),
)

package com.fekete.bangdemo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class used for loading details about detected card from the JSON file from the assets. Uses the
 * [com.fekete.bangdemo.MyApp.cardDetailsService].
 *
 * @param id id used navigating between detail cards
 * @param title title/name of the card
 * @param descriptionEN description of the card
 * @param links link of string values, these are translated into clickable links that move user to details page of
 * linked card
 * @param imagePath path to the image from the assets folder
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
@Serializable
data class CardDetail(
    val id: String = "",
    val title: String = "",
    val descriptionEN: String = "",
    val descriptionSK: String = "",
    val descriptionCZ: String = "",
    val links: List<String> = emptyList(),
    val imagePath: String? = null,
    val totalInDeck: Int = 0,
    val type: CardType = CardType.Other
)

@Serializable
enum class CardType {
    @SerialName("action")
    Action,

    @SerialName("effect")
    Effect,

    @SerialName("character")
    Character,

    @SerialName("role")
    Role,

    @SerialName("weapon")
    Weapon,

    @SerialName("symbol")
    Symbol,

    @SerialName("other")
    Other,
}
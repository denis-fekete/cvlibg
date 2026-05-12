package com.fekete.bangdemo.viewmodels

import androidx.lifecycle.ViewModel
import com.fekete.bangdemo.data.CardDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * View model used for displaying the current game state, this should be used with `activityViewModel()` as this data
 * should be preserved between fragments.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class GameStateSharedViewModel : ViewModel() {
    private val _inventory = MutableStateFlow<List<CardDetail>>(emptyList())

    /**
     * List of [CardDetail] objects, these objects will be shown in the [com.fekete.bangdemo.fragments.InventoryFragment]
     */
    val inventory: StateFlow<List<CardDetail>> = _inventory

    private val _role = MutableStateFlow<CardDetail>(CardDetail())

    /**
     * Currently used Role from the game, this role is displayed by [com.fekete.bangdemo.fragments.CharacterAndRoleFragment]
     */
    val role: StateFlow<CardDetail> = _role

    private val _character = MutableStateFlow<CardDetail>(CardDetail())

    /**
     * Currently used Character from the game, this role is displayed by [com.fekete.bangdemo.fragments.CharacterAndRoleFragment]
     */
    val character: StateFlow<CardDetail> = _character

    private val _inventoryVisible = MutableStateFlow<Boolean>(false)

    /**
     * Shared, subscribable flag, for whenever, the inventory should be visible
     */
    val inventoryVisible: StateFlow<Boolean> = _inventoryVisible


    private val _otherOverlaysVisible = MutableStateFlow<Boolean>(false)

    /**
     * Shared, subscribable flag, for whenever, the role and character should be visible
     */
    val otherOverlaysVisible: StateFlow<Boolean> = _otherOverlaysVisible

    /**
     * Adds new [CardDetail] object to the inventory, [inventory] will be updated.
     */
    fun addToInventory(card: CardDetail) {
        _inventory.update { it + card }
    }

    /**
     * Removes [CardDetail] object to the inventory, [inventory] will be updated.
     */
    fun removeFromInventory(card: CardDetail) {
        _inventory.update { it - card }
    }

    /**
     * Clears the current game state.
     */
    fun clearGameState() {
        _inventory.value = emptyList()
        _role.value = CardDetail()
        _character.value = CardDetail()
    }

    /**
     * Sets the new role using [CardDetail], cards is not checked whenever it has a role type. Setting default
     * object with [CardDetail.id] equal to empty string will disable this functionality and clicks will not respond.
     */
    fun setRole(card: CardDetail) {
        _role.update { card }
    }

    /**
     * Sets the new character using [CardDetail], cards is not checked whenever it has a character type. Setting default
     * object with [CardDetail.id] equal to empty string will hide UI element representing it.
     */
    fun setCharacter(card: CardDetail) {
        _character.update { card }
    }

    /**
     * Separately sets UI responsible for displaying [inventory] and [otherOverlaysVisible] with corresponding values.
     */
    fun overlaysVisible(inventory: Boolean, other: Boolean) {
        _inventoryVisible.update { inventory }
        _otherOverlaysVisible.update { other }
    }
}
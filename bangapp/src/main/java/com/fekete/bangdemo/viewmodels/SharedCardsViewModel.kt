package com.fekete.bangdemo.viewmodels

import androidx.lifecycle.ViewModel
import com.fekete.bangdemo.data.CardDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class SharedCardsViewModel : ViewModel() {
    private val _inventory = MutableStateFlow<List<CardDetail>>(emptyList())
    val inventory: StateFlow<List<CardDetail>> = _inventory

    private val _role = MutableStateFlow<CardDetail>(CardDetail())
    val role: StateFlow<CardDetail> = _role

    private val _character = MutableStateFlow<CardDetail>(CardDetail())
    val character: StateFlow<CardDetail> = _character

    private val _inventoryVisible = MutableStateFlow<Boolean>(false)
    val inventoryVisible: StateFlow<Boolean> = _inventoryVisible

    private val _otherOverlaysVisible = MutableStateFlow<Boolean>(false)
    val otherOverlaysVisible: StateFlow<Boolean> = _otherOverlaysVisible

    fun addToInventory(card: CardDetail) {
        _inventory.update { it + card }
    }

    fun removeFromInventory(card: CardDetail) {
        _inventory.update { it - card }
    }

    fun clearInventory() {
        _inventory.value = emptyList()
    }

    fun setRole(card: CardDetail) {
        _role.update { card }
    }

    fun setCharacter(card: CardDetail) {
        _character.update { card }
    }

    fun overlaysVisible(inventory: Boolean, other: Boolean) {
        _inventoryVisible.update { inventory }
        _otherOverlaysVisible.update { other }
    }
}
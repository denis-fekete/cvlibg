package com.fekete.bangdemo.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.R
import com.fekete.cvlibg.utils.AssetLoader

/**
 * Adapter for the [RecyclerView] utilizing [CardDetail] objects to display cards.
 *
 * @param assetLoader used for loading image assets.
 * @param onCardClicked callback function called on image part of the view being clicked.
 * @param onAddCardClicked callback function called on add button being clicked.
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class CardSearchAdapter(
    private val assetLoader: AssetLoader,
    private val onCardClicked: (CardDetail) -> Unit,
    private val onAddCardClicked: (CardDetail) -> Unit
) :
    RecyclerView.Adapter<CardViewHolder>() {
    private val cardList = mutableListOf<CardDetail>()

    /**
     * Clears the internal [cardList] and adds the new elements from the [list] to it.
     */
    fun setList(list: List<CardDetail>) {
        cardList.clear()
        cardList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cardList[position]
        holder.bindToCard(card, assetLoader, onCardClicked, onAddCardClicked)
    }

    override fun getItemCount(): Int {
        return cardList.size
    }
}
package com.fekete.bangdemo.search

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.R
import com.fekete.cvlibg.utils.AssetLoader
import com.google.android.material.imageview.ShapeableImageView

/**
 * Derived from [RecyclerView.ViewHolder] holding information about a item from [RecyclerView].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageView: ShapeableImageView = itemView.findViewById(R.id.searchItemImage)
    private val addButton: ImageView = itemView.findViewById(R.id.searchAddItemButton)

    /**
     * Bind callback functions and load image.
     *
     * @param cardDetail card to be loaded.
     * @param assetLoader used for loading image assets.
     * @param onCardClicked callback function called on image part of the view being clicked.
     * @param onAddCardClicked callback function called on add button being clicked.
     */
    fun bindToCard(
        cardDetail: CardDetail,
        assetLoader: AssetLoader,
        onCardClicked: (CardDetail) -> Unit,
        onAddCardClicked: (CardDetail) -> Unit
    ) {
        imageView.setOnClickListener { onCardClicked.invoke(cardDetail) }
        addButton.setOnClickListener { onAddCardClicked.invoke(cardDetail) }

        if (cardDetail.imagePath != null) {
            val bitmap = assetLoader.loadImage(cardDetail.imagePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        }

        imageView.setImageResource(R.drawable.error)
    }
}
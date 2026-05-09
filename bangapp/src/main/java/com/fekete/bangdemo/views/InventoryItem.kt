package com.fekete.bangdemo.views

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import com.fekete.bangdemo.R

/**
 * Small view based around [R.layout.view_inventory_item]. Shows loaded [bitmap] as a image and its accompanying delete
 * button.
 *
 * Provides two callback functions: [onImageClicked] and [onDeleteClicked].
 *
 * @param context
 * @param bitmap or image, placed as image representation
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class InventoryItem(
    context: Context,
    bitmap: Bitmap,
) : FrameLayout(context) {
    var onImageClicked: (() -> Unit)? = null
    var onDeleteClicked: (() -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_inventory_item, this, true)

        val imageView = view.findViewById<ImageView>(R.id.itemImageView)
        val deleteImageView = view.findViewById<ImageView>(R.id.deleteItemImageView)

        imageView.setImageBitmap(bitmap)

        imageView.setOnClickListener {
            onImageClicked?.invoke()
        }

        deleteImageView.setOnClickListener {
            onDeleteClicked?.invoke()
        }
    }
}
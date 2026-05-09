package com.fekete.bangdemo.views

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import com.fekete.bangdemo.R

/**
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class CardPreviewView : FrameLayout {
    private val context: Context?
    private val bitmap: Bitmap?

    constructor(context: Context?, bitmap: Bitmap) : super(context!!) {
        this.context = context
        this.bitmap = bitmap

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        params.setMargins(0, 0, 40, 0)
        this.layoutParams = params

        LayoutInflater.from(context).inflate(R.layout.view_card_preview, this, true)

        val imageView = findViewById<ImageView>(R.id.linkViewImageView)

        imageView.setImageBitmap(bitmap)
    }
}
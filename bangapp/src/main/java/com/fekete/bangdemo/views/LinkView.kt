package com.fekete.bangdemo.views

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.fekete.bangdemo.R


class LinkView : LinearLayout {
    private val context: Context?
    private val text: String?
    private val bitmap: Bitmap?

    constructor(context: Context?, text: String?, textUnit: Int, textSize: Float, bitmap: Bitmap?) : super(context) {
        this.context = context
        this.text = text
        this.bitmap = bitmap

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        params.setMargins(0, 0, 40, 0)
        this.layoutParams = params
        this.orientation = HORIZONTAL
        this.gravity = Gravity.CENTER

        LayoutInflater.from(context).inflate(R.layout.view_link, this, true)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val textView = findViewById<TextView>(R.id.textView)
        val textContainer = findViewById<FrameLayout>(R.id.textContainer)

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            textContainer.visibility = GONE
        } else {
            imageView.visibility = GONE

            textView.text = text ?: "Default"
            textView.setTextSize(textUnit, textSize)
        }
    }
}
package com.fekete.bangdemo.views

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.fekete.bangdemo.R
import com.google.android.material.imageview.ShapeableImageView

/**
 * A layout using Linear Layout as its base, contains two views, ImageView and TextView. If [bitmap] is not provided the
 * ImageView is hidden and TextView with [text] is used.
 *
 * @param context
 * @param text string value for [LinkView] to use, if [bitmap] is null.
 * @param bitmap bitmap set to the ImageView.
 * @param textSize font size in [textUnit]
 * @param textUnit font units to use desired dimension unit.
 * @param marginLeft margin applied to the left of LinkView
 * @param marginTop margin applied to the top of LinkView
 * @param marginRight margin applied to the right of LinkView
 * @param marginBottom margin applied to the bottom of LinkView
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class LinkView(
    context: Context,
    bitmap: Bitmap?,
    text: String?,
    textSize: Float,
    textUnit: Int = TypedValue.COMPLEX_UNIT_SP,
    marginLeft: Int = 0,
    marginTop: Int = 0,
    marginRight: Int = 0,
    marginBottom: Int = 0,
) :
    FrameLayout(context) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_link, this, true)

        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
        params.setMargins(marginLeft, marginTop, marginRight, marginBottom)

        this.layoutParams = params

        val imageView = view.findViewById<ShapeableImageView>(R.id.linkViewImageView)
        val textView = view.findViewById<TextView>(R.id.linkViewTextView)
        val textContainer = view.findViewById<FrameLayout>(R.id.linkViewTextContainer)

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
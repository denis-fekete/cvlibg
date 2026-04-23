package com.fekete.cvlibg.ui

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Simple adapter for [android.widget.Spinner] UI element that contains strings. Adds [setTextSize] for changing the
 * size of the text displayed by the UI element.
 *
 * @param items An array of string values that the spinner will contain.
 * @param textUnit Initial unit type of [android.util.TypedValue], mostly used [android.util.TypedValue.COMPLEX_UNIT_SP] or
 * [android.util.TypedValue.COMPLEX_UNIT_PX].
 * @param textSize Initial size of text on element creation.
 */
class ResizableSpinnerAdapter(
    context: Context,
    private val items: Array<String>,
    private var textUnit: Int = TypedValue.COMPLEX_UNIT_SP,
    private var textSize: Float,
    private val resource: Int = android.R.layout.simple_spinner_dropdown_item,
) : ArrayAdapter<String>(context, resource, items) {
    init {
        setDropDownViewResource(resource)
    }

    fun setTextSize(unit: Int = TypedValue.COMPLEX_UNIT_SP, newSize: Float) {
        textSize = newSize
        textUnit = unit
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as TextView).setTextSize(textUnit, textSize)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as TextView).setTextSize(textUnit, textSize)
        return view
    }
}
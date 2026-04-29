package com.fekete.cvlibg.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView

/**
 * Spinner UI element that uses [String] [Collection] for displaying data. This spinner uses [ResizableSpinnerAdapter]
 * for adjusting font size.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class StringListSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.spinnerStyle
) : androidx.appcompat.widget.AppCompatSpinner(context, attrs, defStyleAttr) {
    private var setupCalled = false

    /**
     * Sets up this spinner with elements from the [items]. This function can be called only once.
     *
     * @param items [Collection] of items that will be displayed in the spinner.
     * @param selectedItem string value from the [items], if not found or set to `null` a first element will be chosen.
     * @param fontSize Font size for the [ResizableSpinnerAdapter].
     * @param onItemChanged Callback invoked when selected item changed.
     */
    fun setup(
        items: Collection<String>,
        selectedItem: String?,
        fontSize: Float,
        onItemChanged: (Int) -> Unit
    ) {
        if (setupCalled) return

        val itemArray = items.toTypedArray()

        val modelAdapter = ResizableSpinnerAdapter(
            context,
            itemArray,
            TypedValue.COMPLEX_UNIT_SP,
            fontSize,
        )

        this.adapter = modelAdapter

        val index = if (selectedItem != null) items.indexOf(selectedItem) else -1
        this.setSelection(if (index == -1) 0 else index, false)

        this.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemChanged(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        setupCalled = true
    }
}
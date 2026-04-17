package cv.cbglib.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

/**
 * Subclass of [android.widget.Spinner] UI element that contains list of strings and uses [ResizableSpinnerAdapter].
 *
 * @param context: Context,
 * @param attrs: AttributeSet? = null,
 * @param defStyleAttr: Int = android.R.attr.spinnerStyle
 */
class StringListSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.spinnerStyle
) : Spinner(context, attrs, defStyleAttr) {
    private var setupCalled = false

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
            var lastSelected: Int = 0

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == lastSelected) return

                lastSelected = position
                onItemChanged(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        setupCalled = true
    }
}
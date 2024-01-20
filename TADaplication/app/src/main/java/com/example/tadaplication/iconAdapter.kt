package com.example.tadaplication
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class IconAdapter(context: Context, private val options: Array<String>, private val icons: Array<Int>) :
    ArrayAdapter<CharSequence>(context, android.R.layout.select_dialog_item, options) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = options[position]
        textView.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0)

        return view
    }
}
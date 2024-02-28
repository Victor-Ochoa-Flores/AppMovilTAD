package com.example.tadaplication
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CuentaAdapter(private val personList: List<Cuenta>) :
    RecyclerView.Adapter<CuentaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textViewName)
        val lastNameTextView: TextView = view.findViewById(R.id.textViewLastName)
    }
    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cuenta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val person = personList[position]
        holder.nameTextView.text = person.correo
        holder.lastNameTextView.text = person.nombre

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return personList.size
    }
}

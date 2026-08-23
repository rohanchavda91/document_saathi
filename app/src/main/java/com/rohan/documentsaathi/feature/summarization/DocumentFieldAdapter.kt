package com.rohan.documentsaathi.feature.summarization

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rohan.documentsaathi.databinding.ItemDocumentFieldBinding

class DocumentFieldAdapter(
    private val fields: List<DocumentField>
) : RecyclerView.Adapter<DocumentFieldAdapter.FieldViewHolder>() {

    class FieldViewHolder(val binding: ItemDocumentFieldBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemDocumentFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val field = fields[position]
        holder.binding.tvFieldLabel.text = field.key
        holder.binding.tvFieldValue.text = field.value
    }

    override fun getItemCount() = fields.size
}

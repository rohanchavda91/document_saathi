package com.rohan.documentsaathi.feature.home.ui

import com.rohan.documentsaathi.data.db.entity.Document
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rohan.documentsaathi.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentAdapter(
    private var documents: List<Document>,
    private val onItemClick: (Document) -> Unit,
    private val onDeleteClick: (Document) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {
    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(document: Document) {
            // Document Title (first few words)
            val words = document.extractedText.split(" ").filter { it.isNotBlank() }
            binding.tvDocumentTitle.text = if (words.size >= 3) {
                "${words[0]} ${words[1]} ${words[2]}..."
            } else if (words.isNotEmpty()) {
                document.extractedText.take(20)
            } else {
                "Untitled Document"
            }

            // Preview Text
            binding.tvPreviewText.text = document.extractedText

            // Language
            binding.tvLanguage.text = document.detectedLanguage

            // Date
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvScannedDate.text = dateFormat.format(document.createdAt)

            // Summarized Badge
            binding.tvSummarized.visibility = if (document.isSummarized) android.view.View.VISIBLE else android.view.View.GONE

            // Action Buttons
            binding.btnView.setOnClickListener {
                onItemClick(document)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(document)
            }

            binding.root.setOnClickListener {
                onItemClick(document)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documents[position])
    }

    override fun getItemCount() = documents.size

//    Update documents in adapter
    fun updateDocuments(newDocuments: List<Document>) {
        this.documents = newDocuments
        notifyDataSetChanged()
    }
}
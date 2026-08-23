package com.rohan.documentsaathi.feature.document.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.databinding.FragmentDocumentDetailBinding
import com.rohan.documentsaathi.feature.document.ui.DocumentDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DocumentDetailFragment : Fragment(){
    private var _binding: FragmentDocumentDetailBinding?=null
    private val binding get() = _binding!!

    private val viewModel: DocumentDetailViewModel by viewModels()
    private val args: DocumentDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentDocumentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val documentId = args.documentId

//        documents ne db mathi load kravva
        viewLifecycleOwner.lifecycleScope.launch {
            val document = viewModel.getDocumentById(documentId)
            if (document != null) {
                binding.tvDocumentTitle.text = "Document #$documentId"
                binding.tvExtractedText.text = document.extractedText
                binding.chipLanguage.text = document.detectedLanguage
                binding.tvScannedDate.text = formatDate(document.createdAt)

                // Populate ID and Name (Extracted Info)
                val (docId, holderName) = extractIdAndName(document.extractedText, document.summary)
                binding.tvDocumentIdValue.text = docId ?: getString(R.string.not_available)
                binding.tvHolderNameValue.text = holderName ?: getString(R.string.not_available)

                // ID Copy button
                binding.btnCopyDocumentId.setOnClickListener {
                    docId?.let { copyToClipboard(it) } ?: Toast.makeText(requireContext(), "ID not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

//        Copy nu button for raw text
        binding.btnCopyText.setOnClickListener {
            val text = binding.tvExtractedText.text.toString()
            copyToClipboard(text)
        }

//        Share document btn
        binding.btnShareDocument.setOnClickListener {
            val text = binding.tvExtractedText.text.toString()
            shareText(text)
        }

        binding.btnRescan.setOnClickListener {
            Toast.makeText(requireContext(), "Rescan feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnBookmark.setOnClickListener {
            Toast.makeText(requireContext(), "Bookmark saved", Toast.LENGTH_SHORT).show()
        }

//        Delete nu btn
        binding.btnDelete.setOnClickListener {
            viewModel.deleteDocument(documentId)
            Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        // Back toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

//    Clipboard ma copy krvanu function
    private fun copyToClipboard(text: String){
        val clipboard=requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip=ClipData.newPlainText("document_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

//    Text ne share krva nu function
    private fun shareText(text:String){
        val shareIntent = Intent().apply{
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type="text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share document"))
    }

//    Date ne format krvu
    private fun formatDate(timestamp: Long): String{
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Simple helper to try and extract ID and Name from text.
     * This is a temporary measure until the AI provides structured data.
     */
    private fun extractIdAndName(extractedText: String, summary: String?): Pair<String?, String?> {
        val combinedText = "$extractedText\n${summary ?: ""}"
        
        // Very basic regex patterns for demo purposes
        val idRegex = Regex("(?i)(id|number|no)[:.\\s]+([A-Z0-9]{4,})")
        val nameRegex = Regex("(?i)(name|holder)[:.\\s]+([A-Z\\s]{3,})")
        
        val docId = idRegex.find(combinedText)?.groupValues?.get(2)
        val holderName = nameRegex.find(combinedText)?.groupValues?.get(2)?.trim()
        
        return Pair(docId, holderName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}
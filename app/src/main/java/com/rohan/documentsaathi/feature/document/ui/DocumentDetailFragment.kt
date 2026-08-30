package com.rohan.documentsaathi.feature.document.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.databinding.FragmentDocumentDetailBinding
import com.rohan.documentsaathi.databinding.ItemDynamicFieldBinding
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

        setupEdgeToEdge()
        val documentId = args.documentId

        // Load document through ViewModel
        viewModel.loadDocument(documentId)

        // Observe document state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.documentState.collect { document ->
                if (document != null) {
                    binding.tvExtractedText.text = document.extractedText
                    binding.chipLanguage.text = document.detectedLanguage
                    binding.tvScannedDate.text = formatDate(document.createdAt)

                    // Dynamic Field Population
                    setupDynamicFields(document.structuredDataJson, document.id)

                    // Update Summary UI
                    if (document.summary != null) {
                        binding.tvSummary.text = document.summary
                        binding.summaryProgressBar.visibility = View.GONE
                    } else {
                        binding.tvSummary.text = "Fetching AI summary..."
                        binding.summaryProgressBar.visibility = View.VISIBLE
                    }

                    // PDF View Button
                    binding.btnViewPdf.setOnClickListener {
                        document.pdfUri?.let { uri ->
                            viewPdf(uri)
                        } ?: Toast.makeText(requireContext(), "PDF not available", Toast.LENGTH_SHORT).show()
                    }

                    // Updated Share button for PDF
                    binding.btnShareDocument.setOnClickListener {
                        document.pdfUri?.let { uri ->
                            sharePdf(uri)
                        } ?: shareText(document.extractedText)
                    }
                }
            }
        }

//        Copy nu button for raw text
        binding.btnCopyText.setOnClickListener {
            val text = binding.tvExtractedText.text.toString()
            copyToClipboard(text)
        }

//        Share document btn
        // This is now handled inside lifecycleScope to access document.pdfUri

        binding.btnRescan.setOnClickListener {
            Toast.makeText(requireContext(), "Rescan feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnBookmark.setOnClickListener {
            Toast.makeText(requireContext(), "Bookmark saved", Toast.LENGTH_SHORT).show()
        }

//        Delete nu btn
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation(documentId)
        }

        // Back toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to toolbar
            binding.toolbar.updatePadding(top = systemBars.top)
            
            // Adjust toolbar height to include status bar
            binding.toolbar.updateLayoutParams {
                height = resources.getDimensionPixelSize(R.dimen.app_bar_height) + systemBars.top
            }

            // Apply bottom padding to root container to avoid content being hidden by nav bar
            binding.root.updatePadding(bottom = systemBars.bottom)
            
            insets
        }
    }

    private fun showDeleteConfirmation(documentId: Long) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Document?")
            .setMessage("Are you sure you want to delete this document? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDocument(documentId)
                Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .show()
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

    private fun sharePdf(pdfPath: String) {
        val file = java.io.File(pdfPath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File error", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share PDF Document"))
    }

    private fun viewPdf(pdfPath: String) {
        val file = java.io.File(pdfPath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }

//    Date ne format krvu
    private fun formatDate(timestamp: Long): String{
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun setupDynamicFields(json: String?, documentId: Long) {
        android.util.Log.d("DocumentDetail", "Setting up dynamic fields for Doc: $documentId. JSON: $json")
        
        if (json.isNullOrEmpty()) {
            binding.tvDocumentTitle.text = "Document #$documentId"
            binding.fieldsContainer.removeAllViews()
            val loadingView = TextView(requireContext()).apply {
                text = "Extracting document details..."
                setPadding(0, 16, 0, 16)
                alpha = 0.7f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.poppins_medium)
            }
            binding.fieldsContainer.addView(loadingView)
            return
        }

        try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, String>>() {}.type
            val fields: Map<String, String> = gson.fromJson(json, type)

            // Clear previous fields
            binding.fieldsContainer.removeAllViews()

            // Handle Document Title
            val docType = fields["document_type"] ?: "Document #$documentId"
            binding.tvDocumentTitle.text = docType

            // Inflate each field
            fields.forEach { (key, value) ->
                if (key != "document_type" && value.isNotEmpty()) {
                    val fieldBinding = ItemDynamicFieldBinding.inflate(
                        LayoutInflater.from(requireContext()),
                        binding.fieldsContainer,
                        true
                    )
                    
                    val displayLabel = key.replace("_", " ").uppercase()
                    fieldBinding.tvLabel.text = displayLabel
                    fieldBinding.tvValue.text = value
                    
                    fieldBinding.btnCopy.setOnClickListener {
                        copyToClipboard(value)
                    }
                }
            }
        } catch (e: Exception) {
            binding.tvDocumentTitle.text = "Document #$documentId"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}
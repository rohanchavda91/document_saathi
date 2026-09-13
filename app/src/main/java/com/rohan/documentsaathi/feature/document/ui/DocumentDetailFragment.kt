package com.rohan.documentsaathi.feature.document.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.databinding.DialogPdfViewerBinding
import com.rohan.documentsaathi.databinding.DialogShareBottomSheetBinding
import com.rohan.documentsaathi.databinding.FragmentDocumentDetailBinding
import com.rohan.documentsaathi.databinding.ItemDynamicFieldBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
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
                        binding.tvSummary.text = getString(R.string.fetching_ai_summary)
                        binding.summaryProgressBar.visibility = View.VISIBLE
                    }

                    // PDF View Button
                    binding.btnViewPdf.setOnClickListener {
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Opening PDF...", Toast.LENGTH_SHORT).show()
                            val pdfFile = viewModel.getOrGeneratePdfFile(document)
                            if (pdfFile != null && pdfFile.exists()) {
                                viewPdf(pdfFile.absolutePath)
                            } else {
                                Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // PDF Download Button
                    binding.btnDownloadPdf.setOnClickListener {
                        lifecycleScope.launch {
                            Toast.makeText(requireContext(), "Downloading PDF...", Toast.LENGTH_SHORT).show()
                            val docTitle = binding.tvDocumentTitle.text.toString()
                            val success = viewModel.exportPdfToDownloads(document, docTitle)
                            if (success) {
                                Toast.makeText(requireContext(), "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Custom Share button for PDF / JPG
                    binding.btnShareDocument.setOnClickListener {
                        showShareBottomSheet(document)
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
            findNavController().navigate(R.id.action_documentDetail_to_scanner)
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

    private fun showShareBottomSheet(document: Document) {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.Theme_DocumentSaathi)
        val dialog = BottomSheetDialog(themedContext)
        val dialogBinding = DialogShareBottomSheetBinding.inflate(
            LayoutInflater.from(themedContext)
        )
        dialog.setContentView(dialogBinding.root)

        val defaultName = binding.tvDocumentTitle.text.toString().ifEmpty { "Document_${document.id}" }
        dialogBinding.etFileName.setText(defaultName)

        val sizeInBytes = document.imageUri?.let { File(it).length() }
            ?: document.pdfUri?.let { File(it).length() } ?: 0L
        val sizeInMb = String.format(Locale.US, "%.2f MB", sizeInBytes / (1024.0 * 1024.0))
        dialogBinding.tvFileInfo.text = getString(R.string.file_count_format, 1, sizeInMb)

        dialogBinding.btnEditName.setOnClickListener {
            dialogBinding.etFileName.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(dialogBinding.etFileName, InputMethodManager.SHOW_IMPLICIT)
        }

        dialogBinding.btnCancelShare.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmShare.setOnClickListener {
            val customName = dialogBinding.etFileName.text.toString().trim().ifEmpty { defaultName }
            val isPdfSelected = dialogBinding.toggleFormat.checkedButtonId == R.id.btn_format_pdf

            dialog.dismiss()

            lifecycleScope.launch {
                Toast.makeText(requireContext(), "Preparing file...", Toast.LENGTH_SHORT).show()
                val fileToShare = viewModel.prepareShareFile(document, customName, isPdfSelected)
                if (fileToShare != null && fileToShare.exists()) {
                    shareFile(fileToShare, if (isPdfSelected) "application/pdf" else "image/jpeg")
                } else {
                    Toast.makeText(requireContext(), "Failed to prepare file for sharing", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Document"))
    }

    private fun viewPdf(pdfPath: String) {
        val file = File(pdfPath)
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
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Open PDF with")
        try {
            startActivity(chooser)
        } catch (_: Exception) {
            showInAppPdfViewer(file)
        }
    }

    private fun showInAppPdfViewer(file: File) {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.Theme_DocumentSaathi)
        val dialog = Dialog(themedContext, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        val dialogBinding = DialogPdfViewerBinding.inflate(
            LayoutInflater.from(themedContext)
        )
        dialog.setContentView(dialogBinding.root)

        val docName = binding.tvDocumentTitle.text.toString()
        dialogBinding.tvPdfTitle.text = docName

        try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val bitmap = createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                dialogBinding.ivPdfPage.setImageBitmap(bitmap)
                page.close()
            }
            pdfRenderer.close()
            fileDescriptor.close()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Failed to render PDF preview", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnClosePdf.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSharePdfInViewer.setOnClickListener {
            val currentDoc = viewModel.documentState.value
            if (currentDoc != null) {
                showShareBottomSheet(currentDoc)
            }
        }

        dialogBinding.btnDownloadPdfInViewer.setOnClickListener {
            lifecycleScope.launch {
                val currentDoc = viewModel.documentState.value
                if (currentDoc != null) {
                    val success = viewModel.exportPdfToDownloads(currentDoc, docName)
                    if (success) {
                        Toast.makeText(requireContext(), "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
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
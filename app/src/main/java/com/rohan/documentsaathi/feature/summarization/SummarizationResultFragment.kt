package com.rohan.documentsaathi.feature.summarization

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rohan.documentsaathi.databinding.FragmentSummarizationResultBinding
import com.rohan.documentsaathi.feature.ocr.ui.OcrResultViewModel
import com.rohan.documentsaathi.feature.ocr.ui.OcrResultUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@AndroidEntryPoint
class SummarizationResultFragment : Fragment() {

    private var _binding: FragmentSummarizationResultBinding? = null
    private val binding get() = _binding!!

    private val args: SummarizationResultFragmentArgs by navArgs()
    private val viewModel: OcrResultViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummarizationResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Trigger AI Summarization when fragment opens
        viewModel.callDeepSeekSummarization(
            extractedText = args.extractedText,
            language = args.selectedLanguage
        )
        
        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is OcrResultUiState.SummarizationLoading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvFields.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            binding.btnSavePdf.visibility = View.GONE
                        }

                        is OcrResultUiState.SummarizationFieldsSuccess -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            binding.rvFields.visibility = View.VISIBLE
                            binding.btnSavePdf.visibility = View.VISIBLE

                            // Update Header
                            binding.tvDocumentType.text = state.documentType

                            // Setup RecyclerView
                            binding.rvFields.apply {
                                layoutManager = LinearLayoutManager(requireContext())
                                adapter = DocumentFieldAdapter(state.fields)
                            }

                            // Save PDF button click
                            binding.btnSavePdf.setOnClickListener {
                                // PDF save logic will go here
                                savePdfDocument(state.documentType, state.fields)
                            }
                        }

                        is OcrResultUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvFields.visibility = View.GONE
                            binding.tvError.visibility = View.VISIBLE
                            binding.btnSavePdf.visibility = View.GONE
                            binding.tvError.text = state.message
                        }

                        else -> {
                            // Idle state, initial state — nothing to show
                        }
                    }
                }
            }
        }
    }

    private fun savePdfDocument(documentType: String, fields: List<DocumentField>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = Color.parseColor("#0D47A1")
        }
        val contentPaint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
        }
        val labelPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.GRAY
        }

        var y = 60f
        canvas.drawText("Document Saathi - Summary", 50f, y, titlePaint)
        y += 40f
        canvas.drawText("Type: $documentType", 50f, y, contentPaint)
        y += 30f
        canvas.drawLine(50f, y, 545f, y, contentPaint)
        y += 40f

        for (field in fields) {
            if (y > 780) { // Safety check for page end
                break
            }
            canvas.drawText(field.key.uppercase(), 50f, y, labelPaint)
            y += 20f
            canvas.drawText(field.value, 50f, y, contentPaint)
            y += 35f
        }

        pdfDocument.finishPage(page)

        val fileName = "Summary_${System.currentTimeMillis()}.pdf"
        var outputStream: OutputStream? = null
        var filePath = ""

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/DocumentSaathi")
                }

                val contentResolver = requireContext().contentResolver
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                    filePath = "Documents/DocumentSaathi/$fileName"
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DocumentSaathi")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, fileName)
                outputStream = FileOutputStream(file)
                filePath = file.absolutePath
            }

            outputStream?.use {
                pdfDocument.writeTo(it)
            }
            Toast.makeText(requireContext(), "PDF Saved at: $filePath", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
